package qikahome.jsonmore.lib.ingredient;

import java.util.List;

import javax.annotation.Nullable;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * "自消耗"原料的公共基类，对应 Forge 原版的 {@code AbstractIngredient} 子类。
 * <p>
 * Fabric 侧的 {@link CustomIngredient} 通过 {@link CustomIngredient#toVanilla()} 转成原版
 * {@link Ingredient}，因此这里只保留消耗/输出改写的公共逻辑，展示物品仍走 {@link #getItems()}。
 */
public abstract class SelfConsumingIngredient implements CustomIngredient {
    protected final Ingredient ingredient;

    public SelfConsumingIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    /**
     * 消耗逻辑
     * 
     * @param ingredient 要消耗的配方材料
     * @param stack      要消耗的物品栈（容器中的原始引用）
     * @return 消耗后的返还物品（由调用者决定如何处理）
     */
    public static ItemStack consume(Ingredient ingredient, ItemStack stack) {
        if (stack.isEmpty())
            return stack;
        if (Ingredients.unwrap(ingredient) instanceof SelfConsumingIngredient selfConsumingIngredient)
            return selfConsumingIngredient.consume(stack);
        return vanillaConsume(stack);
    }

    /**
     * 消耗逻辑
     * 
     * @param stack 要消耗的物品栈（会直接修改，必须传入容器中的原始引用）
     * @return 消耗后的返还物品
     */
    public ItemStack consume(ItemStack stack) {
        return consume(ingredient, stack);
    }

    /**
     * 工具方法：实现原版默认消耗逻辑（返还容器物品）。
     * 子类可在 consume 方法中调用此方法，简化代码。
     * 
     * @param stack 要消耗的物品栈
     * @return 消耗后的返还物品
     */
    protected static ItemStack vanillaConsume(ItemStack stack) {
        if (stack.isEmpty())
            return stack;
        // 1.20.1 的物品栈没有 Forge 的 getCraftingRemainingItem()，改用 Item 上的同名方法。
        if (!stack.getItem().hasCraftingRemainingItem())
            return ItemStack.EMPTY;
        return new ItemStack(stack.getItem().getCraftingRemainingItem());
    }

    /**
     * 根据匹配的输入物品，修改配方输出物品。默认不做任何修改。
     *
     * @param ingredient 对应的配方材料
     * @param matched    匹配到的输入物品
     * @param output     输出物品（可直接修改其属性，如设置数量、NBT等）
     */
    public static void outputModify(Ingredient ingredient, ItemStack matched, ItemStack output) {
        if (matched.isEmpty())
            return;
        if (Ingredients.unwrap(ingredient) instanceof SelfConsumingIngredient selfConsumingIngredient)
            selfConsumingIngredient.outputModify(matched, output);
    }

    /**
     * 根据匹配的输入物品，修改配方输出物品。默认不做任何修改。
     *
     * @param matched 匹配到的输入物品
     * @param output  输出物品（可直接修改其属性，如设置数量、NBT等）
     */
    public void outputModify(ItemStack matched, ItemStack output) {
        outputModify(ingredient, matched, output);
    }

    /**
     * Forge 原版的展示物品入口（返回数组）。Fabric 接口只要求 {@link #getMatchingStacks()}，
     * 这里保留该方法供各子类照常覆写，并由基类统一转换成列表。
     */
    public ItemStack[] getItems() {
        return ingredient.getItems();
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
        return List.of(getItems());
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return ingredient.test(stack);
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }
}
