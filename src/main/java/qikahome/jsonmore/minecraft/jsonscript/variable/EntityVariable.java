package qikahome.jsonmore.minecraft.jsonscript.variable;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;

/**
 * 实体变量，提供 uuid、type、name、pos、health 等属性供 JSON 访问。
 */
public class EntityVariable extends ScriptVariable<Entity> {

    public EntityVariable(@Nullable Entity value) {
        super(value);
        registerProperty("uuid", () -> new StringVariable(value != null ? value.getUUID().toString() : ""));
        registerProperty("type", () -> {
            if (value == null) return new StringVariable("minecraft:entity");
            return new StringVariable(BuiltInRegistries.ENTITY_TYPE.getKey(value.getType()).toString());
        });
        registerProperty("name", () -> {
            if (value == null) return new StringVariable("");
            return new StringVariable(value.getName().getString());
        });
        registerProperty("is_player", () -> new BooleanVariable(value instanceof Player));
        registerProperty("is_living", () -> new BooleanVariable(value instanceof LivingEntity));
        registerProperty("sneaking", () -> new BooleanVariable(value != null && value.isShiftKeyDown()));
        registerProperty("pos", () -> new BlockPosVariable(value != null ? value.blockPosition() : null));
        registerProperty("health", () -> {
            if (value instanceof LivingEntity living) {
                return new NumberVariable(living.getHealth());
            }
            return new NumberVariable(0);
        });
        registerProperty("max_health", () -> {
            if (value instanceof LivingEntity living) {
                return new NumberVariable(living.getMaxHealth());
            }
            return new NumberVariable(0);
        });
        registerProperty("mainhand", () -> {
            if (value instanceof Player player) {
                return new ItemStackVariable(player.getMainHandItem());
            }
            return NullVariable.INSTANCE;
        });
        registerProperty("offhand", () -> {
            if (value instanceof Player player) {
                return new ItemStackVariable(player.getOffhandItem());
            }
            return NullVariable.INSTANCE;
        });
    }

    @Nullable
    public String uuid() {
        return value != null ? value.getUUID().toString() : null;
    }

    @Override
    public String toScriptString() {
        if (value == null) return "00000000-0000-0000-0000-000000000000";
        return value.getUUID().toString();
    }

    @Override
    public String type() {
        return "Entity";
    }
}
