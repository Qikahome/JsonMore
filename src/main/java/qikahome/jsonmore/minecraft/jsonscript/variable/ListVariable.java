package qikahome.jsonmore.minecraft.jsonscript.variable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;

/**
 * 列表变量。覆盖 NBT List / 数组 类型。
 * <p>
 * 支持索引属性访问：{@code $(list.0)}、{@code $(list.1)}
 * <br>便捷属性：{@code $(list.size)}、{@code $(list.first)}、{@code $(list.last)}
 * <br>序列化格式：{@code $(list.json)}、{@code $(list.nbt)}
 */
public class ListVariable extends ScriptVariable<List<ScriptVariable<?>>> {

    public ListVariable(@Nullable List<ScriptVariable<?>> value) {
        super(value);
        registerProperty("size", () -> new NumberVariable(value != null ? value.size() : 0));
        registerProperty("first", this::getFirst);
        registerProperty("last", this::getLast);
        registerProperty("nbt", this::formatNBT);
        registerProperty("json", this::formatJson);
        registerProperty("byte_array", () -> new StringVariable(formatArray("B")));
        registerProperty("int_array", () -> new StringVariable(formatArray("I")));
        registerProperty("long_array", () -> new StringVariable(formatArray("L")));
    }

    /**
     * 支持数字索引属性访问，如 {@code $(list.0)}、{@code $(list.1)}。
     */
    @Override
    public boolean hasProperty(String name) {
        if (super.hasProperty(name)) return true;
        return isIndex(name) && value != null && Integer.parseInt(name) < value.size();
    }

    @Nullable
    @Override
    public ScriptVariable<?> getProperty(String name) {
        if (super.hasProperty(name)) return super.getProperty(name);
        if (value != null && isIndex(name)) {
            int idx = Integer.parseInt(name);
            if (idx >= 0 && idx < value.size()) {
                return value.get(idx);
            }
        }
        return null;
    }

    private static boolean isIndex(String name) {
        if (name.isEmpty()) return false;
        for (int i = 0; i < name.length(); i++) {
            if (!Character.isDigit(name.charAt(i))) return false;
        }
        return true;
    }

    private ScriptVariable<?> getFirst() {
        if (value == null || value.isEmpty()) return NullVariable.INSTANCE;
        return value.get(0);
    }

    private ScriptVariable<?> getLast() {
        if (value == null || value.isEmpty()) return NullVariable.INSTANCE;
        return value.get(value.size() - 1);
    }

    @Override
    public String toScriptString() {
        if (value == null) return "[]";
        return value.stream()
                .map(ScriptVariable::toScriptString)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String formatArray(String prefix) {
        if (value == null || value.isEmpty()) return "[" + prefix + ";]";
        var body = value.stream()
                .map(v -> v instanceof NumberVariable nv ? formatNbtElement(nv) : v.toScriptString())
                .collect(Collectors.joining(","));
        return "[" + prefix + ";" + body + "]";
    }

    private ScriptVariable<?> formatNBT() {
        if (value == null) return new StringVariable("[]");
        var body = value.stream()
                .map(ListVariable::formatNbtElement)
                .collect(Collectors.joining(","));
        return new StringVariable("[" + body + "]");
    }

    private static String formatNbtElement(ScriptVariable<?> var) {
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
        if (value == null) return new StringVariable("[]");
        var body = value.stream()
                .map(ListVariable::formatJsonElement)
                .collect(Collectors.joining(","));
        return new StringVariable("[" + body + "]");
    }

    private static String formatJsonElement(ScriptVariable<?> var) {
        if (var instanceof NumberVariable nv) {
            double d = nv.doubleValue();
            if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf((long) d);
            return String.valueOf(d);
        }
        if (var instanceof BooleanVariable bv) {
            return bv.value() != null && (boolean) bv.value() ? "true" : "false";
        }
        return "\"" + var.toScriptString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Override
    public String type() {
        return "List";
    }
}
