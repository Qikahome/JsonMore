package qikahome.jsonmore.mixin; // 替换为你的MOD实际包名

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import qikahome.jsonmore.lib.IFlexEntityBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityType.class)
public abstract class MixinBlockEntityType<T extends BlockEntity> {
        @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
        public void isValid(BlockState p_155263_, CallbackInfoReturnable<Boolean> cir) {
                if (p_155263_.getBlock() instanceof IFlexEntityBlock<?> feb) {
                        cir.setReturnValue(feb.isValid((BlockEntityType)(Object)this));
                        cir.cancel();
                }
        }
}