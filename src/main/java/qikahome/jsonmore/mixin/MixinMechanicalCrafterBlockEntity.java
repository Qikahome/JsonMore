package qikahome.jsonmore.mixin;

import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingInventory;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import qikahome.jsonmore.lib.recipe.IConsumingRecipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = MechanicalCrafterBlockEntity.class, remap = false)
public abstract class MixinMechanicalCrafterBlockEntity {

    @Shadow
    private RecipeGridHandler.GroupedItems groupedItems;

    @ModifyVariable(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Collection;forEach(Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER), ordinal = 0, remap = false)
    private List<ItemStack> replaceContainers(List<ItemStack> originalContainers) {
        // 完全忽略原版 forEach 收集到的物品（即丢弃所有原版容器物品）
        CraftingContainer craftinginventory = new MechanicalCraftingInventory(groupedItems);
        Level world = ((BlockEntity) (Object) this).getLevel();
        var recipe = world.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftinginventory, world).orElse(null);
        if (recipe == null || !(recipe instanceof IConsumingRecipe))
            return originalContainers;
        List<ItemStack> myContainers = new ArrayList<>();
        recipe.getRemainingItems(craftinginventory).forEach(item -> {
            if (item.isEmpty())
                return;
            myContainers.add(item);
        });
        return myContainers; // 返回的新列表将替代原 containers 变量
    }
}