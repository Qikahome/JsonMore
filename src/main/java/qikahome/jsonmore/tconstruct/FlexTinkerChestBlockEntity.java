package qikahome.jsonmore.tconstruct;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.tables.block.entity.chest.AbstractChestBlockEntity;
import slimeknights.tconstruct.tables.block.entity.inventory.IChestItemHandler;
import slimeknights.tconstruct.tables.block.entity.inventory.ScalingChestItemHandler;

public class FlexTinkerChestBlockEntity extends AbstractChestBlockEntity {

    public FlexTinkerChestBlockEntity(BlockPos pos, BlockState state) {
        super(TConstructPlugin.TINKER_CHEST_TILE.get(), pos, state, null, null);
    }

    public FlexTinkerChestBlockEntity(BlockPos pos, BlockState state, Component name, IChestItemHandler itemHandler) {
        super(TConstructPlugin.TINKER_CHEST_TILE.get(), pos, state, name, itemHandler);
    }

    public static BlockEntitySupplier<FlexTinkerChestBlockEntity> getSupplier(String name, String slotMode, int max_slots,
            int slot_stack_limit,
            boolean allow_duplicate_item,
            List<String> filters) {
        NonNullList<TagKey<Item>> tagFilters = NonNullList.create();
        filters.forEach(f -> tagFilters.add(TagKey.create(Registries.ITEM, new ResourceLocation(f))));
        if (slotMode.equals("scaling")) {
            return (pos, state) -> new FlexTinkerChestBlockEntity(pos, state, Component.translatable(name),
                    new FlexScalingChestItemHandler(max_slots, slot_stack_limit, allow_duplicate_item, tagFilters));
        } else if (slotMode.equals("fixed")) {
            return (pos, state) -> new FlexTinkerChestBlockEntity(pos, state, Component.translatable(name),
                    new FlexTinkersChestItemHandler(max_slots, slot_stack_limit));
        }
        throw new IllegalArgumentException("Invalid slot mode: " + slotMode);
    }

    /** Item handler for part chest-like chests */
    public static class FlexScalingChestItemHandler extends ScalingChestItemHandler {

        public FlexScalingChestItemHandler(int max_slots, int slot_stack_limit, boolean allow_duplicate_item,
                NonNullList<TagKey<Item>> filters) {
            super(max_slots);
            this.slot_stack_limit = slot_stack_limit;
            this.allow_duplicate_item = allow_duplicate_item;
            this.filters = filters;
        }

        private final NonNullList<TagKey<Item>> filters;
        private final boolean allow_duplicate_item;
        private final int slot_stack_limit;

        @Override
        public int getSlotLimit(int slot) {
            return slot_stack_limit;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // check if there is no other slot containing that item
            if (!allow_duplicate_item) {
                for (int i = 0; i < this.getSlots(); i++) {
                    // don't compare count
                    if (ItemStack.isSameItemSameTags(stack, this.getStackInSlot(i))) {
                        return i == slot; // only allowed in the same slot
                    }
                }
            }
            return filters.stream().allMatch(tag -> stack.is(tag));
        }
    }

    /** Item handler for tinkers chest-like chests */
    public static class FlexTinkersChestItemHandler extends ItemStackHandler implements IChestItemHandler {

        private MantleBlockEntity parent;
        private final int slot_stack_limit;

        @Override
        public void setParent(MantleBlockEntity parent) {
            this.parent = parent;
        }

        public FlexTinkersChestItemHandler(int size, int slot_stack_limit) {
            super(size);
            this.slot_stack_limit = slot_stack_limit;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot_stack_limit;
        }

        @Override
        public int getVisualSize() {
            return getSlots();
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (parent != null) {
                parent.setChangedFast();
            }
        }
    }
}
