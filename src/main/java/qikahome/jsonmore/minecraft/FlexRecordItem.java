package qikahome.jsonmore.minecraft;

import dev.gigaherz.jsonthings.things.builders.ItemBuilder;
import dev.gigaherz.jsonthings.things.items.FlexItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.JukeboxSong;

/**
 * 唱片物品（{@code jsonmore:record}）。
 *
 * <p>1.21 起唱片改由数据驱动：唱片物品本身不再保存 sound/length/comparator 三件套，
 * 而是通过 {@code jukebox_playable} 组件引用一份
 * {@code data/<命名空间>/jukebox_song/<id>.json}（{@link JukeboxSong} 注册表条目），
 * 播放时长、比较器输出、音效与描述都写在那个条目里。1.21 的 {@code RecordItem} 已移除，
 * 唱片就是"带 jukebox_playable 组件的普通物品"。
 *
 * <p>这里用 {@code jukebox_song} 的 {@link ResourceKey} 驱动组件，而不是在构造期解析
 * {@code Holder}：thingpack 的物品在数据包注册表（{@code JukeboxSong}）加载之前就已注册，
 * 用 key 形式可以把解析推迟到实际使用时，不会被加载顺序卡住。
 *
 * <p>flex 能力（事件钩子、属性修饰符、lore、use_anim/use_time/use_finish_mode、
 * toolActions）直接沿用 JsonThings 的 {@link FlexItem}——它在 1.21.1 就是"带 flex 能力的普通物品"，
 * 与唱片所需的基类完全一致，无需像 Forge 1.20.1 那样重新实现一遍。
 */
public class FlexRecordItem extends FlexItem {

    public FlexRecordItem(Properties properties, ItemBuilder builder, ResourceKey<JukeboxSong> jukeboxSong) {
        // 唱片堆叠上限固定为 1（与原版唱片一致），并挂上 key 驱动的 jukebox_playable 组件。
        super(properties.stacksTo(1).jukeboxPlayable(jukeboxSong), builder);
    }
}
