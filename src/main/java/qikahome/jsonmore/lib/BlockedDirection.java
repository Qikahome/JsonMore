package qikahome.jsonmore.lib;

import net.minecraft.core.Direction;

public enum BlockedDirection {
    NEVER {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing) {
            return false;
        }
    },
    UP {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing) {
            return isSolidBlock(level, pos.above());
        }
    },
    DOWN {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing) {
            return isSolidBlock(level, pos.below());
        }
    },
    EAST {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing) {
            return isSolidBlock(level, pos.east());
        }
    },
    WEST {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing) {
            return isSolidBlock(level, pos.west());
        }
    },
    SOUTH {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing) {
            return isSolidBlock(level, pos.south());
        }
    },
    NORTH {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing) {
            return isSolidBlock(level, pos.north());
        }
    },
    FACING {
        @Override
        public boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                Direction facing) {
            return isSolidBlock(level, pos.relative(facing));
        }
    };

    public abstract boolean isBlocked(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
            Direction facing);

    protected static boolean isSolidBlock(net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos) {
        return level.getBlockState(pos).isSolidRender(level, pos);
    }
}
