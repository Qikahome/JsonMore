package qikahome.jsonmore.lib.ingredient;

import static qikahome.jsonmore.JsonMore.LOGGER;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;

public class CountedIngredient extends SelfConsumingIngredient {
    public static final ResourceLocation ID = new ResourceLocation("jsonmore:counted");
    private final int count;
    @Nullable
    private final ItemStack remainder_override;

    public CountedIngredient(Ingredient ingredient, int count) {
        this(ingredient, count, null);
    }

    public CountedIngredient(Ingredient ingredient, int count, ItemStack remainder_override) {
        super(ingredient);
        this.count = count;
        this.remainder_override = remainder_override == null ? null : remainder_override.copy();
    }

    @Override
    public ItemStack consume(ItemStack stack) {
        if (stack.isEmpty())
            return stack;
        ItemStack remainder = remainder_override != null ? remainder_override.copy() : stack.getCraftingRemainingItem().copy();
        stack.shrink(count-1); // Unsafe
        remainder.setCount(remainder.getCount() * count);
        return remainder;
    }

    @Override
    public ItemStack[] getItems() {
        ItemStack[] items = super.getItems();
        for (int i = 0; i < items.length; i++) {
            items[i].setCount(count);
        }
        return items;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (super.test(stack))
            if (stack.getCount() >= count)
                return true;
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", ID.toString());
        json.add("ingredient", ingredient.toJson());
        json.addProperty("count", count);
        if (remainder_override != null) {
            DataResult<JsonElement> result = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, remainder_override);
            JsonElement element = result.getOrThrow(false, exception -> LOGGER.error(exception));
            json.add("remainder_override", element);
        }
        return json;
    }

    public static class Serializer implements IIngredientSerializer<CountedIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public CountedIngredient parse(FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            int count = buffer.readVarInt();
            boolean hasRemainderOverride = buffer.readBoolean();
            ItemStack remainder_override = null;
            if (hasRemainderOverride) {
                remainder_override = buffer.readItem();
            }
            return new CountedIngredient(ingredient, count, remainder_override);
        }

        @Override
        public CountedIngredient parse(JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonParseException("Counted ingredient must have 'ingredient' field");
            }

            int count = GsonHelper.getAsInt(json, "count", 1);
            ItemStack remainder_override = null;
            if (json.has("remainder_override")) {
                DataResult<ItemStack> result = ItemStack.CODEC.parse(JsonOps.INSTANCE, json.get("remainder_override"));
                remainder_override = result.getOrThrow(false,
                        exception -> new JsonParseException("Invalid remainder_override: " + exception));
            } else {
                remainder_override = null;
            }

            JsonElement ingredientJson = json.get("ingredient");
            Ingredient ingredient = Ingredient.fromJson(ingredientJson);

            return new CountedIngredient(ingredient, count, remainder_override);
        }

        @Override
        public void write(FriendlyByteBuf buffer, CountedIngredient ingredient) {
            ingredient.ingredient.toNetwork(buffer);
            buffer.writeVarInt(ingredient.count);
            buffer.writeBoolean(ingredient.remainder_override != null);
            if (ingredient.remainder_override != null) {
                buffer.writeItemStack(ingredient.remainder_override, false);
            }
        }
    }

    public static void register() {
        CraftingHelper.register(ID, Serializer.INSTANCE);
    }
}
