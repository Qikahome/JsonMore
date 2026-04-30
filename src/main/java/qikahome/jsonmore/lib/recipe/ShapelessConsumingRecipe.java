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
import net.minecraft.world.item.crafting.ShapelessRecipe;
import qikahome.jsonmore.lib.ingredient.SelfConsumingIngredient;

public class ShapelessConsumingRecipe extends ShapelessRecipe implements IConsumingRecipe {

    public ShapelessConsumingRecipe(ResourceLocation id, String group, CraftingBookCategory category,
            ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, category, result, ingredients);
    }

    // ==================== 共用匹配逻辑 ====================
    private Map<Integer, Ingredient> matchIngredients(CraftingContainer container) {
        List<Ingredient> ingredients = new ArrayList<>(getIngredients());
        List<Integer> nonEmptySlots = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (!container.getItem(i).isEmpty()) {
                nonEmptySlots.add(i);
            }
        }

        if (nonEmptySlots.size() < ingredients.size()) {
            throw new IllegalArgumentException("Not enough items in the container to match the recipe");
        }

        Map<Integer, Ingredient> slotToIngredient = new HashMap<>();
        boolean[] slotUsed = new boolean[container.getContainerSize()];
        boolean found = backtrackMatch(0, ingredients, nonEmptySlots, slotUsed, slotToIngredient, container);

        if (!found) {
            throw new IllegalArgumentException("Cannot match recipe ingredients");
        }
        return slotToIngredient;
    }

    public static boolean backtrackMatch(int idx, List<Ingredient> ingredients, List<Integer> slots,
            boolean[] slotUsed, Map<Integer, Ingredient> slotToIngredient,
            CraftingContainer container) {
        if (idx == ingredients.size())
            return true;
        Ingredient ingredient = ingredients.get(idx);
        for (int slot : slots) {
            if (!slotUsed[slot] && ingredient.test(container.getItem(slot))) {
                slotUsed[slot] = true;
                slotToIngredient.put(slot, ingredient);
                if (backtrackMatch(idx + 1, ingredients, slots, slotUsed, slotToIngredient, container)) {
                    return true;
                }
                slotUsed[slot] = false;
                slotToIngredient.remove(slot);
            }
        }
        return false;
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
        // 先获取默认的输出（父类原始结果）
        ItemStack result = super.assemble(container, registryAccess);
        if (result.isEmpty()) {
            return result;
        }

        // 匹配输入槽位与原料
        Map<Integer, Ingredient> slotToIngredient = matchIngredients(container);

        // 让每个原料有机会修改输出
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

    public static class Serializer implements RecipeSerializer<ShapelessConsumingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public ShapelessConsumingRecipe fromJson(ResourceLocation id, JsonObject json) {
            ShapelessRecipe recipe = net.minecraft.world.item.crafting.RecipeSerializer.SHAPELESS_RECIPE.fromJson(id,
                    json);
            return new ShapelessConsumingRecipe(recipe.getId(), recipe.getGroup(), recipe.category(),
                    recipe.getResultItem(RegistryAccess.EMPTY), recipe.getIngredients());
        }

        @Override
        public ShapelessConsumingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ShapelessRecipe recipe = net.minecraft.world.item.crafting.RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id,
                    buffer);
            return new ShapelessConsumingRecipe(recipe.getId(), recipe.getGroup(), recipe.category(),
                    recipe.getResultItem(RegistryAccess.EMPTY), recipe.getIngredients());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ShapelessConsumingRecipe recipe) {
            net.minecraft.world.item.crafting.RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buffer, recipe);
        }
    }
}