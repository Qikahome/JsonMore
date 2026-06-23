package qikahome.jsonmore.lib.ingredient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import qikahome.jsonmore.JsonMore;

public class ItemDisplayOverrideIngredient implements ICustomIngredient {
    public static final ResourceLocation ID = ResourceLocation.parse("jsonmore:item_display_override");

    private static final Map<String, MapCodec<? extends IOpHandler>> OPS = new HashMap<>();

    private static final Codec<IOpHandler> HANDLER_CODEC = Codec.STRING.dispatch("op", IOpHandler::name, OPS::get);

    public interface IOpHandler {
        String name();

        void apply(List<ItemStack> items, @Nullable Ingredient filter);
    }

    public static void registerOp(String name, MapCodec<? extends IOpHandler> codec) {
        OPS.put(name, codec);
    }

    // ---- Built-in ops ----

    private record RemoveHandler(Ingredient toRemove) implements IOpHandler {
        public static final MapCodec<RemoveHandler> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Ingredient.CODEC.fieldOf("value").forGetter(RemoveHandler::toRemove))
                        .apply(instance, RemoveHandler::new));

        @Override
        public String name() {
            return "remove";
        }

        @Override
        public void apply(List<ItemStack> items, @Nullable Ingredient filter) {
            if (filter == null)
                items.removeIf(toRemove);
            else
                items.removeIf(s -> toRemove.test(s) && filter.test(s));
        }
    }

    private record AddAllHandler(Ingredient source) implements IOpHandler {
        public static final MapCodec<AddAllHandler> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Ingredient.CODEC.fieldOf("value").forGetter(AddAllHandler::source))
                        .apply(instance, AddAllHandler::new));

        @Override
        public String name() {
            return "add_all";
        }

        @Override
        public void apply(List<ItemStack> items, @Nullable Ingredient filter) {
            for (ItemStack stack : source.getItems())
                items.add(stack.copy());
        }
    }

    private record AddHandler(ItemStack stack) implements IOpHandler {
        public static final MapCodec<AddHandler> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        ItemStack.CODEC.fieldOf("value").forGetter(AddHandler::stack))
                        .apply(instance, AddHandler::new));

        @Override
        public String name() {
            return "add";
        }

        @Override
        public void apply(List<ItemStack> items, @Nullable Ingredient filter) {
            items.add(stack.copy());
        }
    }

    private record ModifyCountHandler(String operation, int value) implements IOpHandler {
        public static final MapCodec<ModifyCountHandler> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Codec.STRING.fieldOf("operation").forGetter(ModifyCountHandler::operation),
                        Codec.INT.fieldOf("value").forGetter(ModifyCountHandler::value))
                        .apply(instance, ModifyCountHandler::new));

        @Override
        public String name() {
            return "modify_count";
        }

        @Override
        public void apply(List<ItemStack> items, @Nullable Ingredient filter) {
            for (int i = 0; i < items.size(); i++) {
                ItemStack item = items.get(i);
                if (filter != null && !filter.test(item))
                    continue;
                int count = switch (operation) {
                    case "set" -> value;
                    case "add" -> item.getCount() + value;
                    case "multiply" -> item.getCount() * value;
                    default -> item.getCount();
                };
                item.setCount(Math.max(1, count));
            }
        }
    }

    static {
        registerOp("remove", RemoveHandler.CODEC);
        registerOp("add_all", AddAllHandler.CODEC);
        registerOp("add", AddHandler.CODEC);
        registerOp("modify_count", ModifyCountHandler.CODEC);
    }

    // ---- Instance ----

    private final Ingredient ingredient;
    private final List<OpEntry> ops;

    public static final MapCodec<ItemDisplayOverrideIngredient> CODEC = RecordCodecBuilder.mapCodec(
            v -> v.group(
                    Ingredient.CODEC.fieldOf("ingredient").forGetter(i -> i.ingredient),
                    OpEntry.LIST_CODEC.fieldOf("ops").forGetter(i -> i.ops))
                    .apply(v, ItemDisplayOverrideIngredient::new));
    public static final DeferredHolder<IngredientType<?>, IngredientType<ItemDisplayOverrideIngredient>> TYPE = JsonMore.INGREDIENT_TYPES
            .register(ID.getPath(), () -> new IngredientType<>(CODEC));

    private ItemDisplayOverrideIngredient(Ingredient ingredient, List<OpEntry> ops) {
        this.ingredient = ingredient;
        this.ops = ops;
    }

    private Stream<ItemStack> cachedDisplayStacks = null;

    @Override
    public Stream<ItemStack> getItems() {
        if (cachedDisplayStacks == null) {
            Stream<ItemStack> base = Stream.of(ingredient.getItems());
            List<ItemStack> result = new ArrayList<>();
            base.forEach(s -> result.add(s.copy()));
            for (OpEntry op : ops)
                op.handler.apply(result, op.filter);
            cachedDisplayStacks = result.stream();
        }
        return cachedDisplayStacks;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return ingredient.test(stack);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE.get();
    }

    // ---- Op entry ----

    private record OpEntry(@Nullable Ingredient filter, IOpHandler handler) {
        public static final Codec<OpEntry> CODEC = RecordCodecBuilder.create(
                v -> v.group(
                        Ingredient.CODEC.fieldOf("filter").forGetter(OpEntry::filter),
                        HANDLER_CODEC.fieldOf("handler").forGetter(OpEntry::handler))
                        .apply(v, OpEntry::new));
        public static final Codec<List<OpEntry>> LIST_CODEC = CODEC.listOf();
    }

    public static void register() {
    }
}
