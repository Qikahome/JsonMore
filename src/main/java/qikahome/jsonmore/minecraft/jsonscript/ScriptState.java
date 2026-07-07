package qikahome.jsonmore.minecraft.jsonscript;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import dev.gigaherz.jsonthings.things.events.FlexEventResult;

/**
 * 脚本运行时状态，在节点链执行过程中传递。
 * <p>
 * 包含：
 * <ul>
 *   <li>变量表（可读写）</li>
 *   <li>流程控制（继续 / 返回 / 失败）</li>
 *   <li>返回值（{@link FlexEventResult}）</li>
 * </ul>
 */
public class ScriptState {

    public enum FlowControl {
        CONTINUE, RETURN, FAIL
    }

    private final Map<String, ScriptVariable<?>> variables = new HashMap<>();
    private FlowControl flow = FlowControl.CONTINUE;
    @Nullable
    private FlexEventResult returnValue;

    // -- 变量操作 --

    /** 获取变量，不存在时返回 {@code null}。 */
    @Nullable
    public ScriptVariable<?> getVar(String name) {
        return variables.get(name);
    }

    public void setVar(String name, ScriptVariable<?> variable) {
        variables.put(name, variable);
    }

    public boolean hasVar(String name) {
        return variables.containsKey(name);
    }

    public Map<String, ScriptVariable<?>> allVars() {
        return variables;
    }

    // -- 流程控制 --

    public FlowControl flow() {
        return flow;
    }

    public void setFlow(FlowControl flow) {
        this.flow = flow;
    }

    /** 设为 RETURN。 */
    public void returnNow() {
        this.flow = FlowControl.RETURN;
    }

    /** 设为 FAIL。 */
    public void fail() {
        this.flow = FlowControl.FAIL;
    }

    /** 是否应该继续执行。 */
    public boolean shouldContinue() {
        return flow == FlowControl.CONTINUE;
    }

    // -- 返回值 --

    @Nullable
    public FlexEventResult returnValue() {
        return returnValue;
    }

    public void setReturnValue(@Nullable FlexEventResult value) {
        this.returnValue = value;
    }
}
