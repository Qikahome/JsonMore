package qikahome.jsonmore.mixin;

import dev.gigaherz.jsonthings.things.blocks.FlexHorizontalDirectionalBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修复 {@link FlexHorizontalDirectionalBlock#getStateForPlacement} 使用
 * {@code getNearestLookingDirection()} 导致朝上/下时崩溃的 bug。
 * <p>
 * 上游 PR 见：gigaherz/JsonThings#（待补充）
 */
@Mixin(FlexHorizontalDirectionalBlock.class)
public abstract class MixinFlexHorizontalDirectionalBlock {

    @Inject(method = "getStateForPlacement", at = @At("HEAD"), cancellable = true)
    private void fixGetStateForPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        var self = (FlexHorizontalDirectionalBlock) (Object) this;
        cir.setReturnValue(self.defaultBlockState().setValue(
                net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                context.getHorizontalDirection().getOpposite()
        ));
    }
}
