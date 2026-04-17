package qikahome.jsonmore.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import static qikahome.jsonmore.lib.ContainerPart.*;

public enum ExpandableMode {
    X {
        @Override
        public BlockState tryForceConnect(BlockState orig, BlockState anoState, BlockPos anoPos, LevelAccessor level,
                Direction cliFace, boolean simulate) {
            if (cliFace.getAxis() == Direction.Axis.Y)
                return orig;
            Direction anoFacing = anoState.getValue(BlockStateProperties.FACING);
            if (anoFacing.getAxis() == Direction.Axis.Y)
                return orig;
            if (anoFacing.getClockWise() == cliFace) {
                if (!simulate)
                    connect(anoState, anoPos, level, cliFace);
                return orig.setValue(BlockStateProperties.FACING, anoFacing).setValue(PART, RIGHT);
            }
            if (anoFacing.getCounterClockWise() == cliFace) {
                if (!simulate)
                    connect(anoState, anoPos, level, cliFace);
                return orig.setValue(BlockStateProperties.FACING, anoFacing).setValue(PART, LEFT);
            }
            return orig;
        }

        @Override
        public boolean connect(BlockState anoState, BlockPos anoPos, LevelAccessor level, Direction cliFace) {
            if (cliFace.getAxis() == Direction.Axis.Y)
                return false;
            Direction anoFacing = anoState.getValue(BlockStateProperties.FACING);
            if (anoFacing.getAxis() == Direction.Axis.Y)
                return false;
            if (anoFacing.getClockWise() == cliFace) {
                return level.setBlock(anoPos, anoState.setValue(PART, LEFT), 3);
            }
            if (anoFacing.getCounterClockWise() == cliFace) {
                return level.setBlock(anoPos, anoState.setValue(PART, RIGHT), 3);

            }
            return false;
        }
    },

    Y {
        @Override
        public BlockState tryForceConnect(BlockState orig, BlockState anoState, BlockPos anoPos, LevelAccessor level,
                Direction cliFace, boolean simulate) {
            if (cliFace.getAxis() != Direction.Axis.Y)
                return orig;
            Direction anoFacing = anoState.getValue(BlockStateProperties.FACING);
            if (anoFacing.getAxis() == Direction.Axis.Y)
                return orig;
            if (cliFace == Direction.UP) {
                if (!simulate)
                    level.setBlock(anoPos, anoState.setValue(PART, BOTTOM), 3);
                return orig.setValue(BlockStateProperties.FACING, anoFacing).setValue(PART, TOP);
            }
            if (cliFace == Direction.DOWN) {
                if (!simulate)
                    level.setBlock(anoPos, anoState.setValue(PART, TOP), 3);
                return orig.setValue(BlockStateProperties.FACING, anoFacing).setValue(PART, BOTTOM);
            }
            return orig;
        }

        @Override
        public boolean connect(BlockState anoState, BlockPos anoPos, LevelAccessor level, Direction cliFace) {
            Direction anoFacing = anoState.getValue(BlockStateProperties.FACING);
            if (cliFace.getAxis() != Direction.Axis.Y)
                return false;
            if (anoFacing.getAxis() == Direction.Axis.Y)
                return false;
            if (cliFace == Direction.UP) {
                return level.setBlock(anoPos, anoState.setValue(PART, BOTTOM), 3);
            }
            if (cliFace == Direction.DOWN) {
                return level.setBlock(anoPos, anoState.setValue(PART, TOP), 3);
            }
            return false;
        }
    },

    Z {
        @Override
        public BlockState tryForceConnect(BlockState orig, BlockState anoState, BlockPos anoPos, LevelAccessor level,
                Direction cliFace, boolean simulate) {
            Direction anoFacing = anoState.getValue(BlockStateProperties.FACING);
            if (anoFacing.getAxis() != cliFace.getAxis())
                return orig;
            if (!simulate)
                level.setBlock(anoPos, anoState.setValue(PART, cliFace == anoFacing ? BACK : FRONT), 3);
            return orig.setValue(BlockStateProperties.FACING, anoFacing).setValue(PART,
                    cliFace == anoFacing ? FRONT : BACK);
        }

        @Override
        public boolean connect(BlockState anoState, BlockPos anoPos, LevelAccessor level, Direction cliFace) {
            Direction anoFacing = anoState.getValue(BlockStateProperties.FACING);
            if (anoFacing.getAxis() != cliFace.getAxis())
                return false;
            return level.setBlock(anoPos, anoState.setValue(PART, cliFace == anoFacing ? BACK : FRONT), 3);
        }
    },

    O {
        @Override
        public BlockState tryForceConnect(BlockState orig, BlockState anoState, BlockPos anoPos, LevelAccessor level,
                Direction cliFace, boolean simulate) {
            Direction anoFacing = anoState.getValue(BlockStateProperties.FACING);
            if (anoFacing != cliFace.getOpposite())
                return orig;
            if (!simulate)
                connect(anoState, anoPos, level, cliFace);
            return orig.setValue(BlockStateProperties.FACING, anoFacing.getOpposite()).setValue(PART, FRONT);
        }

        @Override
        public boolean connect(BlockState anoState, BlockPos anoPos, LevelAccessor level, Direction cliFace) {
            Direction anoFacing = anoState.getValue(BlockStateProperties.FACING);
            if (anoFacing != cliFace.getOpposite())
                return false;
            return level.setBlock(anoPos, anoState.setValue(PART, FRONT), 3);
        }

        @Override
        public BlockState tryConnect(BlockState orig, BlockState anoState, BlockPos anoPos, LevelAccessor level,
                Direction cliFace, boolean simulate) {
            return orig.getValue(BlockStateProperties.FACING).getOpposite() == anoState
                    .getValue(BlockStateProperties.FACING)
                            ? tryForceConnect(orig, anoState, anoPos, level, cliFace, simulate)
                            : orig;
        }
    };

    public abstract BlockState tryForceConnect(BlockState orig, BlockState anoState, BlockPos anoPos, LevelAccessor level,
            Direction cliFace, boolean simulate);

    public BlockState tryConnect(BlockState orig, BlockState anoState, BlockPos anoPos, LevelAccessor level,
            Direction cliFace, boolean simulate) {
        return orig.getValue(BlockStateProperties.FACING) == anoState.getValue(BlockStateProperties.FACING)
                ? tryForceConnect(orig, anoState, anoPos, level, cliFace, simulate)
                : orig;
    }

    public abstract boolean connect(BlockState anoState, BlockPos anoPos, LevelAccessor level, Direction cliFace);
}