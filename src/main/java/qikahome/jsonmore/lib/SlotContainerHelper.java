package qikahome.jsonmore.lib;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * {@code net.minecraft.world.ContainerHelper} 的替代实现。
 * <p>
 * 原版把槽位写成 TAG_Byte（{@code putByte("Slot", (byte) i)}），槽位 ≥ 256 时会被截断成低位值并与
 * 低槽位撞号（256 → 0），读档时后写入的条目覆盖先前的，导致物品丢失。
 * 这里改成非负 TAG_Long（等价于无符号长整型），槽位不再有上限。
 */
public final class SlotContainerHelper {
    private SlotContainerHelper() {
    }

    public static CompoundTag saveAllItems(CompoundTag tag, NonNullList<ItemStack> items,
            HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putLong("Slot", i);
                list.add(stack.save(registries, entry));
            }
        }
        tag.put("Items", list);
        return tag;
    }

    public static void loadAllItems(CompoundTag tag, NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            long slot = decodeSlot(entry.getLong("Slot"));
            if (slot >= 0 && slot < items.size()) {
                items.set((int) slot, ItemStack.parse(registries, entry).orElse(ItemStack.EMPTY));
            }
        }
    }

    /**
     * 返回存档中出现的最大槽位，没有条目时返回 -1。
     * <p>
     * 用于读档时把容器扩到实际最大槽位，避免 loadAllItems 的边界检查静默丢弃旧存档里越界的条目。
     */
    public static int maxSlot(CompoundTag tag) {
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        long max = -1;
        for (int i = 0; i < list.size(); i++) {
            long slot = decodeSlot(list.getCompound(i).getLong("Slot"));
            if (slot > max) {
                max = slot;
            }
        }
        return max < 0 ? -1 : (int) Math.min(max, Integer.MAX_VALUE - 1);
    }

    /**
     * 旧存档（原版 ContainerHelper 写入）的槽位是 TAG_Byte，其中 128~255 存为负数，按无符号还原；
     * 本类写入的是非负 TAG_Long，原样使用。
     */
    private static long decodeSlot(long raw) {
        return raw < 0 ? raw & 0xFFL : raw;
    }
}
