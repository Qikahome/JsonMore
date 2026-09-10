package qikahome.jsonmore.lib.ingredient;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;

public class TrueIngredient implements CustomIngredient {
    public static final ResourceLocation ID = ResourceLocation.parse("jsonmore:true");
    public static final TrueIngredient INSTANCE = new TrueIngredient();
    public static final MapCodec<TrueIngredient> CODEC = MapCodec.unit(INSTANCE);
    public static final CustomIngredientSerializer<TrueIngredient> SERIALIZER = new SimpleIngredientSerializer<>(ID, CODEC);

    private static final List<ItemStack> ANYTHING_STACK;

    static {
        ItemStack stack = new ItemStack(Items.STICK);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("ingredient.jsonmore.true"));
        ANYTHING_STACK = List.of(stack);
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
        return ANYTHING_STACK;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return true;
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    public static void register() {
        CustomIngredientSerializer.register(SERIALIZER);
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
