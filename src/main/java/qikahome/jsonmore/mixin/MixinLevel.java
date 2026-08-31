package qikahome.jsonmore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import qikahome.jsonmore.lib.IProtectedBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class MixinLevel {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void jsonmore$maySetBlock(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit, CallbackInfoReturnable<Boolean> info) {
        var level = (Level) (Object) (this);
        var oldBlockState = level.getBlockState(pos);
        var oldBlock = oldBlockState.getBlock();
        if (oldBlock instanceof IProtectedBlock protectedBlock) {
            if (!protectedBlock.maySetBlock(oldBlockState, level, pos, blockState, updateFlags, updateLimit)) {
                info.setReturnValue(false);
                return;
            }
        }
    }
}
