package qikahome.jsonmore.minecraft.jsonscript.variable;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;

/**
 * 物品堆变量，提供 item、count、has_nbt 等属性供 JSON 访问。
 */
public class ItemStackVariable extends ScriptVariable<ItemStack> {

    public ItemStackVariable(@Nullable ItemStack value) {
        super(value);
        registerProperty("item", () -> {
            if (value == null || value.isEmpty()) return new StringVariable("minecraft:air");
            return new StringVariable(BuiltInRegistries.ITEM.getKey(value.getItem()).toString());
        });
        registerProperty("count", () -> new NumberVariable(value != null ? value.getCount() : 0));
        registerProperty("has_nbt", () -> new BooleanVariable(value != null && value.hasTag()));
        registerProperty("is_empty", () -> new BooleanVariable(value == null || value.isEmpty()));
        registerProperty("max_stack", () -> new NumberVariable(value != null ? value.getMaxStackSize() : 0));
        registerProperty("damage", () -> new NumberVariable(value != null ? value.getDamageValue() : 0));
        registerProperty("max_damage", () -> new NumberVariable(value != null ? value.getMaxDamage() : 0));
    }

    @Nullable
    public ResourceLocation itemId() {
        if (value == null || value.isEmpty()) return null;
        return BuiltInRegistries.ITEM.getKey(value.getItem());
    }

    public int count() {
        return value != null ? value.getCount() : 0;
    }

    @Nullable
    public CompoundTag nbt() {
        return value != null ? value.getTag() : null;
    }

    @Override
    public String toScriptString() {
        if (value == null || value.isEmpty()) return "minecraft:air 0";
        return BuiltInRegistries.ITEM.getKey(value.getItem()) + " " + value.getCount();
    }

    @Override
    public String type() {
        return "ItemStack";
    }
}
