package qikahome.jsonmore.minecraft.jsonscript.node;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import qikahome.jsonmore.minecraft.jsonscript.ScriptNode;
import qikahome.jsonmore.minecraft.jsonscript.ScriptState;
import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.*;

import javax.annotation.Nullable;
import java.util.Map;

import static qikahome.jsonmore.JsonMore.LOGGER;

/**
 * 从组件创建 ItemStack。
 * <p>
 * JSON 格式：
 * <pre>{@code
 * {
 *   "type": "make_itemstack",
 *   "item": "minecraft:diamond",
 *   "count": 1,
 *   "nbt": { "Damage": 0 }
 * }
 * }</pre>
 */
public class MakeItemStackNode implements ScriptNode {
    private final ScriptNode itemNode;
    private final ScriptNode countNode;
    private final ScriptNode nbtNode;

    static {
        ScriptNode.register("make_itemstack", MakeItemStackNode::new);
    }

    private MakeItemStackNode(JsonObject json) {
        this.itemNode = ScriptNode.parse(json.get("item"));
        this.countNode = json.has("count") ? ScriptNode.parse(json.get("count")) : null;
        JsonElement nbtEl = json.get("nbt");
        this.nbtNode = nbtEl != null ? ScriptNode.parse(nbtEl) : null;
    }

    @Override
    public ScriptVariable<?> process(ScriptState state) {
        var itemVar = itemNode.process(state);
        if (!(itemVar instanceof StringVariable sv) || sv.value() == null) {
            LOGGER.error("make_itemstack: item 必须是字符串");
            state.fail();
            return NullVariable.INSTANCE;
        }

        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(sv.value()));
        if (item == null) {
            LOGGER.error("make_itemstack: 未知物品 '{}'", sv.value());
            state.fail();
            return NullVariable.INSTANCE;
        }

        int count = 1;
        if (countNode != null) {
            var cv = countNode.process(state);
            if (cv instanceof NumberVariable nv) {
                count = Math.max(1, nv.intValue());
            }
        }

        ItemStack stack = new ItemStack(item, count);

        if (nbtNode != null) {
            var nbtVar = nbtNode.process(state);
            if (nbtVar instanceof MapVariable mv && mv.value() != null) {
                CompoundTag tag = toNBT(mv);
                if (tag != null) {
                    stack.setTag(tag);
                }
            }
        }

        return new ItemStackVariable(stack);
    }

    @Nullable
    private static CompoundTag toNBT(MapVariable mv) {
        if (mv.value() == null || mv.value().isEmpty()) return null;
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<String, ScriptVariable<?>> entry : mv.value().entrySet()) {
            putTag(tag, entry.getKey(), entry.getValue());
        }
        return tag;
    }

    private static void putTag(CompoundTag tag, String key, ScriptVariable<?> var) {
        if (var instanceof NumberVariable nv) {
            double d = nv.doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                long l = (long) d;
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                    tag.putInt(key, (int) l);
                } else {
                    tag.putLong(key, l);
                }
            } else {
                tag.putDouble(key, d);
            }
        } else if (var instanceof StringVariable sv && sv.value() != null) {
            tag.putString(key, sv.value());
        } else if (var instanceof BooleanVariable bv) {
            tag.putBoolean(key, bv.value() != null && bv.value());
        } else if (var instanceof ListVariable lv && lv.value() != null) {
            ListTag list = new ListTag();
            for (ScriptVariable<?> elem : lv.value()) {
                Tag nbtElem = toTag(elem);
                if (nbtElem != null) list.add(nbtElem);
            }
            tag.put(key, list);
        } else if (var instanceof MapVariable mv && mv.value() != null) {
            CompoundTag sub = toNBT(mv);
            if (sub != null) tag.put(key, sub);
        }
    }

    @Nullable
    private static Tag toTag(ScriptVariable<?> var) {
        if (var instanceof NumberVariable nv) {
            double d = nv.doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                long l = (long) d;
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                    return net.minecraft.nbt.IntTag.valueOf((int) l);
                }
                return net.minecraft.nbt.LongTag.valueOf(l);
            }
            return net.minecraft.nbt.DoubleTag.valueOf(d);
        }
        if (var instanceof StringVariable sv && sv.value() != null) {
            return net.minecraft.nbt.StringTag.valueOf(sv.value());
        }
        if (var instanceof BooleanVariable bv) {
            return net.minecraft.nbt.ByteTag.valueOf(bv.value() != null && bv.value());
        }
        return null;
    }
}
