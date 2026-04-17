package qikahome.jsonmore.lib;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public enum ContainerPart implements StringRepresentable {
    NONE,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    FRONT,
    BACK;

    public static final EnumProperty<ContainerPart> PART = EnumProperty.create("part",
            ContainerPart.class);

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    public boolean isConnected() {
        return this != NONE;
    }

    public ContainerPart getOpposite() {
        return switch (this) {
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
            case TOP -> BOTTOM;
            case BOTTOM -> TOP;
            case FRONT -> BACK;
            case BACK -> FRONT;
            case NONE -> NONE;
        };
    }

    public ContainerPart getClockWise()
    {
        return switch (this)
        {
            case FRONT -> RIGHT;
            case RIGHT -> BACK;
            case BACK -> LEFT;
            case LEFT -> FRONT;
            default->this;
        };
    }

    public ContainerPart getCounterClockWise()
    {
        return switch (this)
        {
            case TOP -> TOP;
            case BOTTOM -> BOTTOM;
            default->this.getClockWise().getOpposite();
        };
    }

    public Direction getWorldDirection(Direction facing) {
        return switch (this) {
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getCounterClockWise();
            case RIGHT -> facing.getClockWise();
            case NONE -> null;
        };
    }

    public static ContainerPart fromDirection(Direction facing, Direction worldDirection) {
        if (worldDirection == null) {
            return NONE;
        }

        if (worldDirection == Direction.UP)
            return TOP;
        if (worldDirection == Direction.DOWN)
            return BOTTOM;
        if (worldDirection == facing)
            return FRONT;
        if (worldDirection == facing.getOpposite())
            return BACK;
        if (worldDirection == facing.getCounterClockWise())
            return LEFT;
        if (worldDirection == facing.getClockWise())
            return RIGHT;

        return NONE;
    }
}
