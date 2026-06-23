package qikahome.jsonmore.lib.ingredient;

import java.util.ArrayList;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import qikahome.jsonmore.JsonMore;

public class CountedIngredient extends SelfConsumingIngredient {
    public static final ResourceLocation ID = ResourceLocation.parse("jsonmore:counted");
    public static final MapCodec<CountedIngredient> CODEC = RecordCodecBuilder.mapCodec(
            v -> v.group(
                    getIngredientField(),
                    Codec.INT.fieldOf("count").forGetter(i -> i.count))
                    .apply(v, CountedIngredient::new));
    public static final DeferredHolder<IngredientType<?>, IngredientType<CountedIngredient>> TYPE = JsonMore.INGREDIENT_TYPES
            .register(ID.getPath(), () -> new IngredientType<>(CODEC));

    private final int count;

    public CountedIngredient(Ingredient ingredient, int count) {
        super(ingredient);
        this.count = count;
    }

    @Override
    public ItemStack consume(ItemStack stack, ServerLevel level, @Nullable LivingEntity entity) {
        if (stack.isEmpty())
            return stack;
        ItemStack remainder = super.consume(stack, level, entity).copy();
        if (count == 0) {
            ItemStack stack2 = stack.copy();
            stack2.setCount(1);
            return stack2;
        }
        stack.shrink(count - 1);
        remainder.setCount(remainder.getCount() * count);
        return remainder;
    }

    @Override
    public Stream<ItemStack> getItems() {
        return super.getItems().map(stack -> {
            stack = stack.copy();
            if (count > 0) {
                stack.setCount(count);
            } else {
                ItemLore lore = stack.get(DataComponents.LORE);
                Component noConsume = Component.translatable("ui.jsonmore.no_consume");
                if (lore == null) {
                    stack.set(DataComponents.LORE, new ItemLore(new ArrayList<>(java.util.List.of(noConsume))));
                } else if (!lore.lines().contains(noConsume)) {
                    var lines = new ArrayList<>(lore.lines());
                    lines.add(noConsume);
                    stack.set(DataComponents.LORE, new ItemLore(lines));
                }
            }
            return stack;
        });
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (super.test(stack))
            if (stack.getCount() >= count)
                return true;
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE.get();
    }

    public static void register() {
    }
}
