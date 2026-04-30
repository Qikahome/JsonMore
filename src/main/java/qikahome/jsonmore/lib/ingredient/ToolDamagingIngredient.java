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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.fml.ModList;
import qikahome.jsonmore.tconstruct.TConstructPlugin;

public class ToolDamagingIngredient extends SelfConsumingIngredient {
    public static final ResourceLocation ID = new ResourceLocation("jsonmore:tool_damaging");
    private final int damage;
    @Nullable
    private final ItemStack remainder_override;

    public ToolDamagingIngredient(Ingredient ingredient, int damage) {
        this(ingredient, damage, null);
    }

    public ToolDamagingIngredient(Ingredient ingredient, int damage, @Nullable ItemStack remainder_override) {
        super(ingredient);
        this.damage = damage;
        this.remainder_override = remainder_override == null ? null : remainder_override.copy();
    }

    @Override
    public ItemStack consume(ItemStack stack) {
        if (stack.isEmpty())
            return stack;
        if (stack.getCount() > 1)
            throw new IllegalArgumentException("ToolDamagingIngredient only consumes single items");
        ItemStack copy = stack.copy();
        ItemStack remainder = remainder_override != null ? remainder_override.copy() : stack.getCraftingRemainingItem();
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
            if (items[i].getMaxDamage()>=damage){
                items[i].setDamageValue(items[i].getMaxDamage()-damage);
            }
        }
        return items;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (super.test(stack)) {
            ModList modList = ModList.get();
            int maxDamage = stack.getMaxDamage();
            if (modList.isLoaded("tconstruct"))
                maxDamage = TConstructPlugin.getRealMaxDamage(stack, maxDamage);
            if (stack.isDamageableItem() && maxDamage - stack.getDamageValue() >= damage)
                return true;
        }
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
        json.addProperty("damage", damage);
        if (remainder_override != null) {
            DataResult<JsonElement> result = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, remainder_override);
            JsonElement element = result.getOrThrow(false, exception -> LOGGER.error(exception));
            json.add("remainder_override", element);
        }
        return json;
    }

    public static class Serializer implements IIngredientSerializer<ToolDamagingIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public ToolDamagingIngredient parse(FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            int damage = buffer.readVarInt();
            boolean hasRemainderOverride = buffer.readBoolean();
            ItemStack remainder_override = null;
            if (hasRemainderOverride) {
                remainder_override = buffer.readItem();
            }
            return new ToolDamagingIngredient(ingredient, damage, remainder_override);
        }

        @Override
        public ToolDamagingIngredient parse(JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonParseException("Tool damaging ingredient must have 'ingredient' field");
            }

            int damage = GsonHelper.getAsInt(json, "damage", 1);
            ItemStack remainder_override = null;
            if (json.has("remainder_override")) {
                DataResult<ItemStack> result = ItemStack.CODEC.parse(JsonOps.INSTANCE, json.get("remainder_override"));
                remainder_override = result.getOrThrow(false,
                        exception -> new JsonParseException("Invalid remainder_override: " + exception));
            }

            JsonElement ingredientJson = json.get("ingredient");
            Ingredient ingredient = Ingredient.fromJson(ingredientJson);

            return new ToolDamagingIngredient(ingredient, damage, remainder_override);
        }

        @Override
        public void write(FriendlyByteBuf buffer, ToolDamagingIngredient ingredient) {
            ingredient.ingredient.toNetwork(buffer);
            buffer.writeVarInt(ingredient.damage);
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