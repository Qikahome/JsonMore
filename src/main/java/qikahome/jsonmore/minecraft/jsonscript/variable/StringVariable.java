package qikahome.jsonmore.minecraft.jsonscript.variable;

import javax.annotation.Nullable;

import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;

/**
 * 字符串变量。
 */
public class StringVariable extends ScriptVariable<String> {

    public StringVariable(@Nullable String value) {
        super(value);
        registerProperty("number", () -> {
            if (value == null || value.isEmpty()) return NullVariable.INSTANCE;
            try {
                return new NumberVariable(Double.parseDouble(value));
            } catch (NumberFormatException e) {
                return NullVariable.INSTANCE;
            }
        });
    }

    public int length() {
        return value != null ? value.length() : 0;
    }

    public boolean isEmpty() {
        return value == null || value.isEmpty();
    }

    @Override
    public String toScriptString() {
        return value != null ? value : "";
    }

    @Override
    public String type() {
        return "String";
    }
}
