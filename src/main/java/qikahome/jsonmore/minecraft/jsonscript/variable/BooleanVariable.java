package qikahome.jsonmore.minecraft.jsonscript.variable;

import javax.annotation.Nullable;

import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;

/**
 * 布尔变量。
 */
public class BooleanVariable extends ScriptVariable<Boolean> {

    public BooleanVariable(@Nullable Boolean value) {
        super(value);
        registerProperty("negate", this::negate);
    }

    public boolean booleanValue() {
        return value != null && value;
    }

    public BooleanVariable negate() {
        return new BooleanVariable(value == null || !value);
    }

    @Override
    public String toScriptString() {
        return value != null ? value.toString() : "false";
    }

    @Override
    public String type() {
        return "Boolean";
    }
}
