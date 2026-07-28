package qikahome.jsonmore.autosizedgui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import qikahome.autosizedgui.screen.element.ItemSlot;
import static qikahome.jsonmore.autosizedgui.AutoSizedGUIPlugin.AUTO_SIZED_MENU;
import qikahome.jsonmore.lib.IFlexContainer;
import qikahome.jsonmore.lib.MultiContainer;

public class AutoSizedMenu extends AbstractContainerMenu implements IFlexContainer {
    private final Container container;
    public final int containerSize;

    private AutoSizedMenu(MenuType<?> type, int containerId, Inventory playerInventory, Container container) {
        super(type, containerId);
        this.container = container;
        this.containerSize = container.getContainerSize();

        for (int slot = 0; slot < containerSize; ++slot) {
            this.addSlot(new ItemSlot(container, slot, slot));
        }

        for (int i = 9; i < 36; ++i) {
            this.addSlot(new ItemSlot(playerInventory, i, i));
        }

        for (int i = 0; i < 9; ++i) {
            this.addSlot(new ItemSlot(playerInventory, i, i));
        }
        this.container.startOpen(playerInventory.player);
    }

    public static AutoSizedMenu create(int containerId, Inventory playerInventory, Container container) {
        return new AutoSizedMenu(AUTO_SIZED_MENU.get(), containerId, playerInventory, container);
    }

    public static AutoSizedMenu create(int id, Inventory playerInventory, FriendlyByteBuf data) {
        return create(id, playerInventory, new SimpleContainer(data.readVarInt()));
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
