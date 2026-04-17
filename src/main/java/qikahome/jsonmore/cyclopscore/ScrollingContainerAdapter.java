/*
The MIT License (MIT)

Copyright (c) 2015 Cyclops

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package qikahome.jsonmore.cyclopscore;

import com.google.common.collect.Lists;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import qikahome.jsonmore.lib.IFlexContainer;
import qikahome.jsonmore.lib.MultiContainer;

import org.cyclops.cyclopscore.inventory.LargeInventory;
import org.cyclops.cyclopscore.inventory.container.ContainerExtended;
import org.cyclops.cyclopscore.inventory.container.ScrollingInventoryContainer;
import org.cyclops.cyclopscore.inventory.slot.SlotExtended;

import java.util.Collections;
import java.util.List;

import static qikahome.jsonmore.JsonMore.LOGGER;

/**
 * Container for the {@link org.cyclops.colossalchests.block.ColossalChest}.
 * @author rubensworks
 *
 */
public class ScrollingContainerAdapter extends ScrollingInventoryContainer<Slot> implements IFlexContainer {

    private static final int INVENTORY_OFFSET_X = 9;
    private static final int INVENTORY_OFFSET_Y = 112;

    private static final int CHEST_INVENTORY_OFFSET_X = 9;
    private static final int CHEST_INVENTORY_OFFSET_Y = 18;
    /**
     * Amount of visible rows in the chest.
     */
    public static final int CHEST_INVENTORY_ROWS = 5;
    /**
     * Amount of columns in the chest.
     */
    public static final int CHEST_INVENTORY_COLUMNS = 9;


    private final List<Slot> chestSlots;

    public ScrollingContainerAdapter(int id, Inventory playerInventory, FriendlyByteBuf data) {
        this(id, playerInventory, new LargeInventory(data.readVarInt(), 64));
    }

    public ScrollingContainerAdapter(int id, Inventory playerInventory, Container inventory) {
        super(CyclopsCorePlugin.SCROLLING_CONTAINER_MENU.get(), id, playerInventory, inventory, Collections.<Slot>emptyList(), (item, pattern) -> true);
        //LOGGER.info("createAdapterMenu");
        this.chestSlots = Lists.newArrayListWithCapacity(getSizeInventory());
        this.addChestSlots(getSizeInventory() / CHEST_INVENTORY_COLUMNS, CHEST_INVENTORY_COLUMNS);
        this.addPlayerInventory(playerInventory, INVENTORY_OFFSET_X, INVENTORY_OFFSET_Y);
        updateFilter("");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Slot> getUnfilteredItems() {
        return this.chestSlots;
    }

    protected void addChestSlots(int rows, int columns) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                Slot slot = makeSlot(inventory, column + row * columns, CHEST_INVENTORY_OFFSET_X + column * 18, CHEST_INVENTORY_OFFSET_Y + row * 18);
                addSlot(slot);
                chestSlots.add(slot);
            }
        }
    }

    protected Slot makeSlot(Container inventory, int index, int row, int column) {
        return new SlotExtended(inventory, index, row, column);
    }

    @Override
    public int getColumns() {
        return CHEST_INVENTORY_COLUMNS;
    }

    @Override
    public int getPageSize() {
        return CHEST_INVENTORY_ROWS;
    }

    protected void disableSlot(int slotIndex) {
        Slot slot = getSlot(slotIndex);
        // Yes I know this is ugly.
        // If you are reading this and know a better way, please tell me.
        ContainerExtended.setSlotPosX(slot, Integer.MIN_VALUE);
        ContainerExtended.setSlotPosY(slot, Integer.MIN_VALUE);
    }

    protected void enableSlot(int slotIndex, int row, int column) {
        Slot slot = getSlot(slotIndex);
        // Yes I know this is ugly.
        // If you are reading this and know a better way, please tell me.
        ContainerExtended.setSlotPosX(slot, CHEST_INVENTORY_OFFSET_X + column * 18);
        ContainerExtended.setSlotPosY(slot, CHEST_INVENTORY_OFFSET_Y + row * 18);
    }

    @Override
    public void onScroll(int firstRow) {
        for(int i = 0; i < getUnfilteredItemCount(); i++) {
            disableSlot(i);
        }
        super.onScroll(firstRow);
    }

    @Override
    protected void enableElementAt(int visibleIndex, int elementIndex, Slot element) {
        super.enableElementAt(visibleIndex, elementIndex, element);
        int column = visibleIndex % getColumns();
        int row = (visibleIndex - column) / getColumns();
        enableSlot(elementIndex, row, column);
    }

    @Override
    public boolean isThisContainer(Container container) {
        return MultiContainer.contains(this.inventory, container);
    }
}