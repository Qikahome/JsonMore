package qikahome.jsonmore.lib.ingredient;

import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;

public abstract class SelfConsumingIngredient extends AbstractIngredient {
    protected final Ingredient ingredient;

    public SelfConsumingIngredient(Ingredient ingredient) {
        if(ingredient instanceof SelfConsumingIngredient)
            throw new IllegalArgumentException("Self consuming ingredient must not be nested");
        this.ingredient = ingredient;
    }

    /**
     * 消耗逻辑
     * 
     * @param ingredient 要消耗的配方材料
     * @param stack      要消耗的物品栈
     * @return 消耗后的剩余物
     */
    public static ItemStack consume(Ingredient ingredient, ItemStack stack) {
        if (stack.isEmpty())
            return stack;
        if (ingredient instanceof SelfConsumingIngredient selfConsumingIngredient)
            return selfConsumingIngredient.consume(stack);
        return vanillaConsume(stack);
    }

    /**
     * 消耗逻辑
     * 
     * @param stack  要消耗的物品栈（不应修改）
     * @return 消耗后的剩余物
     */
    public abstract ItemStack consume(ItemStack stack);

    /**
     * 工具方法：实现原版默认消耗逻辑（消耗一个，返还容器物品）。
     * 子类可在 consume 方法中调用此方法，简化代码。
     * 
     * @param stack  要消耗的物品栈（不应修改）
     * @return 消耗后的剩余物
     */
    protected static ItemStack vanillaConsume(ItemStack stack) {
        if (stack.isEmpty())
            return stack;
        //ItemStack copy = stack.copy();
        ItemStack remainder = stack.getCraftingRemainingItem();
        /*copy.shrink(1);
        if (!remainder.isEmpty())
            if (!copy.isEmpty())
                output.accept(remainder);
            else
                copy = remainder;
        return copy;*/
        return remainder;
    }

    @Override
    public ItemStack[] getItems() {
        return ingredient.getItems();
    }

    @Override
    public boolean isEmpty() {
        return ingredient.isEmpty();
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
    public abstract net.minecraftforge.common.crafting.IIngredientSerializer<? extends Ingredient> getSerializer();
}