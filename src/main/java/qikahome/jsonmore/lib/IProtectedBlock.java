package qikahome.jsonmore.lib;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface IProtectedBlock {
    /**
     * 是否可以设置块状态
     * @param oldState 旧块状态
     * @param level 世界
     * @param pos 块位置
     * @param newState 新块状态
     * @param updateFlags 更新标志
     * @param updateLimit 更新限制
     * @return 是否可以设置
     */
    boolean maySetBlock(BlockState oldState, Level level, BlockPos pos, BlockState newState, @Block.UpdateFlags int updateFlags, int updateLimit);
}
