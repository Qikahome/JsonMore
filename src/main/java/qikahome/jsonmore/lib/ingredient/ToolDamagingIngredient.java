package qikahome.jsonmore.lib.ingredient;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class ToolDamagingIngredient extends SelfConsumingIngredient {
    public static final ResourceLocation ID = new ResourceLocation("jsonmore:tool_damaging");
    private final int damage;

    public ToolDamagingIngredient(Ingredient ingredient, int damage) {
        super(ingredient);
        this.damage = damage;
    }

    @Override
    public ItemStack consume(ItemStack stack) {
        if (stack.isEmpty())
            return stack;
        if (stack.getCount() > 1)
            throw new IllegalArgumentException("ToolDamagingIngredient only consumes single items");
        ItemStack copy = stack.copy();
        ItemStack remainder = super.consume(stack);
        // 1.20.1 原版签名是 hurt(int, RandomSource, ServerPlayer)，第三参传 null 即可。
        if (copy.hurt(damage, RandomSource.create(), null)) {
            copy = remainder;
        }
        return copy;
    }

    @Override
    public ItemStack[] getItems() {
        ItemStack[] items = super.getItems();
        for (int i = 0; i < items.length; i++) {
            items[i] = items[i].copy();
            if (items[i].getMaxDamage() >= damage) {
                items[i].setDamageValue(items[i].getMaxDamage() - damage);
            }
        }
        return items;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (super.test(stack)) {
            int maxDamage = stack.getMaxDamage();
            if (stack.isDamageableItem() && maxDamage - stack.getDamageValue() >= damage)
                return true;
        }
        return false;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements CustomIngredientSerializer<ToolDamagingIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public ResourceLocation getIdentifier() {
            return ID;
        }

        @Override
        public ToolDamagingIngredient read(FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            int damage = buffer.readVarInt();
            return new ToolDamagingIngredient(ingredient, damage);
        }

        @Override
        public ToolDamagingIngredient read(JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonParseException("Tool damaging ingredient must have 'ingredient' field");
            }

            int damage = GsonHelper.getAsInt(json, "damage", 1);
            Ingredient ingredient;
            if (json.has("remainder_override")) {
                ingredient = RemainderOverrideIngredient.Serializer.INSTANCE.read(json).toVanilla();
            } else {
                ingredient = Ingredient.fromJson(json.get("ingredient"));
            }

            return new ToolDamagingIngredient(ingredient, damage);
        }

        @Override
        public void write(FriendlyByteBuf buffer, ToolDamagingIngredient ingredient) {
            ingredient.ingredient.toNetwork(buffer);
            buffer.writeVarInt(ingredient.damage);
        }

        @Override
        public void write(JsonObject json, ToolDamagingIngredient ingredient) {
            json.add("ingredient", ingredient.ingredient.toJson());
            json.addProperty("damage", ingredient.damage);
        }
    }

    public static void register() {
        CustomIngredientSerializer.register(Serializer.INSTANCE);
    }
}
