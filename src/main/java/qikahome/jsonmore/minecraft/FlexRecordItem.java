package qikahome.jsonmore.minecraft;

import dev.gigaherz.jsonthings.things.builders.ItemBuilder;
import dev.gigaherz.jsonthings.things.items.FlexItem;
import net.minecraft.world.item.Item;

/**
 * 唱片物品类型（{@code jsonmore:record}）。
 *
 * <p>1.21 起唱片已经数据驱动：原版不再有独立的 {@code RecordItem} 类，唱片就是一个普通
 * {@link Item}，播放信息由 {@code jukebox_playable} 数据组件引用 {@code JukeboxSong}
 * 数据包注册表条目提供。因此这里直接继承 JsonThings 的 {@link FlexItem}，
 * 复用其全部 flex 能力（事件钩子、属性修饰符、lore、use_anim/use_time/use_finish_mode、
 * toolActions 等）；{@code jukebox_playable} 组件由注册处通过
 * {@link Item.Properties#jukeboxPlayable(net.minecraft.resources.ResourceKey)} 延迟挂载。
 */
public class FlexRecordItem extends FlexItem {
    public FlexRecordItem(Item.Properties properties, ItemBuilder builder) {
        super(properties, builder);
    }
}
