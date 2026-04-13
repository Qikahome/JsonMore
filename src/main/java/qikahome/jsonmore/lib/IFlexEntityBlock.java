package qikahome.jsonmore.lib;

import dev.gigaherz.jsonthings.things.IFlexBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface IFlexEntityBlock<E extends BlockEntity> extends IFlexBlock, EntityBlock {
    BlockEntityType<E> getBlockEntityType();
    
    default boolean isValid(BlockEntityType<?> entityTypeIn) {
        return entityTypeIn == getBlockEntityType();
    }
}