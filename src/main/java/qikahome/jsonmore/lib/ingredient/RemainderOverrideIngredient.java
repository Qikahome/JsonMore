package qikahome.jsonmore.lib.ingredient;

import static qikahome.jsonmore.JsonMore.LOGGER;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class RemainderOverrideIngredient extends SelfConsumingIngredient {
    public static final ResourceLocation ID = new ResourceLocation("jsonmore:remainder_override");

    private final ItemStack remainderOverride;

    public RemainderOverrideIngredient(Ingredient ingredient, ItemStack remainderOverride) {
        super(ingredient);
        if (Ingredients.unwrap(ingredient) instanceof SelfConsumingIngredient)
            throw new IllegalArgumentException(
                    "RemainderOverrideIngredient's inner ingredient must not be a SelfConsumingIngredient");
        this.remainderOverride = remainderOverride.copy();
    }

    @Override
    public ItemStack consume(ItemStack stack) {
        SelfConsumingIngredient.consume(ingredient, stack);
        return remainderOverride.copy();
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements CustomIngredientSerializer<RemainderOverrideIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public ResourceLocation getIdentifier() {
            return ID;
        }

        @Override
        public RemainderOverrideIngredient read(FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            ItemStack remainderOverride = buffer.readItem();
            return new RemainderOverrideIngredient(ingredient, remainderOverride);
        }

        @Override
        public RemainderOverrideIngredient read(JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonParseException("RemainderOverrideIngredient must have 'ingredient' field");
            }
            if (!json.has("remainder_override")) {
                throw new JsonParseException("RemainderOverrideIngredient must have 'remainder_override' field");
            }

            Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"));

            try {
                DataResult<ItemStack> result = ItemStack.CODEC.parse(JsonOps.INSTANCE,
                        json.get("remainder_override"));
                ItemStack remainderOverride = result.getOrThrow(false, String::new);
                return new RemainderOverrideIngredient(ingredient, remainderOverride);
            } catch (RuntimeException e) {
                throw new JsonParseException("Invalid remainder_override: " + e.getMessage(), e);
            }
        }

        @Override
        public void write(FriendlyByteBuf buffer, RemainderOverrideIngredient ingredient) {
            ingredient.ingredient.toNetwork(buffer);
            buffer.writeItem(ingredient.remainderOverride);
        }

        @Override
        public void write(JsonObject json, RemainderOverrideIngredient ingredient) {
            json.add("ingredient", ingredient.ingredient.toJson());
            DataResult<JsonElement> result = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, ingredient.remainderOverride);
            JsonElement element = result.getOrThrow(false, exception -> LOGGER.error(exception));
            json.add("remainder_override", element);
        }
    }

    public static void register() {
        CustomIngredientSerializer.register(Serializer.INSTANCE);
    }
}
