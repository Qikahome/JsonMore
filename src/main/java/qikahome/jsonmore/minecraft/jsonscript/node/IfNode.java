package qikahome.jsonmore.minecraft.jsonscript.node;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import qikahome.jsonmore.minecraft.jsonscript.ScriptNode;
import qikahome.jsonmore.minecraft.jsonscript.ScriptState;
import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.BooleanVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.NullVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.NumberVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.StringVariable;

/**
 * 条件分支节点。根据条件结果执行不同分支。
 * <p>
 * JSON 示例：
 * <pre>
 * {"type":"if","condition":{"type":"...","...":"..."},"if_true":[...],"if_false":[...]}
 * {"type":"if","condition":true,"if_true":[{"type":"return","value":"SUCCESS"}]}
 * </pre>
 */
public class IfNode implements ScriptNode {

    private final ScriptNode condition;
    private final ScriptNode ifTrue;
    @Nullable
    private final ScriptNode ifFalse;

    public IfNode(ScriptNode condition, ScriptNode ifTrue, @Nullable ScriptNode ifFalse) {
        this.condition = condition;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
    }

    @Override
    public ScriptVariable<?> process(ScriptState state) {
        ScriptVariable<?> condResult = condition.process(state);
        if (isTruthy(condResult)) {
            return ifTrue.process(state);
        } else if (ifFalse != null) {
            return ifFalse.process(state);
        }
        return NullVariable.INSTANCE;
    }

    private static boolean isTruthy(ScriptVariable<?> var) {
        if (var instanceof NullVariable) return false;
        if (var instanceof BooleanVariable b) return b.booleanValue();
        if (var instanceof NumberVariable n) return n.doubleValue() != 0;
        if (var instanceof StringVariable s) return !s.isEmpty();
        return var.value() != null;
    }

    static {
        ScriptNode.register("if", json -> {
            ScriptNode condition = ScriptNode.parse(json.get("condition"));
            ScriptNode ifTrue = ScriptNode.parse(json.get("if_true"));
            ScriptNode ifFalse = json.has("if_false") ? ScriptNode.parse(json.get("if_false")) : null;
            return new IfNode(condition, ifTrue, ifFalse);
        });
    }
}
