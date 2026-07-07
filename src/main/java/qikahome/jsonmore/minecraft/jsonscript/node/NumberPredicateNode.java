package qikahome.jsonmore.minecraft.jsonscript.node;

import com.google.gson.JsonObject;
import qikahome.jsonmore.Utils;
import qikahome.jsonmore.minecraft.jsonscript.ScriptNode;
import qikahome.jsonmore.minecraft.jsonscript.ScriptState;
import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.BooleanVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.NumberVariable;

/**
 * 数值谓词节点。判断数值是否匹配指定的 {@link Utils.IntRange}。
 * <p>
 * JSON 示例：
 * <pre>
 * {"type":"number_predicate","value":"$(servings)","predicate":"(,0]"}
 * {"type":"number_predicate","value":5,"predicate":"[1,4]"}
 * {"type":"number_predicate","value":"$(count)","predicate":"*"}
 * </pre>
 */
public class NumberPredicateNode implements ScriptNode {

    private final ScriptNode valueNode;
    private final String predicateStr;

    public NumberPredicateNode(ScriptNode valueNode, String predicateStr) {
        this.valueNode = valueNode;
        this.predicateStr = predicateStr;
    }

    @Override
    public ScriptVariable<?> process(ScriptState state) {
        ScriptVariable<?> val = valueNode.process(state);
        if (!(val instanceof NumberVariable nv)) {
            return new BooleanVariable(false);
        }

        String resolved = ScriptNode.resolveParenthesized(predicateStr, state);
        if (resolved == null) {
            return new BooleanVariable(false);
        }
        Utils.IntRange range = Utils.IntRange.parse(resolved);
        return new BooleanVariable(range.contains(nv.intValue()));
    }

    static {
        ScriptNode.register("number_predicate", json -> {
            ScriptNode valueNode = ScriptNode.parse(json.get("value"));
            String predicate = json.get("predicate").getAsString();
            return new NumberPredicateNode(valueNode, predicate);
        });
    }
}
