package qikahome.jsonmore.mixin;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.gigaherz.jsonthings.things.items.FlexBlockItem;
import qikahome.jsonmore.lib.KeepInventoryMode;
import qikahome.jsonmore.minecraft.FlexBarrelBlock;

@Mixin(FlexBlockItem.class)
public class MixinFlexBarrelBlockItem {

    @Inject(method = "canFitInsideContainerItems", at = @At("HEAD"), cancellable = true)
    private void jsonmore$canFitInsideContainerItems(CallbackInfoReturnable<Boolean> cir) {
        Block block = ((BlockItem) (Object) this).getBlock();
        if (block instanceof FlexBarrelBlock flexBarrelBlock && flexBarrelBlock.keepInventory != KeepInventoryMode.NEVER) {
            cir.setReturnValue(false);
        }
    }
}
