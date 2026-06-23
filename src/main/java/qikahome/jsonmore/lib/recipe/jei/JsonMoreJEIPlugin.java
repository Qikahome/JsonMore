package qikahome.jsonmore.lib.recipe.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import qikahome.jsonmore.lib.recipe.ItemApplicationRecipe;

import java.util.List;

@JeiPlugin
public class JsonMoreJEIPlugin implements IModPlugin {
    private static final ResourceLocation ID = ResourceLocation.parse("jsonmore:jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new ItemApplicationRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
        List<RecipeHolder<ItemApplicationRecipe>> recipes = recipeManager.getAllRecipesFor(ItemApplicationRecipe.TYPE);
        registration.addRecipes(ItemApplicationRecipeCategory.TYPE, recipes.stream().map(RecipeHolder::value).toList());
    }
}
