package qikahome.jsonmore.minecraft.jsonscript.variable;

import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;

/**
 * 方块状态变量，提供 block、properties 等属性供 JSON 访问。
 */
public class BlockStateVariable extends ScriptVariable<BlockState> {

    public BlockStateVariable(@Nullable BlockState value) {
        super(value);
        registerProperty("block", () -> {
            if (value == null) return new StringVariable("minecraft:air");
            return new StringVariable(BuiltInRegistries.BLOCK.getKey(value.getBlock()).toString());
        });
        registerProperty("is_air", () -> new BooleanVariable(value == null || value.isAir()));
        registerProperty("properties", () -> {
            if (value == null) return new MapVariable(Map.of());
            return new MapVariable(value.getValues().entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().getName(),
                            e -> new StringVariable(e.getValue().toString()))));
        });
    }

    @Nullable
    public ResourceLocation blockId() {
        if (value == null) return null;
        return BuiltInRegistries.BLOCK.getKey(value.getBlock());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends Comparable<T>> T getPropertyValue(Property<T> prop) {
        return value != null ? value.getValue(prop) : null;
    }

    @Override
    public String toScriptString() {
        if (value == null) return "minecraft:air";
        var id = BuiltInRegistries.BLOCK.getKey(value.getBlock());
        var props = getProperty("properties");
        if (props == null) return id.toString();
        var propStr = props.toScriptString();
        return propStr.isEmpty() ? id.toString() : id + propStr;
    }

    @Override
    public String type() {
        return "BlockState";
    }
}
