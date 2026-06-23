package qikahome.jsonmore.lib.ingredient;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import qikahome.jsonmore.JsonMore;

public class RemainderOverrideIngredient extends SelfConsumingIngredient {
    public static final ResourceLocation ID = ResourceLocation.parse("jsonmore:remainder_override");
    public static final MapCodec<RemainderOverrideIngredient> CODEC = RecordCodecBuilder.mapCodec(
            v -> v.group(
                    getIngredientField(),
                    ItemStack.CODEC.fieldOf("remainder_override").forGetter(i -> i.remainderOverride))
                    .apply(v, RemainderOverrideIngredient::new));
    public static final DeferredHolder<IngredientType<?>, IngredientType<RemainderOverrideIngredient>> TYPE = JsonMore.INGREDIENT_TYPES
            .register(ID.getPath(), () -> new IngredientType<>(CODEC));

    private final ItemStack remainderOverride;

    public RemainderOverrideIngredient(Ingredient ingredient, ItemStack remainderOverride) {
        super(ingredient);
        if (ingredient.getCustomIngredient() instanceof SelfConsumingIngredient)
            throw new IllegalArgumentException(
                    "RemainderOverrideIngredient's inner ingredient must not be a SelfConsumingIngredient");
        this.remainderOverride = remainderOverride.copy();
    }

    @Override
    public ItemStack consume(ItemStack stack, ServerLevel world, @Nullable LivingEntity entity) {
        SelfConsumingIngredient.consume(ingredient, stack, world, entity);
        return remainderOverride.copy();
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE.get();
    }

    public static void register() {
    }
}
