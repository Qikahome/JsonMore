package qikahome.jsonmore.minecraft.jsonscript;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import qikahome.jsonmore.minecraft.jsonscript.node.CalcNode;
import qikahome.jsonmore.minecraft.jsonscript.variable.BooleanVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.NullVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.NumberVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.StringVariable;

import static qikahome.jsonmore.JsonMore.LOGGER;

/**
 * 脚本节点，是脚本执行的基本单位。
 * <p>
 * 每个节点通过 {@link #process(ScriptState)} 执行逻辑，
 * 执行结果（继续、返回、失败）通过修改 {@link ScriptState} 来表达，
 * 并可选返回一个变量值供父节点使用。
 */
public interface ScriptNode {

    /**
     * 解析字符串中的 {@code $(var.prop)} 宏占位符，替换为对应变量的 {@link ScriptVariable#toScriptString()}。
     * <p>
     * 用于混合字符串，如 {@code "(,$(count)]"} → {@code "(,5]"}。
     * 纯 {@code $(var)} 完整占位应由 {@link #parseVarRef} 处理。
     * <p>
     * 不存在的变量/属性替换为空字符串，不会导致失败。
     */
    @Nullable
    public static String resolveParenthesized(String template, ScriptState state) {
        var sb = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            int dollar = template.indexOf("$(", i);
            if (dollar < 0) {
                sb.append(template.substring(i));
                break;
            }
            sb.append(template.substring(i, dollar));
            int close = findMatchingClose(template, dollar + 2);
            if (close < 0) {
                LOGGER.error("宏缺少闭合 ): {}", template.substring(dollar));
                return null;
            }
            String ref = template.substring(dollar + 2, close);
            String[] parts = ref.split("\\.");
            String varName = parts[0];

            ScriptVariable<?> cur = state.getVar(varName);
            if (cur == null) cur = NullVariable.INSTANCE;

            for (int p = 1; p < parts.length; p++) {
                String prop = parts[p];
                if (!cur.hasProperty(prop)) {
                    LOGGER.warn("变量 {} 上没有属性 '{}'，替换为空字符串", varName, prop);
                    cur = NullVariable.INSTANCE;
                    break;
                }
                cur = cur.getProperty(prop);
                if (cur == null) {
                    cur = NullVariable.INSTANCE;
                    break;
                }
            }

            try {
                sb.append(cur.toScriptString());
            } catch (UnsupportedOperationException e) {
                LOGGER.error("变量 '{}' 不能序列化为字符串", ref);
                return null;
            }
            i = close + 1;
        }
        return sb.toString();
    }

    /** 从 start 位置开始找匹配的 {@code )}，跳过嵌套的 {@code $(...)}。 */
    private static int findMatchingClose(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            if (s.startsWith("$(", i)) { depth++; i++; }
            else if (s.charAt(i) == ')') {
                if (depth == 0) return i;
                depth--;
            }
        }
        return -1;
    }

    /**
     * 解析变量引用字符串。
     * <p>
     * <ul>
     *   <li>{@code $flag} → 取变量 flag</li>
     *   <li>{@code $(flag.negate)} → 取变量 flag，再访问属性 negate</li>
     *   <li>{@code $(user.mainhand.item)} → 取变量 user，属性 mainhand，属性 item</li>
     * </ul>
     */
    private static ScriptNode parseVarRef(String s) {
        if (s.startsWith("$(")) {
            // 带属性链的变量引用：$(var.prop1.prop2) 或 $(var).prop1.prop2
            int closeParen = s.indexOf(')');
            if (closeParen < 2) {
                LOGGER.error("无效的变量引用语法: {}", s);
                return state -> NullVariable.INSTANCE;
            }
            String expr = s.substring(2, closeParen);
            String rest = s.substring(closeParen + 1); // 可能为 ".properties.servings" 或空字符串
            String[] parts = (expr + rest).split("\\.");
            String varName = parts[0];
            String[] props = new String[parts.length - 1];
            System.arraycopy(parts, 1, props, 0, props.length);
            return state -> {
                ScriptVariable<?> var = state.getVar(varName);
                if (var == null) return NullVariable.INSTANCE;
                for (String prop : props) {
                    if (!var.hasProperty(prop)) return NullVariable.INSTANCE;
                    var = var.getProperty(prop);
                    if (var == null) return NullVariable.INSTANCE;
                }
                return var;
            };
        }
        // 简单变量引用：$var 或 $var.prop
        String[] parts = s.substring(1).split("\\.");
        String varName = parts[0];
        String[] props = new String[parts.length - 1];
        System.arraycopy(parts, 1, props, 0, props.length);
        return state -> {
            ScriptVariable<?> var = state.getVar(varName);
            if (var == null) return NullVariable.INSTANCE;
            for (String prop : props) {
                if (!var.hasProperty(prop)) return NullVariable.INSTANCE;
                var = var.getProperty(prop);
                if (var == null) return NullVariable.INSTANCE;
            }
            return var;
        };
    }

    /** 脚本节点类型注册表。键为 JSON 中的 {@code type} 字段值。 */
    Map<String, Function<JsonObject, ScriptNode>> REGISTRY = new HashMap<>();

    /**
     * 注册一个脚本节点类型。
     *
     * @param type         JSON 中的 type 字段值
     * @param deserializer 从 JSON 反序列化为 ScriptNode 的函数
     */
    static void register(String type, Function<JsonObject, ScriptNode> deserializer) {
        REGISTRY.put(type, deserializer);
    }

    /**
     * 从 {@link JsonElement} 解析为 {@link ScriptNode}。
     * <p>
     * 解析规则：
     * <ul>
     *   <li>boolean → 返回 {@link BooleanVariable} 的字面量节点</li>
     *   <li>number → 返回 {@link NumberVariable} 的字面量节点</li>
     *   <li>以 {@code $} 开头的字符串 → 变量引用节点</li>
     *   <li>其它字符串 → 返回 {@link StringVariable} 的字面量节点</li>
     *   <li>对象 → 通过 {@link #REGISTRY} 查找对应的节点类型</li>
     * </ul>
     */
    static ScriptNode parse(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return state -> null;
        }

        if (json.isJsonPrimitive()) {
            JsonPrimitive prim = json.getAsJsonPrimitive();
            if (prim.isBoolean()) {
                boolean val = prim.getAsBoolean();
                return state -> new BooleanVariable(val);
            }
            if (prim.isNumber()) {
                double val = prim.getAsDouble();
                return state -> new NumberVariable(val);
            }
            if (prim.isString()) {
                String s = prim.getAsString();
                if (s.startsWith("$")) {
                    return parseVarRef(s);
                }
                if (s.contains("$(")) {
                    return state -> {
                        String resolved = resolveParenthesized(s, state);
                        return resolved != null ? new StringVariable(resolved) : NullVariable.INSTANCE;
                    };
                }
                return state -> new StringVariable(s);
            }
        }

        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            String type = obj.get("type").getAsString();
            Function<JsonObject, ScriptNode> factory = REGISTRY.get(type);
            if (factory != null) {
                return factory.apply(obj);
            }
            throw new IllegalArgumentException("Unknown script node type: " + type);
        }

        // JsonArray → 按顺序执行（SequenceNode）
        if (json.isJsonArray()) {
            var list = new java.util.ArrayList<ScriptNode>();
            for (var element : json.getAsJsonArray()) {
                list.add(parse(element));
            }
            return new SequentialNode(list);
        }

        throw new IllegalArgumentException("Cannot parse script node from: " + json);
    }

    /**
     * 执行此节点。
     *
     * @param state 脚本运行时状态，节点可读写其中的变量和控制流
     * @return 节点产生的结果值，不存在时返回 {@link NullVariable#INSTANCE}
     */
    ScriptVariable<?> process(ScriptState state);

    /**
     * 显式触发所有内置节点类的加载，确保它们的 {@code static {}} 注册块被执行。
     * <p>
     * 在 {@link JsonScriptParser} 构造器中调用，避免解析时出现 "Unknown script node type" 错误。
     */
    static void loadBuiltinNodes() {
        CalcNode.load();
        try {
            Class.forName("qikahome.jsonmore.minecraft.jsonscript.node.IfNode");
            Class.forName("qikahome.jsonmore.minecraft.jsonscript.node.SetVarNode");
            Class.forName("qikahome.jsonmore.minecraft.jsonscript.node.ReturnNode");
            Class.forName("qikahome.jsonmore.minecraft.jsonscript.node.RunCommandNode");
            Class.forName("qikahome.jsonmore.minecraft.jsonscript.node.ItemPredicateNode");
            Class.forName("qikahome.jsonmore.minecraft.jsonscript.node.NumberPredicateNode");
            Class.forName("qikahome.jsonmore.minecraft.jsonscript.node.MakeItemStackNode");
            Class.forName("qikahome.jsonmore.minecraft.jsonscript.node.MakeBlockStateNode");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load built-in script node classes", e);
        }
    }
}
