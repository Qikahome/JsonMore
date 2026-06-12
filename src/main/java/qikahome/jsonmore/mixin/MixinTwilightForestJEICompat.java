package qikahome.jsonmore.mixin;

import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import qikahome.jsonmore.lib.recipe.IConsumingRecipe;
import twilightforest.compat.jei.JEICompat;
import twilightforest.compat.jei.categories.JEIUncraftingCategory;

import java.util.List;

@Mixin(JEICompat.class)
public class MixinTwilightForestJEICompat {

    @Redirect(method = "registerRecipes", at = @At(value = "INVOKE", target = "Lmezz/jei/api/registration/IRecipeRegistration;addRecipes(Lmezz/jei/api/recipe/RecipeType;Ljava/util/List;)V"), remap = false)
    private void redirectAddRecipes(IRecipeRegistration registration, RecipeType<CraftingRecipe> recipeType,
            List<CraftingRecipe> recipes) {
        if (recipeType == JEIUncraftingCategory.UNCRAFTING)
            recipes.removeIf(r -> r instanceof IConsumingRecipe);
        registration.addRecipes(recipeType, recipes);
    }
}