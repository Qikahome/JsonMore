package qikahome.jsonmore.minecraft;

import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import qikahome.jsonmore.lib.IFlexContainer;
import qikahome.jsonmore.lib.MultiContainer;

import javax.annotation.Nullable;

import static qikahome.jsonmore.JsonMore.LOGGER;

public class FilteredChestMenu extends AbstractContainerMenu implements IFlexContainer {
    private final Container container;
    private final int containerRows;
    @Nullable
    private final Container blockEntity;

    private FilteredChestMenu(MenuType<?> type, int containerId, Inventory playerInventory, Container container,
            int rows) {
        super(type, containerId);
        this.container = container;
        this.containerRows = rows;
        this.blockEntity = container;
        checkContainerSize(container, rows * 9);

        int containerY = 18;

        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(
                        new FilteredSlot(container, col + row * 9, 8 + col * 18, containerY + row * 18, blockEntity));
            }
        }

        int playerInventoryY = containerY + rows * 18 + 14;

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInventoryY + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, playerInventoryY + 58));
        }
        this.container.startOpen(playerInventory.player);
    }

    public static FilteredChestMenu create(int containerId, Inventory playerInventory, Container container,
            int containerSize) {
        int rows = Math.max(1, Math.min(6, (int) Math.ceil((double) containerSize / 9)));
        MenuType<?> type = getMenuType(rows);
        return new FilteredChestMenu(type, containerId, playerInventory, container, rows);
    }

    private static MenuType<?> getMenuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotItem = slot.getItem();
        ItemStack result = slotItem.copy();

        int containerSlots = this.container.getContainerSize();

        if (index < containerSlots) {
            if (!this.moveItemStackTo(slotItem, containerSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(slotItem, 0, containerSlots, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotItem.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }

    public Container getContainer() {
        return container;
    }

    public int getContainerRows() {
        return containerRows;
    }

    private static class FilteredSlot extends Slot {
        @Nullable
        private final Container blockEntity;

        public FilteredSlot(Container container, int slot, int x, int y, @Nullable Container blockEntity) {
            super(container, slot, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (blockEntity != null) {
                return blockEntity.canPlaceItem(this.getSlotIndex(), stack);
            }
            return super.mayPlace(stack);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    @Override
    public boolean isThisContainer(Container container) {
        return MultiContainer.contains(this.container, container);
    }
}
