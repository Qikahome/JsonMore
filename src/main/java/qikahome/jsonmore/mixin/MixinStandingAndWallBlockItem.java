package qikahome.jsonmore.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import qikahome.jsonmore.minecraft.IFlexStandingAndWallBlockItem;

import org.spongepowered.asm.mixin.injection.At;

@Mixin(StandingAndWallBlockItem.class)
public class MixinStandingAndWallBlockItem {

    @Shadow
    @Mutable
    protected Block wallBlock;

    @Redirect(method = "*", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/StandingAndWallBlockItem;wallBlock:Lnet/minecraft/world/level/block/Block;", opcode = Opcodes.GETFIELD))
    private Block redirectWallBlockRead(StandingAndWallBlockItem instance) {
        if (wallBlock == null && instance instanceof IFlexStandingAndWallBlockItem flexStandingAndWallBlockItem) {
            wallBlock = flexStandingAndWallBlockItem.getWallBlock();
        }
        return wallBlock;
    }
}
