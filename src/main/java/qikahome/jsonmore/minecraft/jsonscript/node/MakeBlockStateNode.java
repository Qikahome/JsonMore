package qikahome.jsonmore.minecraft.jsonscript.node;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import qikahome.jsonmore.minecraft.jsonscript.ScriptNode;
import qikahome.jsonmore.minecraft.jsonscript.ScriptState;
import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.*;

import static qikahome.jsonmore.JsonMore.LOGGER;

/**
 * 从组件创建 BlockState。
 * <p>
 * JSON 格式：
 * <pre>{@code
 * {
 *   "type": "make_blockstate",
 *   "block": "minecraft:stone",
 *   "properties": { "facing": "north" }
 * }
 * }</pre>
 */
public class MakeBlockStateNode implements ScriptNode {
    private final ScriptNode blockNode;
    private final ScriptNode propsNode;

    static {
        ScriptNode.register("make_blockstate", MakeBlockStateNode::new);
    }

    private MakeBlockStateNode(JsonObject json) {
        this.blockNode = ScriptNode.parse(json.get("block"));
        this.propsNode = json.has("properties") ? ScriptNode.parse(json.get("properties")) : null;
    }

    @Override
    public ScriptVariable<?> process(ScriptState state) {
        var blockVar = blockNode.process(state);
        if (!(blockVar instanceof StringVariable sv) || sv.value() == null) {
            LOGGER.error("make_blockstate: block 必须是字符串");
            state.fail();
            return NullVariable.INSTANCE;
        }

        var block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(sv.value()));
        if (block == null) {
            LOGGER.error("make_blockstate: 未知方块 '{}'", sv.value());
            state.fail();
            return NullVariable.INSTANCE;
        }

        BlockState bs = block.defaultBlockState();

        if (propsNode != null) {
            var propsVar = propsNode.process(state);
            if (propsVar instanceof MapVariable mv && mv.value() != null) {
                bs = applyProperties(bs, mv);
            }
        }

        return new BlockStateVariable(bs);
    }

    private static BlockState applyProperties(BlockState state, MapVariable mv) {
        for (var entry : mv.value().entrySet()) {
            String key = entry.getKey();
            ScriptVariable<?> valVar = entry.getValue();
            String valStr = valVar.toScriptString();

            Property<?> prop = state.getBlock().getStateDefinition().getProperty(key);
            if (prop == null) {
                LOGGER.warn("make_blockstate: 方块 {} 没有属性 '{}'",
                        ForgeRegistries.BLOCKS.getKey(state.getBlock()), key);
                continue;
            }
            state = setProperty(state, prop, valStr);
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, Property<T> prop, String value) {
        return prop.getValue(value)
                .map(v -> state.setValue(prop, v))
                .orElseGet(() -> {
                    LOGGER.warn("make_blockstate: 属性 '{}' 的值 '{}' 无效", prop.getName(), value);
                    return state;
                });
    }
}
