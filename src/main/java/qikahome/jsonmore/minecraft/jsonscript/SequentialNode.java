package qikahome.jsonmore.minecraft.jsonscript;

import java.util.List;

import qikahome.jsonmore.minecraft.jsonscript.variable.NullVariable;

/**
 * 顺序执行节点。按列表顺序执行子节点，遇到 return/fail 时终止。
 * <p>
 * 通常由 {@link ScriptNode#parse} 在解析 JSON 数组时自动创建，无需显式注册。
 */
public class SequentialNode implements ScriptNode {

    private final List<ScriptNode> children;

    public SequentialNode(List<ScriptNode> children) {
        this.children = children;
    }

    @Override
    public ScriptVariable<?> process(ScriptState state) {
        ScriptVariable<?> last = null;
        for (ScriptNode child : children) {
            last = child.process(state);
            if (!state.shouldContinue()) {
                break;
            }
        }
        return last != null ? last : NullVariable.INSTANCE;
    }
}
