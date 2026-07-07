package qikahome.jsonmore.minecraft.jsonscript.node;

import com.google.gson.JsonObject;

import qikahome.jsonmore.minecraft.jsonscript.ScriptNode;
import qikahome.jsonmore.minecraft.jsonscript.ScriptState;
import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;

/**
 * 设置变量节点。将一个值（字面量、变量引用、子节点）赋给指定变量。
 * <p>
 * JSON 示例：
 * <pre>
 * {"type":"set_var","var":"count","value":3}
 * {"type":"set_var","var":"name","value":"hello"}
 * {"type":"set_var","var":"flag","value":true}
 * {"type":"set_var","var":"other","value":"$existing_var"}
 * {"type":"set_var","var":"result","value":{"type":"..."}}
 * </pre>
 */
public class SetVarNode implements ScriptNode {

    private final String varName;
    private final ScriptNode valueNode;

    public SetVarNode(String varName, ScriptNode valueNode) {
        this.varName = varName;
        this.valueNode = valueNode;
    }

    @Override
    public ScriptVariable<?> process(ScriptState state) {
        ScriptVariable<?> value = valueNode.process(state);
        state.setVar(varName, value);
        return value;
    }

    static {
        ScriptNode.register("set_var", json -> {
            String varName = json.get("var").getAsString();
            ScriptNode valueNode = ScriptNode.parse(json.get("value"));
            return new SetVarNode(varName, valueNode);
        });
    }
}
