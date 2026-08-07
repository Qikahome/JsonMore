package qikahome.jsonmore.mantle.ingredient;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import qikahome.jsonmore.lib.ingredient.SelfConsumingIngredient;
import slimeknights.mantle.recipe.ingredient.FluidIngredient;

public class FluidItemIngredient extends SelfConsumingIngredient {
    public static final ResourceLocation ID = new ResourceLocation("jsonmore:fluid");
    private final FluidIngredient fluid;

    public FluidItemIngredient(Ingredient ingredient, FluidIngredient fluid) {
        super(ingredient);
        this.fluid = fluid;
    }

    @Override
    public ItemStack consume(ItemStack stack) {
        if (stack.isEmpty())
            return stack;
        var remainder=stack.copy();
        remainder.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler->
            {
                for(var fl:fluid.getFluids())
                    if(handler.drain(fl,FluidAction.SIMULATE).equals(fl))
                    {
                        handler.drain(fl,FluidAction.EXECUTE);
                        return;
                    }
            }
        );
        return remainder;
    }

    @Override
    public ItemStack[] getItems() {
        ItemStack[] items = super.getItems();
        var fluids = fluid.getFluids();
        List<ItemStack> result = new ArrayList<>(items.length * fluids.size());
        for (ItemStack item : items) {
            for (var fl : fluids) {
                ItemStack copy = item.copy();
                boolean ok = copy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                        .map(handler -> handler.fill(fl, FluidAction.EXECUTE) == fl.getAmount())
                        .orElse(false);
                if (ok)
                    result.add(copy);
            }
        }
        return result.toArray(new ItemStack[0]);
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (super.test(stack)) {
            var capa = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
            var handler = capa.resolve().orElse(null);
            if (handler != null) {
                for (var fl : fluid.getFluids())
                    if (handler.drain(fl, FluidAction.SIMULATE).equals(fl))
                        return true;
            }
        }

        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", ID.toString());
        json.add("ingredient", ingredient.toJson());
        json.add("fluid",fluid.serialize());
        return json;
    }

    public static class Serializer implements IIngredientSerializer<FluidItemIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public FluidItemIngredient parse(FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            FluidIngredient fluid = FluidIngredient.LOADABLE.decode(buffer);
            return new FluidItemIngredient(ingredient, fluid);
        }

        @Override
        public FluidItemIngredient parse(JsonObject json) {
            if (!json.has("ingredient")) {
                throw new JsonParseException("Fluid ingredient must have 'ingredient' field");
            }
            if (!json.has("fluid")) {
                throw new JsonParseException("Fluid ingredient must have 'fluid' field");
            }
            Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"));

            FluidIngredient fluid = FluidIngredient.LOADABLE.getIfPresent(json, "fluid");
            if (fluid.getFluids().isEmpty()) {
                throw new JsonParseException("Fluid ingredient must have at least one fluid");
            }

            return new FluidItemIngredient(ingredient, fluid);
        }

        @Override
        public void write(FriendlyByteBuf buffer, FluidItemIngredient ingredient) {
            ingredient.ingredient.toNetwork(buffer);
            FluidIngredient.LOADABLE.encode(buffer, ingredient.fluid);
        }
    }

    public static void register() {
        CraftingHelper.register(ID, Serializer.INSTANCE);
    }
}
