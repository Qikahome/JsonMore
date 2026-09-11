package qikahome.jsonmore.lib.ingredient;

import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class KeepInventoryContainerIngredient implements CustomIngredient {
    public static final ResourceLocation ID = new ResourceLocation("jsonmore:keep_inventory_container");

    public enum Mode {
        MAY,
        CONTAINS
    }

    private final Mode mode;

    public KeepInventoryContainerIngredient(Mode mode) {
        this.mode = mode;
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (mode == Mode.MAY) {
            return !stack.getItem().canFitInsideContainerItems();
        }

        if (mode == Mode.CONTAINS) {
            return hasItems(stack);
        }

        return false;
    }

    private boolean hasItems(ItemStack stack) {
        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        if (blockEntityTag == null) {
            return false;
        }

        if (blockEntityTag.contains("Items", 9)) {
            ListTag items = blockEntityTag.getList("Items", 10);
            return !items.isEmpty();
        }

        return false;
    }

    @Override
    public List<ItemStack> getMatchingStacks() {
        // 与 Forge 原版一致：该原料没有展示物品，仅靠 test 匹配（配合 NotIngredient 展示）。
        return List.of();
    }

    @Override
    public boolean requiresTesting() {
        return true;
    }

    @Override
    public CustomIngredientSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static class Serializer implements CustomIngredientSerializer<KeepInventoryContainerIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public ResourceLocation getIdentifier() {
            return ID;
        }

        @Override
        public KeepInventoryContainerIngredient read(FriendlyByteBuf buffer) {
            String modeStr = buffer.readUtf();
            Mode mode = Mode.valueOf(modeStr.toUpperCase());
            return new KeepInventoryContainerIngredient(mode);
        }

        @Override
        public KeepInventoryContainerIngredient read(JsonObject json) {
            String modeStr = json.has("mode") ? json.get("mode").getAsString().toUpperCase() : "MAY";
            try {
                Mode mode = Mode.valueOf(modeStr);
                return new KeepInventoryContainerIngredient(mode);
            } catch (IllegalArgumentException e) {
                throw new JsonParseException("Invalid mode: " + modeStr + ". Expected 'may' or 'contains'");
            }
        }

        @Override
        public void write(FriendlyByteBuf buffer, KeepInventoryContainerIngredient ingredient) {
            buffer.writeUtf(ingredient.mode.name().toLowerCase());
        }

        @Override
        public void write(JsonObject json, KeepInventoryContainerIngredient ingredient) {
            json.addProperty("mode", ingredient.mode.name().toLowerCase());
        }
    }

    public static void register() {
        CustomIngredientSerializer.register(Serializer.INSTANCE);
    }
}
