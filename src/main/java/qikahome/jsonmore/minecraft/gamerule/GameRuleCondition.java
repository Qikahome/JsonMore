package qikahome.jsonmore.minecraft.gamerule;

import java.util.Optional;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import qikahome.jsonmore.Utils;
import qikahome.jsonmore.Utils.IntRange;

/**
 * 资源条件 {@code jsonmore:gamerule}：按游戏规则名（可带取值范围）决定资源是否加载。
 * <p>
 * 对应上游 Neo 的 {@code ICondition}，Fabric 侧等价物是 {@link ResourceCondition}。
 */
public class GameRuleCondition implements ResourceCondition {
    public static final ResourceLocation ID = ResourceLocation.parse("jsonmore:gamerule");
    public static final MapCodec<GameRuleCondition> CODEC = RecordCodecBuilder.mapCodec(
            v -> v.group(
                    Codec.STRING.fieldOf("rule").forGetter(c -> c.ruleName),
                    IntRange.CODEC.optionalFieldOf("value").forGetter(c -> c.valueRange))
                    .apply(v, GameRuleCondition::new));
    public static final ResourceConditionType<GameRuleCondition> TYPE = ResourceConditionType.create(ID, CODEC);

    public static void register() {
        ResourceConditions.register(TYPE);
    }

    private final String ruleName;
    private final Optional<IntRange> valueRange;

    public GameRuleCondition(String ruleName, Optional<IntRange> valueRange) {
        this.ruleName = ruleName;
        this.valueRange = valueRange;
    }

    @Override
    public boolean test(@Nullable HolderLookup.Provider registryLookup) {
        MinecraftServer server = Utils.getCurrentServer();

        if (server == null)
            return false;

        GameRules.Key<?> foundKey = findKey(ruleName);
        if (foundKey == null)
            return false;

        GameRules.Value<?> value = server.getGameRules().getRule(foundKey);
        if (value == null)
            return false;

        if (!valueRange.isPresent()) {
            if (value instanceof GameRules.BooleanValue bv)
                return bv.get();
            return false;
        }

        if (value instanceof GameRules.IntegerValue iv) {
            return valueRange.get().contains(iv.get());
        }
        return false;
    }

    private static GameRules.Key<?> findKey(String name) {
        for (var entry : GameRules.GAME_RULE_TYPES.entrySet()) {
            if (entry.getKey().toString().equals(name))
                return entry.getKey();
        }
        return null;
    }

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }
}
