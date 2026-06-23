package qikahome.jsonmore.lib.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import qikahome.jsonmore.lib.ingredient.SelfConsumingIngredient;

import static qikahome.jsonmore.lib.recipe.ShapelessConsumingRecipe.backtrackMatch;

public class ShapedConsumingRecipe extends ShapedRecipe implements IConsumingRecipe {

    private transient Map<Integer, Ingredient> lastMatch;

    public ShapedConsumingRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern,
            ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
    }

    public ShapedConsumingRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern,
            ItemStack result) {
        this(group, category, pattern, result, true);
    }

    public static ShapedConsumingRecipe fromVanilla(ShapedRecipe recipe) {
        return new ShapedConsumingRecipe(recipe.getGroup(), recipe.category(),
                recipe.pattern,
                recipe.getResultItem(RegistryAccess.EMPTY), recipe.showNotification());
    }

    private Map<Integer, Ingredient> matchIngredients(CraftingInput container) {
        if (lastMatch != null && isCacheValid(container)) {
            return lastMatch;
        }

        int containerWidth = container.width();
        int containerHeight = container.height();
        List<Ingredient> ingredients = getIngredients().stream()
                .filter(ing -> !ing.isEmpty())
                .collect(Collectors.toList());

        if (containerWidth == 0 || containerHeight == 0) {
            List<Integer> nonEmptySlots = new ArrayList<>();
            for (int i = 0; i < container.size(); i++) {
                if (!container.getItem(i).isEmpty()) {
                    nonEmptySlots.add(i);
                }
            }
            if (nonEmptySlots.size() != ingredients.size()) {
                throw new IllegalArgumentException("Item count mismatch: expected " + ingredients.size() +
                        ", got " + nonEmptySlots.size());
            }
            Map<Integer, Ingredient> slotToIngredient = new HashMap<>();
            boolean[] slotUsed = new boolean[container.size()];
            boolean found = backtrackMatch(0, ingredients, nonEmptySlots, slotUsed, slotToIngredient, container);
            if (!found) {
                throw new IllegalArgumentException("Cannot match recipe ingredients (unordered)");
            }
            lastMatch = slotToIngredient;
            return slotToIngredient;
        }

        int recipeWidth = pattern.width();
        int recipeHeight = pattern.height();
        for (int startX = 0; startX <= containerWidth - recipeWidth; startX++) {
            for (int startY = 0; startY <= containerHeight - recipeHeight; startY++) {
                for (boolean mirrored : new boolean[]{false, true}) {
                    if (!matchesShape(container, startX, startY, mirrored))
                        continue;

                    Map<Integer, Ingredient> matchedSlots = new HashMap<>();
                    List<Ingredient> remainingIngredients = new ArrayList<>(ingredients);

                    for (int row = 0; row < recipeHeight; row++) {
                        for (int col = 0; col < recipeWidth; col++) {
                            int ingredientIndex = mirrored
                                    ? (recipeWidth - col - 1) + row * recipeWidth
                                    : col + row * recipeWidth;
                            Ingredient originalIng = getIngredients().get(ingredientIndex);
                            if (originalIng.isEmpty())
                                continue;

                            int matchedIdx = -1;
                            for (int idx = 0; idx < remainingIngredients.size(); idx++) {
                                if (remainingIngredients.get(idx) == originalIng) {
                                    matchedIdx = idx;
                                    break;
                                }
                            }
                            if (matchedIdx == -1) {
                                matchedSlots = null;
                                break;
                            }

                            int containerSlot = (startY + row) * containerWidth + (startX + col);
                            if (containerSlot >= container.size()) {
                                matchedSlots = null;
                                break;
                            }
                            ItemStack stack = container.getItem(containerSlot);
                            if (stack.isEmpty() || !originalIng.test(stack)) {
                                matchedSlots = null;
                                break;
                            }

                            matchedSlots.put(containerSlot, originalIng);
                            remainingIngredients.remove(matchedIdx);
                        }
                        if (matchedSlots == null) break;
                    }

                    if (matchedSlots != null && remainingIngredients.isEmpty()) {
                        lastMatch = matchedSlots;
                        return matchedSlots;
                    }
                }
            }
        }
        throw new IllegalArgumentException("Cannot match recipe ingredients (shaped)");
    }

    private boolean matchesShape(CraftingInput container, int startX, int startY, boolean mirrored) {
        NonNullList<Ingredient> ingredients = getIngredients();
        int recipeWidth = pattern.width();
        int recipeHeight = pattern.height();
        int containerWidth = container.width();

        for (int i = 0; i < containerWidth; i++) {
            for (int j = 0; j < container.height(); j++) {
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

    private boolean isCacheValid(CraftingInput container) {
        if (lastMatch == null) return false;
        for (Map.Entry<Integer, Ingredient> entry : lastMatch.entrySet()) {
            int slot = entry.getKey();
            if (slot >= container.size()) return false;
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) return false;
            if (!entry.getValue().test(stack)) return false;
        }
        for (int i = 0; i < container.size(); i++) {
            if (!container.getItem(i).isEmpty() && !lastMatch.containsKey(i)) {
                return false;
            }
        }
        long nonEmptyIngredientCount = getIngredients().stream().filter(ing -> !ing.isEmpty()).count();
        return lastMatch.size() == nonEmptyIngredientCount;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput container) {
        NonNullList<ItemStack> remainingItems = NonNullList.withSize(container.size(), ItemStack.EMPTY);
        Map<Integer, Ingredient> slotToIngredient = matchIngredients(container);

        Player player = CommonHooks.getCraftingPlayer();
        ServerLevel level = null;
        if (player != null && player.level() instanceof ServerLevel serverLevel)
            level = serverLevel;
        if (level == null) {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) level = server.overworld();
            else return remainingItems; // 客户端预览，跳过消耗逻辑
        }

        for (var entry : slotToIngredient.entrySet()) {
            int slot = entry.getKey();
            ItemStack stack = container.getItem(slot);
            Ingredient ingredient = entry.getValue();
            ItemStack remaining = SelfConsumingIngredient.consume(ingredient, stack, level, player);
            remainingItems.set(slot, remaining);
        }
        return remainingItems;
    }

    @Override
    public ItemStack assemble(CraftingInput container, HolderLookup.Provider lookup) {
        ItemStack result = super.assemble(container, lookup);
        if (result.isEmpty()) {
            return result;
        }

        Map<Integer, Ingredient> slotToIngredient = matchIngredients(container);

        for (var entry : slotToIngredient.entrySet()) {
            int slot = entry.getKey();
            ItemStack inputStack = container.getItem(slot);
            Ingredient ingredient = entry.getValue();
            SelfConsumingIngredient.outputModify(ingredient, inputStack, result);
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements RecipeSerializer<ShapedConsumingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<ShapedConsumingRecipe> CODEC = RecipeSerializer.SHAPED_RECIPE
                .codec().xmap(ShapedConsumingRecipe::fromVanilla, a -> a);

        private static final StreamCodec<RegistryFriendlyByteBuf, ShapedConsumingRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, recipe) -> RecipeSerializer.SHAPED_RECIPE.streamCodec().encode(buf, recipe),
                buf -> ShapedConsumingRecipe.fromVanilla(RecipeSerializer.SHAPED_RECIPE.streamCodec().decode(buf)));

        @Override
        public MapCodec<ShapedConsumingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShapedConsumingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
