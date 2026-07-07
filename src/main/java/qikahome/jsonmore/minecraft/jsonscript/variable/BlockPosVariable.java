package qikahome.jsonmore.minecraft.jsonscript.variable;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;

/**
 * 方块坐标变量，提供 x、y、z 属性供 JSON 访问。
 */
public class BlockPosVariable extends ScriptVariable<BlockPos> {
    public BlockPosVariable(int x, int y, int z) {
        this(new BlockPos(x, y, z));
    }

    public BlockPosVariable(@Nullable BlockPos value) {
        super(value);
        registerProperty("x", () -> new NumberVariable(value != null ? value.getX() : 0));
        registerProperty("y", () -> new NumberVariable(value != null ? value.getY() : 0));
        registerProperty("z", () -> new NumberVariable(value != null ? value.getZ() : 0));
    }

    public int x() {
        return value != null ? value.getX() : 0;
    }

    public int y() {
        return value != null ? value.getY() : 0;
    }

    public int z() {
        return value != null ? value.getZ() : 0;
    }

    @Override
    public String toScriptString() {
        if (value == null)
            return "0 0 0";
        return value.getX() + " " + value.getY() + " " + value.getZ();
    }

    @Override
    public String type() {
        return "BlockPos";
    }
}
