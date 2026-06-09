package qikahome.jsonmore.lib.ingredient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;

public class ItemDisplayOverrideIngredient extends AbstractIngredient {
    public static final ResourceLocation ID = new ResourceLocation("jsonmore:item_display_override");

    private static final Map<String, IOpSerializer> OPS = new HashMap<>();

    public interface IOpSerializer {
        IOpHandler deserialize(JsonObject data);
        default IOpHandler deserialize(FriendlyByteBuf buffer) {
            throw new UnsupportedOperationException("Network deserialization not supported for op");
        }
    }

    public interface IOpHandler {
        void apply(List<ItemStack> items, @Nullable Ingredient filter);
        void write(FriendlyByteBuf buffer);
    }

    public static void registerOp(String name, IOpSerializer serializer) {
        OPS.put(name, serializer);
    }

    // ---- Built-in ops ----

    private record RemoveHandler(Ingredient toRemove) implements IOpHandler {
        @Override public void apply(List<ItemStack> items, @Nullable Ingredient filter) {
            if (filter == null) items.removeIf(toRemove);
            else items.removeIf(s -> toRemove.test(s) && filter.test(s));
        }
        @Override public void write(FriendlyByteBuf buffer) { toRemove.toNetwork(buffer); }
    }
    private record AddAllHandler(Ingredient source) implements IOpHandler {
        @Override public void apply(List<ItemStack> items, @Nullable Ingredient filter) {
            for (ItemStack stack : source.getItems()) items.add(stack.copy());
        }
        @Override public void write(FriendlyByteBuf buffer) { source.toNetwork(buffer); }
    }
    private record AddHandler(ItemStack stack) implements IOpHandler {
        @Override public void apply(List<ItemStack> items, @Nullable Ingredient filter) {
            items.add(stack.copy());
        }
        @Override public void write(FriendlyByteBuf buffer) { buffer.writeItemStack(stack, false); }
    }
    private record ModifyNbtHandler(CompoundTag nbt, String mode) implements IOpHandler {
        @Override public void apply(List<ItemStack> items, @Nullable Ingredient filter) {
            for (int i = 0; i < items.size(); i++) {
                ItemStack item = items.get(i);
                if (filter != null && !filter.test(item)) continue;
                if ("replace".equals(mode)) item.setTag(nbt.copy());
                else item.getOrCreateTag().merge(nbt);
            }
        }
        @Override public void write(FriendlyByteBuf buffer) {
            buffer.writeUtf(mode);
            buffer.writeNbt(nbt);
        }
    }
    private record ModifyCountHandler(String operation, int value) implements IOpHandler {
        @Override public void apply(List<ItemStack> items, @Nullable Ingredient filter) {
            for (int i = 0; i < items.size(); i++) {
                ItemStack item = items.get(i);
                if (filter != null && !filter.test(item)) continue;
                int count = switch (operation) {
                    case "set" -> value;
                    case "add" -> item.getCount() + value;
                    case "multiply" -> item.getCount() * value;
                    default -> item.getCount();
                };
                item.setCount(Math.max(1, count));
            }
        }
        @Override public void write(FriendlyByteBuf buffer) {
            buffer.writeUtf(operation);
            buffer.writeVarInt(value);
        }
    }

    static {
        registerOp("remove", data -> new RemoveHandler(
                Ingredient.fromJson(data.get("value"))));
        registerOp("add_all", data -> new AddAllHandler(
                Ingredient.fromJson(data.get("value"))));
        registerOp("add", data -> new AddHandler(
                ItemStack.CODEC.parse(JsonOps.INSTANCE, data.get("value"))
                        .getOrThrow(false, s -> { throw new JsonParseException("Invalid item stack: " + s); })));
        registerOp("modify_nbt", data -> new ModifyNbtHandler(
                CompoundTag.CODEC.parse(JsonOps.INSTANCE, data.get("value"))
                        .getOrThrow(false, s -> { throw new JsonParseException("Invalid compound tag: " + s); }),
                GsonHelper.getAsString(data, "mode", "merge")));
        registerOp("modify_count", data -> new ModifyCountHandler(
                GsonHelper.getAsString(data, "operation", "set"),
                GsonHelper.getAsInt(data, "value")));
    }

    // ---- Instance ----

    private final Ingredient ingredient;
    private final List<OpEntry> ops;

    private ItemDisplayOverrideIngredient(Ingredient ingredient, List<OpEntry> ops) {
        super(Stream.empty());
        this.ingredient = ingredient;
        this.ops = ops;
    }

    private ItemStack[] cachedDisplayStacks = null;

    @Override
    public ItemStack[] getItems() {
        if (cachedDisplayStacks == null) {
            ItemStack[] base = ingredient.getItems();
            List<ItemStack> result = new ArrayList<>();
            for (ItemStack stack : base) result.add(stack.copy());
            for (OpEntry op : ops) op.handler.apply(result, op.filter);
            cachedDisplayStacks = result.toArray(new ItemStack[0]);
        }
        return cachedDisplayStacks.clone();
    }

    @Override public boolean isEmpty() { return ingredient.isEmpty(); }
    @Override public boolean test(@Nullable ItemStack stack) { return ingredient.test(stack); }
    @Override public boolean isSimple() { return false; }
    @Override public IIngredientSerializer<? extends Ingredient> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", ID.toString());
        json.add("ingredient", ingredient.toJson());
        JsonArray arr = new JsonArray();
        for (OpEntry op : ops) {
            JsonObject obj = new JsonObject();
            obj.addProperty("op", op.name);
            if (op.filter != null) obj.add("filter", op.filter.toJson());
            IOpSerializer serializer = OPS.get(op.name);
            if (serializer instanceof BuiltinSerializer bs) {
                JsonObject data = new JsonObject();
                bs.writeJson(data, op.handler);
                for (Map.Entry<String, JsonElement> e : data.entrySet())
                    obj.add(e.getKey(), e.getValue());
            }
            arr.add(obj);
        }
        json.add("ops", arr);
        return json;
    }

    // ---- Op entry ----

    private record OpEntry(String name, @Nullable Ingredient filter, IOpHandler handler) {}

    // ---- Builtin serializer with writeJson ----

    private interface BuiltinSerializer extends IOpSerializer {
        void writeJson(JsonObject out, IOpHandler handler);
        @Override IOpHandler deserialize(JsonObject data);
    }

    private static void registerBuiltin(String name, BiConsumer<JsonObject, IOpHandler> jsonWriter, IOpSerializer serializer) {
        var builtin = new BuiltinSerializer() {
            @Override public IOpHandler deserialize(JsonObject data) { return serializer.deserialize(data); }
            @Override public void writeJson(JsonObject out, IOpHandler handler) { jsonWriter.accept(out, handler); }
        };
        OPS.put(name, builtin);
    }

    static {
        registerBuiltin("remove", (out, h) -> out.add("value", ((RemoveHandler)h).toRemove.toJson()),
            data -> new RemoveHandler(Ingredient.fromJson(data.get("value"))));
        registerBuiltin("add_all", (out, h) -> out.add("value", ((AddAllHandler)h).source.toJson()),
            data -> new AddAllHandler(Ingredient.fromJson(data.get("value"))));
        registerBuiltin("add", (out, h) -> {
            DataResult<JsonElement> r = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, ((AddHandler)h).stack);
            out.add("value", r.getOrThrow(false, s -> {}));
        }, data -> new AddHandler(ItemStack.CODEC.parse(JsonOps.INSTANCE, data.get("value"))
                .getOrThrow(false, s -> { throw new JsonParseException("Invalid item stack: " + s); })));
        registerBuiltin("modify_nbt", (out, h) -> {
            var handler = (ModifyNbtHandler)h;
            out.addProperty("mode", handler.mode);
            DataResult<JsonElement> r = CompoundTag.CODEC.encodeStart(JsonOps.INSTANCE, handler.nbt);
            out.add("value", r.getOrThrow(false, s -> {}));
        }, data -> new ModifyNbtHandler(CompoundTag.CODEC.parse(JsonOps.INSTANCE, data.get("value"))
                .getOrThrow(false, s -> { throw new JsonParseException("Invalid compound tag: " + s); }),
                GsonHelper.getAsString(data, "mode", "merge")));
        registerBuiltin("modify_count", (out, h) -> {
            var handler = (ModifyCountHandler)h;
            out.addProperty("operation", handler.operation);
            out.addProperty("value", handler.value);
        }, data -> new ModifyCountHandler(GsonHelper.getAsString(data, "operation", "set"),
                GsonHelper.getAsInt(data, "value")));
    }

    // ---- Serializer ----

    public static class Serializer implements IIngredientSerializer<ItemDisplayOverrideIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public ItemDisplayOverrideIngredient parse(FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            int opCount = buffer.readVarInt();
            List<OpEntry> ops = new ArrayList<>();
            for (int i = 0; i < opCount; i++) {
                String name = buffer.readUtf();
                boolean hasFilter = buffer.readBoolean();
                Ingredient filter = hasFilter ? Ingredient.fromNetwork(buffer) : null;
                IOpHandler handler = switch (name) {
                    case "remove" -> new RemoveHandler(Ingredient.fromNetwork(buffer));
                    case "add_all" -> new AddAllHandler(Ingredient.fromNetwork(buffer));
                    case "add" -> new AddHandler(buffer.readItem());
                    case "modify_nbt" -> new ModifyNbtHandler(buffer.readNbt(), buffer.readUtf());
                    case "modify_count" -> new ModifyCountHandler(buffer.readUtf(), buffer.readVarInt());
                    default -> {
                        IOpSerializer ser = OPS.get(name);
                        if (ser == null) throw new RuntimeException("Unknown op: " + name);
                        yield ser.deserialize(buffer);
                    }
                };
                ops.add(new OpEntry(name, filter, handler));
            }
            return new ItemDisplayOverrideIngredient(ingredient, ops);
        }

        @Override
        public ItemDisplayOverrideIngredient parse(JsonObject json) {
            if (!json.has("ingredient"))
                throw new JsonParseException("ItemDisplayOverrideIngredient must have 'ingredient' field");
            if (!json.has("ops"))
                throw new JsonParseException("ItemDisplayOverrideIngredient must have 'ops' array");

            Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"));
            JsonArray opsArray = GsonHelper.getAsJsonArray(json, "ops");
            List<OpEntry> ops = new ArrayList<>();

            for (JsonElement elem : opsArray) {
                JsonObject opObj = elem.getAsJsonObject();
                String name = GsonHelper.getAsString(opObj, "op");
                Ingredient filter = opObj.has("filter") ? Ingredient.fromJson(opObj.get("filter")) : null;

                IOpSerializer serializer = OPS.get(name);
                if (serializer == null)
                    throw new JsonParseException("Unknown op: " + name);

                IOpHandler handler = serializer.deserialize(opObj);
                ops.add(new OpEntry(name, filter, handler));
            }

            return new ItemDisplayOverrideIngredient(ingredient, ops);
        }

        @Override
        public void write(FriendlyByteBuf buffer, ItemDisplayOverrideIngredient ingredient) {
            ingredient.ingredient.toNetwork(buffer);
            buffer.writeVarInt(ingredient.ops.size());
            for (OpEntry op : ingredient.ops) {
                buffer.writeUtf(op.name);
                buffer.writeBoolean(op.filter != null);
                if (op.filter != null) op.filter.toNetwork(buffer);
                op.handler.write(buffer);
            }
        }
    }

    public static void register() {
        CraftingHelper.register(ID, Serializer.INSTANCE);
    }
}
