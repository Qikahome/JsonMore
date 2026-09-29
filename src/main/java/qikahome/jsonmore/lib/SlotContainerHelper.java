package qikahome.jsonmore.lib;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * {@code net.minecraft.world.ContainerHelper} 的替代实现。
 * <p>
 * 原版槽位走 {@code ItemStackWithSlot} 的 codec，字段用 {@code ExtraCodecs.UNSIGNED_BYTE}（0~255），
 * 槽位 ≥ 256 时直接编码失败，物品丢失。这里改成非负 TAG_Long（等价于无符号长整型），槽位不再有上限。
 */
public final class SlotContainerHelper {
    private SlotContainerHelper() {
    }

    public static void saveAllItems(ValueOutput output, NonNullList<ItemStack> items) {
        ValueOutput.TypedOutputList<SlotEntry> list = output.list("Items", SlotEntry.CODEC);
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                list.add(new SlotEntry(i, stack));
            }
        }
    }

    public static void loadAllItems(ValueInput input, NonNullList<ItemStack> items) {
        for (SlotEntry entry : input.listOrEmpty("Items", SlotEntry.CODEC)) {
            long slot = decodeSlot(entry.slot());
            if (slot >= 0 && slot < items.size()) {
                items.set((int) slot, entry.stack());
            }
        }
    }

    /**
     * 旧存档（原版 ContainerHelper 写入）的槽位是 TAG_Byte，其中 128~255 存为负数，按无符号还原；
     * 本类写入的是非负 TAG_Long，原样使用。
     */
    private static long decodeSlot(long raw) {
        return raw < 0 ? raw & 0xFFL : raw;
    }

    private record SlotEntry(long slot, ItemStack stack) {
        static final Codec<SlotEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("Slot").orElse(0L).forGetter(SlotEntry::slot),
                ItemStack.MAP_CODEC.forGetter(SlotEntry::stack)).apply(instance, SlotEntry::new));
    }
}
