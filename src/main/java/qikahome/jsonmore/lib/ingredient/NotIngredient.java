package qikahome.jsonmore.lib.ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import qikahome.jsonmore.JsonMore;

public class NotIngredient implements ICustomIngredient {
    public static final ResourceLocation ID = ResourceLocation.parse("jsonmore:not");
    public static final MapCodec<NotIngredient> CODEC = RecordCodecBuilder.mapCodec(
            v -> v.group(
                    Ingredient.CODEC.fieldOf("ingredient").forGetter(i -> i.ingredient)).apply(v, NotIngredient::new));
    public static final DeferredHolder<IngredientType<?>, IngredientType<NotIngredient>> TYPE = JsonMore.INGREDIENT_TYPES
            .register(ID.getPath(), () -> new IngredientType<>(CODEC));
    private final Ingredient ingredient;

    private NotIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public static Ingredient of(Ingredient ingredient) {
        if (ingredient.getCustomIngredient() instanceof NotIngredient not) {
            return not.ingredient;
        }
        return new NotIngredient(ingredient).toVanilla();
    }

    private Stream<ItemStack> cachedDisplayStacks = null;

    @Override
    public Stream<ItemStack> getItems() {
        if (cachedDisplayStacks == null) {
            ItemStack[] subItems = ingredient.getItems();
            if (subItems.length == 0) {
                List<ItemStack> list = new ArrayList(List.of(TrueIngredient.INSTANCE.getItems()));
                for (var stack : list) {
                    stack.set(DataComponents.CUSTOM_NAME, Component.translatable("ingredient.jsonmore.not", "nothing"));
                }
                cachedDisplayStacks = list.stream();
            } else {
                List<ItemStack> list = new ArrayList(List.of(subItems));
                for (var stack : list) {
                    stack.set(DataComponents.CUSTOM_NAME,
                            Component.translatable("ingredient.jsonmore.not", stack.getHoverName()));
                }
                cachedDisplayStacks = list.stream();
            }
        }
        return cachedDisplayStacks;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return !ingredient.test(stack);
    }

    @Override
    public boolean isSimple() {
        return ingredient.isSimple();
    }

    public static void register() {
        // do nothing
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE.get();
    }
}
