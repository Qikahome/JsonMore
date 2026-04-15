package qikahome.jsonmore.lib;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;

public class NotIngredient extends AbstractIngredient {
    public static final ResourceLocation ID = new ResourceLocation("jsonmore:not");

    private final Ingredient ingredient;

    private NotIngredient(Ingredient ingredient) {
        super(Stream.empty());
        this.ingredient = ingredient;
    }

    public static Ingredient of(Ingredient ingredient) {
        if (ingredient instanceof NotIngredient not) {
            return not.ingredient;
        }
        return new NotIngredient(ingredient);
    }

    @Override
    public ItemStack[] getItems() {
        return new ItemStack[0];
    }

    @Override
    public boolean isEmpty() {
        return false; // 因为 NotIngredient 通常不是空的（除非内部 ingredient 匹配所有物品，但这种情况极少）
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return !ingredient.test(stack);
    }

    @Override
    public boolean isSimple() {
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
        return json;
    }

    public static class Serializer implements IIngredientSerializer<NotIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public NotIngredient parse(FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            return new NotIngredient(ingredient);
        }

        @Override
        public NotIngredient parse(JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonParseException("Not ingredient must have 'ingredient' field");
            }

            JsonElement ingredientJson = json.get("ingredient");
            Ingredient ingredient = Ingredient.fromJson(ingredientJson);

            return new NotIngredient(ingredient);
        }

        @Override
        public void write(FriendlyByteBuf buffer, NotIngredient ingredient) {
            ingredient.ingredient.toNetwork(buffer);
        }
    }

    public static void register() {
        CraftingHelper.register(ID, Serializer.INSTANCE);
    }
}
