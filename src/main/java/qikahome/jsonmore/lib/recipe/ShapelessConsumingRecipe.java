package qikahome.jsonmore.lib.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

public class ShapelessConsumingRecipe extends ShapelessRecipe {
    public ShapelessConsumingRecipe(ResourceLocation id, String group, CraftingBookCategory category,
            ItemStack result, NonNullList<Ingredient> ingredients) {
        super(id, group, category, result, ingredients);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);

        List<Ingredient> ingredients = new ArrayList<>(getIngredients());
        Map<Integer, Ingredient> matchedSlots = new HashMap<>();

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty())
                continue;
            for (int j = 0; j < ingredients.size(); j++) {
                Ingredient ingredient = ingredients.get(j);
                if (ingredient.test(stack)) {
                    matchedSlots.put(i, ingredient);
                    ingredients.set(j, ingredient.of());
                    break;
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
