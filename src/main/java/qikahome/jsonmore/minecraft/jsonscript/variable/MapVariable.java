package qikahome.jsonmore.minecraft.jsonscript.variable;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.stream.Collectors;

import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;

/**
 * 键值对变量。值可以是任意 {@link ScriptVariable} 类型。
 * <p>
 * 每个键自动注册为属性，可通过 {@code $(map.key)} 链式访问。
 * 支持序列化为不同格式（作为属性访问）：
 * <ul>
 *   <li>{@code $(map.blockstate)} → {@code [prop1=value1,prop2=value2]}</li>
 *   <li>{@code $(map.nbt)} → {@code {key1:value1,key2:value2}}</li>
 *   <li>{@code $(map.json)} → {@code {"key1":"value1","key2":"value2"}}</li>
 * </ul>
 */
public class MapVariable extends ScriptVariable<Map<String, ScriptVariable<?>>> {

    public MapVariable(@Nullable Map<String, ScriptVariable<?>> value) {
        super(value);
        if (value != null) {
            for (String key : value.keySet()) {
                registerProperty(key, () -> value.get(key));
            }
        }
        registerProperty("blockstate", this::formatBlockState);
        registerProperty("nbt", this::formatNBT);
        registerProperty("json", this::formatJson);
    }

    /**
     * 默认序列化：{@code [key=value,key=value]}
     */
    @Override
    public String toScriptString() {
        if (value == null || value.isEmpty()) return "";
        return value.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().toScriptString())
                .collect(Collectors.joining(",", "[", "]"));
    }

    private ScriptVariable<?> formatBlockState() {
        return new StringVariable(toScriptString());
    }

    private ScriptVariable<?> formatNBT() {
        if (value == null || value.isEmpty()) return new StringVariable("{}");
        var body = value.entrySet().stream()
                .map(e -> e.getKey() + ":" + formatNbtValue(e.getValue()))
                .collect(Collectors.joining(","));
        return new StringVariable("{" + body + "}");
    }

    private static String formatNbtValue(ScriptVariable<?> var) {
        if (var instanceof NumberVariable nv) {
            double d = nv.doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
            return nv.toScriptString();
        }
        if (var instanceof BooleanVariable) {
            return var.value() != null && (boolean) var.value() ? "1b" : "0b";
        }
        return "\"" + var.toScriptString() + "\"";
    }

    private ScriptVariable<?> formatJson() {
        if (value == null || value.isEmpty()) return new StringVariable("{}");
        var body = value.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":" + formatJsonValue(e.getValue()))
                .collect(Collectors.joining(","));
        return new StringVariable("{" + body + "}");
    }

    private static String formatJsonValue(ScriptVariable<?> var) {
        if (var instanceof NumberVariable nv) {
            double d = nv.doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
            return String.valueOf(d);
        }
        if (var instanceof BooleanVariable bv) {
            return bv.value() != null && (boolean) bv.value() ? "true" : "false";
        }
        var raw = var.toScriptString();
        return "\"" + raw.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Override
    public String type() {
        return "Map";
    }
}
