package qikahome.jsonmore.minecraft.gamerule;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import qikahome.jsonmore.JsonMore;
import qikahome.jsonmore.Utils;
import qikahome.jsonmore.Utils.IntRange;

public class GameRuleCondition implements ICondition {
    public static final ResourceLocation ID = ResourceLocation.parse("jsonmore:gamerule");
    public static final MapCodec<GameRuleCondition> CODEC = RecordCodecBuilder.mapCodec(
            v -> v.group(
                    Codec.STRING.fieldOf("rule").forGetter(c -> c.ruleName),
                    Utils.IntRange.CODEC.optionalFieldOf("value").forGetter(c -> c.valueRange))
                    .apply(v, GameRuleCondition::new));
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<GameRuleCondition>> HOLDER = JsonMore.CONDITION_CODECS
            .register("gamerule", () -> CODEC);

    public static void register() {
    }

    private final String ruleName;
    private final Optional<IntRange> valueRange;

    public GameRuleCondition(String ruleName, Optional<IntRange> valueRange) {
        this.ruleName = ruleName;
        this.valueRange = valueRange;
    }

    @Override
    public boolean test(IContext context) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

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
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
