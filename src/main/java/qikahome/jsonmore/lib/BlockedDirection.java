package qikahome.jsonmore.lib;

import net.minecraft.core.Direction;

public enum BlockedDirection {
    NEVER {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing, ContainerPart partType) {
            return false;
        }
    },
    UP {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing, ContainerPart partType) {
            return partType!=ContainerPart.BOTTOM && isSolidBlock(level, pos.above());
        }
    },
    DOWN {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing, ContainerPart partType) {
            return partType!=ContainerPart.TOP && isSolidBlock(level, pos.below());
        }
    },
    EAST {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing, ContainerPart partType) {
            return partType.getWorldDirection(facing).getOpposite() != Direction.EAST && isSolidBlock(level, pos.east());
        }
    },
    WEST {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing, ContainerPart partType) {
            return partType.getWorldDirection(facing).getOpposite() != Direction.WEST && isSolidBlock(level, pos.west());
        }
    },
    SOUTH {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing, ContainerPart partType) {
            return partType.getWorldDirection(facing).getOpposite() != Direction.SOUTH && isSolidBlock(level, pos.south());
        }
    },
    NORTH {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing, ContainerPart partType) {
            return partType.getWorldDirection(facing).getOpposite() != Direction.NORTH && isSolidBlock(level, pos.north());
        }
    },
    FACING {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing, ContainerPart partType) {
            return partType!=ContainerPart.BACK && isSolidBlock(level, pos.relative(facing));
        }
    };

    public abstract boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
            Direction facing, ContainerPart partType);

    protected static boolean isSolidBlock(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos) {
        return level.getBlockState(pos).isSolidRender();
    }
}
