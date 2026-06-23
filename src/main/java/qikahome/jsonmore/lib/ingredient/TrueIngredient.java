package qikahome.jsonmore.lib.ingredient;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import qikahome.jsonmore.JsonMore;

public class TrueIngredient implements ICustomIngredient {
    public static final ResourceLocation ID = ResourceLocation.parse("jsonmore:true");
    public static final TrueIngredient INSTANCE = new TrueIngredient();
    public static final MapCodec<TrueIngredient> CODEC = MapCodec.unit(INSTANCE);
    public static final DeferredHolder<IngredientType<?>, IngredientType<TrueIngredient>> TYPE = JsonMore.INGREDIENT_TYPES
            .register(ID.getPath(), () -> new IngredientType<>(CODEC));

    private static final Stream<ItemStack> ANYTHING_STACK;

    static {
        ItemStack stack = new ItemStack(Items.STICK);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("ingredient.jsonmore.true"));
        ANYTHING_STACK = Stream.of(stack);
    }

    @Override
    public Stream<ItemStack> getItems() {
        return ANYTHING_STACK;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return true;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    public static void register() {
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE.get();
    }
}
