package qikahome.jsonmore.lib.ingredient;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
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
import qikahome.jsonmore.Utils;

public class ToolDamagingIngredient extends SelfConsumingIngredient {
    public static final ResourceLocation ID = ResourceLocation.parse("jsonmore:tool_damaging");
    public static final MapCodec<ToolDamagingIngredient> CODEC = RecordCodecBuilder
            .mapCodec(v -> v.group(getIngredientField(), Codec.INT.fieldOf("damage").forGetter(i -> i.damage)).apply(v,
                    ToolDamagingIngredient::new));
    public static final DeferredHolder<IngredientType<?>, IngredientType<ToolDamagingIngredient>> TYPE = JsonMore.INGREDIENT_TYPES
            .register(ID.getPath(), () -> new IngredientType<>(CODEC));

    private final int damage;

    public ToolDamagingIngredient(Ingredient ingredient, int damage) {
        super(ingredient);
        this.damage = damage;
    }

    @Override
    public ItemStack consume(ItemStack stack, ServerLevel level, @Nullable LivingEntity entity) {
        if (stack.isEmpty())
            return stack;
        if (stack.getCount() > 1)
            throw new IllegalArgumentException("ToolDamagingIngredient only consumes single items");
        ItemStack copy = stack.copy();
        ItemStack remainder = super.consume(stack, level, entity);
        var breaked = new Utils.PackedValue<>(false);
        copy.hurtAndBreak(damage, level, entity, item -> {
            breaked.setValue(true);
        });
        if (breaked.getValue())
            copy = remainder;
        return copy;
    }

    @Override
    public Stream<ItemStack> getItems() {
        return super.getItems().map(stack -> {
            stack = stack.copy();
            if (stack.getMaxDamage() >= damage)
                stack.setDamageValue(damage);
            return stack;
        });
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (super.test(stack)) {
            // ModList modList = ModList.get();
            int maxDamage = stack.getMaxDamage();
            // if (modList.isLoaded("tconstruct"))
            // maxDamage = TConstructPlugin.getRealMaxDamage(stack, maxDamage);
            if (stack.isDamageableItem() && maxDamage - stack.getDamageValue() >= damage)
                return true;
        }
        return false;
    }

    public static void register() {
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE.get();
    }
}
