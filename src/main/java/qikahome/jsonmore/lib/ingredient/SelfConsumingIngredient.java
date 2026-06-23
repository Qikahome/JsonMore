package qikahome.jsonmore.lib.ingredient;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;

public abstract class SelfConsumingIngredient implements ICustomIngredient {
    protected static final MapCodec<Ingredient> INGREDIENT = Ingredient.CODEC.fieldOf("ingredient");

    protected static <T extends SelfConsumingIngredient> RecordCodecBuilder<T, Ingredient> getIngredientField() {
        return INGREDIENT.forGetter(ing -> ing.ingredient);
    }

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
    public static ItemStack consume(Ingredient ingredient, ItemStack stack, ServerLevel level,
            @Nullable LivingEntity entity) {
        if (stack.isEmpty())
            return stack;
        if (ingredient.getCustomIngredient() instanceof SelfConsumingIngredient selfConsumingIngredient)
            return selfConsumingIngredient.consume(stack, level, entity);
        return vanillaConsume(stack);
    }

    /**
     * 消耗逻辑
     * 
     * @param stack 要消耗的物品栈（会直接修改，必须传入容器中的原始引用）
     * @return 消耗后的返还物品
     */
    public ItemStack consume(ItemStack stack, ServerLevel level, @Nullable LivingEntity entity) {
        return consume(ingredient, stack, level, entity);
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
        return stack.getCraftingRemainingItem();
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
        if (ingredient.getCustomIngredient() instanceof SelfConsumingIngredient selfConsumingIngredient)
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

    @Override
    public Stream<ItemStack> getItems() {
        return Stream.of(ingredient.getItems());
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        return ingredient.test(stack);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

}
