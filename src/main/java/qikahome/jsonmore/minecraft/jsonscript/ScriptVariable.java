package qikahome.jsonmore.minecraft.jsonscript;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import qikahome.jsonmore.minecraft.jsonscript.variable.BooleanVariable;

/**
 * 带类型的脚本变量，知道如何把自己序列化成脚本可用的字符串，
 * 并支持通过属性名链式访问子属性供 JSON 调用。
 * <p>
 * 子类在构造时调用 {@link #registerProperty(String, Supplier)} 注册可暴露的属性。
 *
 * @param <T> 变量值的 Java 类型
 */
public abstract class ScriptVariable<T> {
    @Nullable
    protected T value;

    private final Map<String, Supplier<ScriptVariable<?>>> properties = new HashMap<>();

    public ScriptVariable(@Nullable T value) {
        this.value = value;
        registerProperty("exists", () -> new BooleanVariable(value != null));
    }

    public abstract String type();

    @Nullable
    public T value() {
        return value;
    }

    public void setValue(@Nullable T value) {
        this.value = value;
    }

    /**
     * 注册一个供 JSON 访问的属性。
     * <p>
     * 示例：<pre>
     * registerProperty("count", () -> new NumberVariable(heldItem.getCount()));
     * registerProperty("item", () -> new StringVariable("minecraft:diamond"));
     * </pre>
     */
    protected void registerProperty(String name, Supplier<ScriptVariable<?>> accessor) {
        properties.put(name, accessor);
    }

    /**
     * 通过属性名获取子变量，供 JSON 链式访问（如 {@code held_item.count.item}）。
     */
    @Nullable
    public ScriptVariable<?> getProperty(String name) {
        if (value == null) return null;
        var accessor = properties.get(name);
        return accessor != null ? accessor.get() : null;
    }

    /**
     * 检查是否存在指定属性名。
     */
    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    /**
     * 返回所有可用的属性名（供调试和文档用）。
     */
    public Set<String> propertyNames() {
        return properties.keySet();
    }

    /**
     * 把变量值序列化成脚本命令（如 mcfunction 参数）或调试时可读的字符串。
     * <p>
     * 示例：
     * <ul>
     *   <li>Entity → UUID 字符串</li>
     *   <li>BlockPos → "x y z"</li>
     *   <li>ItemStack → "minecraft:diamond 3"</li>
     *   <li>String → 原字符串</li>
     * </ul>
     */
    public abstract String toScriptString();
}
