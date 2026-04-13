package qikahome.jsonmore.lib;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;

public enum PlacingDirections {
    UP {
        @Override
        public Direction getDirection(BlockPlaceContext context) {
            return Direction.UP;
        }
    },
    DOWN {
        @Override
        public Direction getDirection(BlockPlaceContext context) {
            return Direction.DOWN;
        }
    },
    NORTH {
        @Override
        public Direction getDirection(BlockPlaceContext context) {
            return Direction.NORTH;
        }
    },
    SOUTH {
        @Override
        public Direction getDirection(BlockPlaceContext context) {
            return Direction.SOUTH;
        }
    },
    EAST {
        @Override
        public Direction getDirection(BlockPlaceContext context) {
            return Direction.EAST;
        }
    },
    WEST {
        @Override
        public Direction getDirection(BlockPlaceContext context) {
            return Direction.WEST;
        }
    },
    FACING_HORIZONTAL {
        @Override
        public Direction getDirection(BlockPlaceContext context) {
            return context.getHorizontalDirection().getOpposite();
        }
    },
    CLICKING_HORIZONTAL {
        @Override
        public Direction getDirection(BlockPlaceContext context) {
            return context.getClickedFace().getAxis() != Direction.Axis.Y
                    ? context.getClickedFace().getOpposite()
                    : FACING_HORIZONTAL.getDirection(context);
        }
    },
    VERTICAL {
        @Override
        public Direction getDirection(BlockPlaceContext context) {
            return context.getClickedFace().getAxis() == Direction.Axis.Y
                    ? context.getClickedFace()
                    : (context.getPlayer() != null && context.getPlayer().getXRot() > 0
                            ? Direction.UP
                            : Direction.DOWN);
        }
    },
    FACING_ALL {
        @Override
        public Direction getDirection(BlockPlaceContext context) {
            return context.getNearestLookingDirection().getOpposite();
        }
    },
    CLICKING_ALL {
        @Override
        public Direction getDirection(BlockPlaceContext context) {
            return context.getClickedFace().getOpposite();
        }
    };

    public abstract Direction getDirection(BlockPlaceContext context);
}
