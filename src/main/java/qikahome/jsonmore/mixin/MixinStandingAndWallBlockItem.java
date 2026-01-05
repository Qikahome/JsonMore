package qikahome.jsonmore.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import qikahome.jsonmore.minecraft.FlexSignItem;

import org.spongepowered.asm.mixin.injection.At;

@Mixin(StandingAndWallBlockItem.class)
public class MixinStandingAndWallBlockItem {
    @Shadow
    protected final Block wallBlock;

    public MixinStandingAndWallBlockItem() {
        this.wallBlock = Blocks.AIR;
    }

    @Redirect(method = "*", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/StandingAndWallBlockItem;wallBlock:Lnet/minecraft/world/level/block/Block;", opcode = Opcodes.GETFIELD))
    private Block redirectWallBlockRead(StandingAndWallBlockItem instance) {
        if (instance instanceof FlexSignItem flexSignItem) {
            return flexSignItem.getWallBlock();
        }
        return wallBlock;
    }
}
