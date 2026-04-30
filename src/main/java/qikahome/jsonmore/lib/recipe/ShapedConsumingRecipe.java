package qikahome.jsonmore.lib.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import qikahome.jsonmore.lib.ingredient.SelfConsumingIngredient;

import static qikahome.jsonmore.lib.recipe.ShapelessConsumingRecipe.backtrackMatch;

public class ShapedConsumingRecipe extends ShapedRecipe implements IConsumingRecipe {

    public ShapedConsumingRecipe(ResourceLocation id, String group, CraftingBookCategory category,
            int width, int height, NonNullList<Ingredient> ingredients,
            ItemStack result, boolean showNotification) {
        super(id, group, category, width, height, ingredients, result, showNotification);
    }

    public ShapedConsumingRecipe(ResourceLocation id, String group, CraftingBookCategory category,
            int width, int height, NonNullList<Ingredient> ingredients,
            ItemStack result) {
        this(id, group, category, width, height, ingredients, result, true);
    }

    // ==================== 共用匹配逻辑 ====================
    /**
     * 匹配容器中的物品与配方的原料，返回每个格子对应的原料。
     * 若无法匹配则抛出异常。
     */
    private Map<Integer, Ingredient> matchIngredients(CraftingContainer container) {
        List<Ingredient> ingredients = new ArrayList<>(getIngredients());
        int recipeWidth = getWidth();
        int recipeHeight = getHeight();
        int containerWidth = container.getWidth();
        int containerHeight = container.getHeight();

        // 情况1：容器宽度为0（例如某些模组的假工作台），使用无序回溯匹配
        if (containerWidth == 0 || containerHeight == 0) {
            List<Integer> nonEmptySlots = new ArrayList<>();
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (!container.getItem(i).isEmpty()) {
                    nonEmptySlots.add(i);
                }
            }
            if (nonEmptySlots.size() < ingredients.size()) {
                throw new IllegalArgumentException("Not enough items in container to match recipe");
            }
            Map<Integer, Ingredient> slotToIngredient = new HashMap<>();
            boolean[] slotUsed = new boolean[container.getContainerSize()];
            boolean found = backtrackMatch(0, ingredients, nonEmptySlots, slotUsed, slotToIngredient, container);
            if (!found) {
                throw new IllegalArgumentException("Cannot match recipe ingredients");
            }
            return slotToIngredient;
        }

        // 情况2：正常有序网格，尝试所有起始位置和镜像
        for (int startX = 0; startX <= containerWidth - recipeWidth; startX++) {
            for (int startY = 0; startY <= containerHeight - recipeHeight; startY++) {
                for (boolean mirrored : new boolean[] { false, true }) {
                    if (!matches(container, startX, startY, mirrored))
                        continue;

                    Map<Integer, Ingredient> matchedSlots = new HashMap<>();
                    // 临时原料列表，用于消耗标记（不修改原列表）
                    List<Ingredient> remainingIngredients = new ArrayList<>(ingredients);

                    for (int row = 0; row < recipeHeight; row++) {
                        for (int col = 0; col < recipeWidth; col++) {
                            int ingredientIndex = mirrored
                                    ? (recipeWidth - col - 1) + row * recipeWidth
                                    : col + row * recipeWidth;
                            if (ingredientIndex >= remainingIngredients.size())
                                continue;
                            Ingredient ing = remainingIngredients.get(ingredientIndex);
                            if (ing.isEmpty())
                                continue;

                            int containerSlot = (startY + row) * containerWidth + (startX + col);
                            if (containerSlot >= container.getContainerSize())
                                continue;
                            ItemStack stack = container.getItem(containerSlot);
                            if (stack.isEmpty())
                                continue;

                            if (ing.test(stack)) {
                                matchedSlots.put(containerSlot, ing);
                                remainingIngredients.set(ingredientIndex, Ingredient.EMPTY);
                            }
                        }
                    }
                    // 检查所有原料是否都被匹配
                    boolean allMatched = remainingIngredients.stream().allMatch(Ingredient::isEmpty);
                    if (allMatched) {
                        return matchedSlots;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Cannot match recipe ingredients");
    }

    // 辅助方法：判断在给定偏移和镜像下是否匹配（仅用于有序情况）
    private boolean matches(CraftingContainer container, int startX, int startY, boolean mirrored) {
        NonNullList<Ingredient> ingredients = getIngredients();
        int recipeWidth = getWidth();
        int recipeHeight = getHeight();
        int containerWidth = container.getWidth();

        for (int i = 0; i < containerWidth; i++) {
            for (int j = 0; j < container.getHeight(); j++) {
                int k = i - startX;
                int l = j - startY;
                Ingredient ingredient = Ingredient.EMPTY;
                if (k >= 0 && l >= 0 && k < recipeWidth && l < recipeHeight) {
                    if (mirrored) {
                        ingredient = ingredients.get(recipeWidth - k - 1 + l * recipeWidth);
                    } else {
                        ingredient = ingredients.get(k + l * recipeWidth);
                    }
                }
                if (!ingredient.test(container.getItem(i + j * containerWidth))) {
                    return false;
                }
            }
        }
        return true;
    }

    // ==================== 剩余物品处理 ====================
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        Map<Integer, Ingredient> slotToIngredient = matchIngredients(container);

        for (var entry : slotToIngredient.entrySet()) {
            int slot = entry.getKey();
            ItemStack stack = container.getItem(slot);
            Ingredient ingredient = entry.getValue();
            ItemStack remaining = SelfConsumingIngredient.consume(ingredient, stack);
            remainingItems.set(slot, remaining);
        }
        return remainingItems;
    }

    // ==================== 合成输出处理 ====================
    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        // 获取默认的输出结果
        ItemStack result = super.assemble(container, registryAccess);
        if (result.isEmpty()) {
            return result;
        }

        // 匹配槽位与原料
        Map<Integer, Ingredient> slotToIngredient = matchIngredients(container);

        // 让每个原料修改输出
        for (var entry : slotToIngredient.entrySet()) {
            int slot = entry.getKey();
            ItemStack inputStack = container.getItem(slot);
            Ingredient ingredient = entry.getValue();
            SelfConsumingIngredient.outputModify(ingredient, inputStack, result);
        }
        return result;
    }

    // ==================== 序列化 ====================
    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements RecipeSerializer<ShapedConsumingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public ShapedConsumingRecipe fromJson(ResourceLocation id, JsonObject json) {
            ShapedRecipe recipe = net.minecraft.world.item.crafting.RecipeSerializer.SHAPED_RECIPE.fromJson(id, json);
            return new ShapedConsumingRecipe(recipe.getId(), recipe.getGroup(), recipe.category(),
                    recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(),
                    recipe.getResultItem(RegistryAccess.EMPTY), recipe.showNotification());
        }

        @Override
        public ShapedConsumingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ShapedRecipe recipe = net.minecraft.world.item.crafting.RecipeSerializer.SHAPED_RECIPE.fromNetwork(id,
                    buffer);
            return new ShapedConsumingRecipe(recipe.getId(), recipe.getGroup(), recipe.category(),
                    recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(),
                    recipe.getResultItem(RegistryAccess.EMPTY), recipe.showNotification());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ShapedConsumingRecipe recipe) {
            net.minecraft.world.item.crafting.RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe);
        }
    }
}