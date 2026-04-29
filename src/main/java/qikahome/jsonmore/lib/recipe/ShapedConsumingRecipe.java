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

public class ShapedConsumingRecipe extends ShapedRecipe {
    public ShapedConsumingRecipe(ResourceLocation id, String group, CraftingBookCategory category,
            int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, boolean showNotification) {
        super(id, group, category, width, height, ingredients, result, showNotification);
    }

    public ShapedConsumingRecipe(ResourceLocation id, String group, CraftingBookCategory category,
            int width, int height, NonNullList<Ingredient> ingredients, ItemStack result) {
        this(id, group, category, width, height, ingredients, result, true);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);

        NonNullList<Ingredient> recipeIngredients = getIngredients();
        int recipeWidth = getWidth();
        int recipeHeight = getHeight();
        int containerWidth = container.getWidth();
        int containerHeight = container.getHeight();

        for (int startX = 0; startX <= containerWidth - recipeWidth; startX++) {
            for (int startY = 0; startY <= containerHeight - recipeHeight; startY++) {
                if (matches(container, startX, startY, false) || matches(container, startX, startY, true)) {
                    boolean mirrored = matches(container, startX, startY, true);

                    List<Ingredient> ingredients = new ArrayList<>(recipeIngredients);
                    Map<Integer, Ingredient> matchedSlots = new HashMap<>();

                    for (int row = 0; row < recipeHeight; row++) {
                        for (int col = 0; col < recipeWidth; col++) {
                            int ingredientIndex;
                            if (mirrored) {
                                ingredientIndex = (recipeWidth - col - 1) + row * recipeWidth;
                            } else {
                                ingredientIndex = col + row * recipeWidth;
                            }

                            if (ingredientIndex >= ingredients.size())
                                continue;

                            Ingredient ingredient = ingredients.get(ingredientIndex);
                            if (ingredient.isEmpty())
                                continue;

                            int containerSlot = (startY + row) * containerWidth + (startX + col);
                            if (containerSlot >= container.getContainerSize())
                                continue;

                            ItemStack stack = container.getItem(containerSlot);
                            if (stack.isEmpty())
                                continue;

                            if (ingredient.test(stack)) {
                                matchedSlots.put(containerSlot, ingredient);
                                ingredients.set(ingredientIndex, ingredient.of());
                            }
                        }
                    }

                    for (var entry : matchedSlots.entrySet()) {
                        int slot = entry.getKey();
                        ItemStack stack = container.getItem(slot);
                        Ingredient ingredient = entry.getValue();
                        ItemStack remaining = SelfConsumingIngredient.consume(ingredient, stack);
                        remainingItems.set(slot, remaining);
                    }

                    return remainingItems;
                }
            }
        }

        return remainingItems;
    }

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
            ShapedRecipe recipe = net.minecraft.world.item.crafting.RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer);
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
