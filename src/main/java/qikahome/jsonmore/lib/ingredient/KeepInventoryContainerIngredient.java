package qikahome.jsonmore.lib.ingredient;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import qikahome.jsonmore.JsonMore;
import qikahome.jsonmore.Utils;

public enum KeepInventoryContainerIngredient implements ICustomIngredient {
    MAY {
        {
            ItemStack stack = new ItemStack(Items.SHULKER_BOX);
            stack.set(DataComponents.CUSTOM_NAME, Component.translatable("ingredient.jsonmore.container.may"));
            items = Stream.of(stack);
        }

        @Override
        public boolean test(@Nullable ItemStack stack) {
            return super.test(stack) && !stack.canFitInsideContainerItems();
        }

    },
    CONTAINS {
        {
            ItemStack stack = new ItemStack(Items.SHULKER_BOX);
            stack.set(DataComponents.CUSTOM_NAME, Component.translatable("ingredient.jsonmore.container.contains"));
            items = Stream.of(stack);
        }

        @Override
        public boolean test(@Nullable ItemStack stack) {
            return super.test(stack) && !hasItems(stack);
        }
    };

    public static final ResourceLocation ID = ResourceLocation.parse("jsonmore:keep_inventory_container");
    public static final MapCodec<KeepInventoryContainerIngredient> CODEC = Utils.enumCodecIgnoreCase(KeepInventoryContainerIngredient.class).fieldOf("mode");
    public static final DeferredHolder<IngredientType<?>, IngredientType<KeepInventoryContainerIngredient>> TYPE = JsonMore.INGREDIENT_TYPES
            .register(ID.getPath(), () -> new IngredientType<>(CODEC));
    protected Stream<ItemStack> items;

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return true;
    }

    private static boolean hasItems(ItemStack stack) {
        var blockEntityTag = stack.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
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
    public Stream<ItemStack> getItems() {
        return items;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    public static void register() {
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE.get();
    }
}
