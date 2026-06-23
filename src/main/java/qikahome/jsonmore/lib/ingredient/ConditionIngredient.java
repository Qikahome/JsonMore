package qikahome.jsonmore.lib.ingredient;

import java.util.stream.Stream;

import javax.annotation.Nullable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import qikahome.jsonmore.JsonMore;

public class ConditionIngredient extends SelfConsumingIngredient {
    public static final ResourceLocation ID = ResourceLocation.parse("jsonmore:condition");
    private static final String DEFAULT_MESSAGE = "recipe.jsonmore.disabled";

    public static final MapCodec<ConditionIngredient> CODEC = RecordCodecBuilder.mapCodec(
            v -> v.group(
                    getIngredientField(),
                    ICondition.CODEC.fieldOf("condition").forGetter(i -> i.condition),
                    Codec.STRING.optionalFieldOf("message", DEFAULT_MESSAGE).forGetter(i -> i.message))
                    .apply(v, ConditionIngredient::new));
    public static final DeferredHolder<IngredientType<?>, IngredientType<ConditionIngredient>> TYPE = JsonMore.INGREDIENT_TYPES
            .register(ID.getPath(), () -> new IngredientType<>(CODEC));

    @Nullable
    private final ICondition condition;
    private final String message;

    private ConditionIngredient(Ingredient ingredient, @Nullable ICondition condition, String message) {
        super(ingredient);
        this.condition = condition;
        this.message = message;
    }

    private boolean passes() {
        return condition.test(ICondition.IContext.EMPTY);
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (!passes())
            return false;
        return super.test(stack);
    }

    @Override
    public Stream<ItemStack> getItems() {
        if (!passes()) {
            ItemStack barrier = new ItemStack(Items.BARRIER);
            barrier.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.translatable(message));
            return Stream.of(barrier);
        }
        return super.getItems();
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE.get();
    }

    public static void register() {
    }
}
