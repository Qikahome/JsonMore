package qikahome.jsonmore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.crafting.Recipe;
import qikahome.jsonmore.lib.recipe.IConsumingRecipe;
import twilightforest.inventory.UncraftingMenu;

@Mixin(UncraftingMenu.class)
public class MixinUncraftingMenu {

    @Inject(method = "isRecipeSupported", at = @At("HEAD"), cancellable = true, remap = false)
    private static void iConsumingRecipeIsNotSupported(Recipe<?> recipe, CallbackInfoReturnable<Boolean> cir) {
        if (recipe instanceof IConsumingRecipe) {
            cir.setReturnValue(false);
        }
    }
}