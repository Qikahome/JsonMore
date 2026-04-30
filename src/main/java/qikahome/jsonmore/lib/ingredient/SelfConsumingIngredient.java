package qikahome.jsonmore.lib.ingredient;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;

public abstract class SelfConsumingIngredient extends AbstractIngredient {
    protected final Ingredient ingredient;

    public SelfConsumingIngredient(Ingredient ingredient) {
        if (ingredient instanceof SelfConsumingIngredient)
            throw new IllegalArgumentException("Self consuming ingredient must not be nested");
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
        if (ingredient instanceof SelfConsumingIngredient selfConsumingIngredient)
            return selfConsumingIngredient.consume(stack);
        return vanillaConsume(stack);
    }

    /**
     * 消耗逻辑
     * 
     * @param stack 要消耗的物品栈（会直接修改，必须传入容器中的原始引用）
     * @return 消耗后的返还物品
     */
    public abstract ItemStack consume(ItemStack stack);

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
        // ItemStack copy = stack.copy();
        ItemStack remainder = stack.getCraftingRemainingItem();
        /*
         * ItemStack copy = stack.copy();
         * copy.shrink(1);
         * if (!remainder.isEmpty())
         * if (!copy.isEmpty())
         * output.accept(remainder);
         * else
         * copy = remainder;
         * return copy;
         */
        return remainder;
    }

    /**
     * 工具方法：从 JSON 对象中解析 remainder_override 字段。
     * 提供统一的错误处理，当格式错误时抛出 JsonSyntaxException。
     * 
     * @param json JSON 对象
     * @return 解析后的物品栈，如果不存在则返回 null
     * @throws com.google.gson.JsonSyntaxException 当格式错误时抛出
     */
    protected static ItemStack parseRemainderOverride(JsonObject json) {
        if (!json.has("remainder_override")) {
            return null;
        }
        try {
            DataResult<ItemStack> result = ItemStack.CODEC.parse(JsonOps.INSTANCE, json.get("remainder_override"));
            return result.getOrThrow(false, String::new);
        } catch (RuntimeException e) {
            throw new com.google.gson.JsonSyntaxException("Invalid remainder_override: " + e.getMessage(), e);
        }
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
        if (ingredient instanceof SelfConsumingIngredient selfConsumingIngredient)
            selfConsumingIngredient.outputModify(matched, output);
    }

    /**
     * 根据匹配的输入物品，修改配方输出物品。默认不做任何修改。
     *
     * @param matched 匹配到的输入物品
     * @param output  输出物品（可直接修改其属性，如设置数量、NBT等）
     */
    public void outputModify(ItemStack matched, ItemStack output) {
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