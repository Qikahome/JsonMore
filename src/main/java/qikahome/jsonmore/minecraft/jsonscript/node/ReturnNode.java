package qikahome.jsonmore.minecraft.jsonscript.node;

import dev.gigaherz.jsonthings.things.events.FlexEventResult;
import qikahome.jsonmore.minecraft.jsonscript.ScriptNode;
import qikahome.jsonmore.minecraft.jsonscript.ScriptState;
import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.NullVariable;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

/**
 * 返回节点。终止执行并将返回值填入 ScriptState。
 * <p>
 * JSON 示例：
 * <pre>
 * {"type":"return","value":"SUCCESS"}
 * {"type":"return","value":"$held_item"}
 * {"type":"return"}
 * </pre>
 */
public class ReturnNode implements ScriptNode {

    @Nullable
    private final ScriptNode valueNode;

    public ReturnNode(@Nullable ScriptNode valueNode) {
        this.valueNode = valueNode;
    }

    @Override
    public ScriptVariable<?> process(ScriptState state) {
        if (valueNode != null) {
            ScriptVariable<?> result = valueNode.process(state);
            Object raw = (result instanceof NullVariable) ? null : result.value();
            state.setReturnValue(raw != null ? toFlexResult(raw) : FlexEventResult.success());
        } else {
            state.setReturnValue(FlexEventResult.success());
        }
        state.returnNow();
        return NullVariable.INSTANCE;
    }

    /** 将 ScriptNode 返回的原始值映射为 {@link FlexEventResult}。 */
    private static FlexEventResult toFlexResult(Object raw) {
        if (raw instanceof String s) {
            return switch (s) {
                case "SUCCESS", "success" -> FlexEventResult.success();
                case "CONSUME", "consume" -> FlexEventResult.consume();
                case "CONSUME_PARTIAL", "consume_partial" -> FlexEventResult.consumePartial();
                case "PASS", "pass" -> FlexEventResult.pass();
                case "FAIL", "fail" -> FlexEventResult.fail();
                default -> FlexEventResult.success(raw);
            };
        }
        return FlexEventResult.success(raw);
    }

    static {
        ScriptNode.register("return", json -> {
            ScriptNode valueNode = json.has("value") ? ScriptNode.parse(json.get("value")) : null;
            return new ReturnNode(valueNode);
        });
    }
}
