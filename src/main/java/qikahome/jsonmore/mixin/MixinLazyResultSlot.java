package qikahome.jsonmore.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import slimeknights.tconstruct.tables.block.entity.inventory.LazyResultContainer;
import slimeknights.tconstruct.tables.menu.slot.LazyResultSlot;
import static qikahome.jsonmore.JsonMore.LOGGER;

@Mixin(LazyResultSlot.class)
public abstract class MixinLazyResultSlot {
    @Redirect(method = "onTake", at = @At(value = "INVOKE", target = "Lslimeknights/tconstruct/tables/block/entity/inventory/LazyResultContainer;craftResult(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V"))
    private void redirectCraftResult(LazyResultContainer container, Player player, ItemStack resultStack,
            int amountCrafted) {
        try {
            container.craftResult(player, resultStack, amountCrafted);
        } catch (Exception e) {
            LOGGER.error("Failed to craft result: {}", e);
            net.minecraftforge.common.ForgeHooks.setCraftingPlayer(null);
            resultStack.setCount(0);
        }
    }
}