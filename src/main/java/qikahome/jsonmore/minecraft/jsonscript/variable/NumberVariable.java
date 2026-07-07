package qikahome.jsonmore.minecraft.jsonscript.variable;

import javax.annotation.Nullable;

import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;

/**
 * 数字变量，支持 int、double、float 等数值类型。
 * <p>
 * 注册的属性：negate、sqrt、abs
 */
public class NumberVariable extends ScriptVariable<Number> {

    public NumberVariable(@Nullable Number value) {
        super(value);
        registerProperty("negate", () -> new NumberVariable(value != null ? -value.doubleValue() : 0));
        registerProperty("sqrt", () -> {
            double v = value != null ? value.doubleValue() : 0;
            return v >= 0 ? new NumberVariable(Math.sqrt(v)) : null;
        });
        registerProperty("abs", () -> new NumberVariable(value != null ? Math.abs(value.doubleValue()) : 0));
        registerProperty("round", () -> {
            double v = value != null ? value.doubleValue() : 0;
            return new NumberVariable((int) Math.round(v));
        });
        registerProperty("ceil", () -> {
            double v = value != null ? value.doubleValue() : 0;
            return new NumberVariable((int) Math.ceil(v));
        });
        registerProperty("floor", () -> {
            double v = value != null ? value.doubleValue() : 0;
            return new NumberVariable((int) Math.floor(v));
        });
        registerProperty("byte", () -> new StringVariable((value != null ? (byte) value.longValue() : 0) + "b"));
        registerProperty("short", () -> new StringVariable((value != null ? (short) value.longValue() : 0) + "s"));
        registerProperty("int", () -> new StringVariable(String.valueOf(value != null ? value.intValue() : 0)));
        registerProperty("long", () -> new StringVariable((value != null ? value.longValue() : 0) + "L"));
        registerProperty("float", () -> new StringVariable((value != null ? value.floatValue() : 0f) + "f"));
        registerProperty("double", () -> new StringVariable(String.valueOf(value != null ? value.doubleValue() : 0d) + "d"));
    }

    public int intValue() {
        return value != null ? value.intValue() : 0;
    }

    public double doubleValue() {
        return value != null ? value.doubleValue() : 0;
    }

    @Override
    public String toScriptString() {
        if (value == null) return "0";
        double d = value.doubleValue();
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf(value.longValue());
        }
        return String.valueOf(d);
    }

    @Override
    public String type() {
        return "Number";
    }
}
