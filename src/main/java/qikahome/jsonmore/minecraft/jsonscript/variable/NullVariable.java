package qikahome.jsonmore.minecraft.jsonscript.variable;

import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;

import javax.annotation.Nullable;

/**
 * 空变量单例。表示"变量不存在"。
 * <p>
 * {@code exists → false}，{@code value → null}，
 * {@code toScriptString()} 抛出 {@link UnsupportedOperationException}。
 */
public class NullVariable extends ScriptVariable<Object> {

    public static final NullVariable INSTANCE = new NullVariable();

    private NullVariable() {
        super(null);
    }

    @Override
    public String type() {
        return "Null";
    }

    @Override
    public String toScriptString() {
        throw new UnsupportedOperationException("NullVariable 不能用于命令字符串");
    }
}
