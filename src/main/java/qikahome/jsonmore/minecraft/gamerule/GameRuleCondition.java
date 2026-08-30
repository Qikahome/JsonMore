package qikahome.jsonmore.minecraft.gamerule;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import qikahome.jsonmore.JsonMore;
import qikahome.jsonmore.Utils;
import qikahome.jsonmore.Utils.IntRange;

public class GameRuleCondition implements ICondition {
    public static final Identifier ID = Identifier.parse("jsonmore:gamerule");
    public static final MapCodec<GameRuleCondition> CODEC = RecordCodecBuilder.mapCodec(
            v -> v.group(
                    Identifier.CODEC.fieldOf("rule").forGetter(c -> c.ruleName),
                    Utils.IntRange.CODEC.optionalFieldOf("value").forGetter(c -> c.valueRange))
                    .apply(v, GameRuleCondition::new));
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<GameRuleCondition>> HOLDER = JsonMore.CONDITION_CODECS
            .register("gamerule", () -> CODEC);

    public static void register() {
    }

    private final Identifier ruleName;
    private final Optional<IntRange> valueRange;

    public GameRuleCondition(Identifier ruleName, Optional<IntRange> valueRange) {
        this.ruleName = ruleName;
        this.valueRange = valueRange;
    }

    @Override
    public boolean test(IContext context) {

                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null)
            return false;

        GameRule<?> rule = BuiltInRegistries.GAME_RULE.getValue(ruleName);
        if (rule == null)
            return false;

        if (!valueRange.isPresent()) {
            return server.getGameRules().get(rule) instanceof Boolean bool?bool:false;
        }

        return server.getGameRules().get(rule) instanceof Number num?valueRange.get().contains(num.intValue()):false;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
