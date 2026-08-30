package qikahome.jsonmore.minecraft.gamerule;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;
import javax.annotation.Nonnull;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;

import dev.gigaherz.jsonthings.things.ThingRegistries;
import dev.gigaherz.jsonthings.things.events.ContextValue;
import dev.gigaherz.jsonthings.things.events.FlexEventContext;
import dev.gigaherz.jsonthings.things.events.FlexEventType;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import qikahome.jsonmore.minecraft.gamerule.GameRuleBuilder.IGameRuleBuilderSerializer;
import qikahome.jsonmore.minecraft.gamerule.GameRuleBuilder.IGameRuleBuilderFactory;

public class FlexGameRuleType {
    public static final ResourceKey<Registry<FlexGameRuleType>> key = ResourceKey
            .createRegistryKey(Identifier.parse("jsonmore:gamerule"));

    public static final String DEFAULT_VALUE = "default_value";

    public static final String MIN_VALUE = "min_value";
    public static final String MAX_VALUE = "max_value";

    public static final Map<GameRule<?>,BiConsumer<MinecraftServer,GameRule<?>>> events = new HashMap<>();

    public static final ContextValue<MinecraftServer> SERVER = ContextValue.create("server", MinecraftServer.class);
    public static final ContextValue<GameRule> GAMERULE = ContextValue.create("gamerule",            GameRule.class);
    public static final FlexEventType<Void> GAMERULE_CHANGE = new FlexEventType<>("gamerule_change");

    public static <T> GameRule<T> getGameRule(GameRuleBuilder builder,GameRuleType type,ArgumentType<T> argType,GameRules.VisitorCaller<T> visitorCaller,Codec<T> codec,ToIntFunction<T> commandResultFunction, T defaultValue)
    {
        return new GameRule<T>(builder.getCategory(),type,argType,visitorCaller,codec,commandResultFunction,defaultValue,FeatureFlagSet.of());
    }
    

    public static final Registry<FlexGameRuleType> INSTANCE = Registry.register(
            ThingRegistries.THING_REGISTRIES, key.identifier(),
            new MappedRegistry<FlexGameRuleType>(key, Lifecycle.experimental(), false));

    @Nonnull
    public static BiConsumer<MinecraftServer, GameRule<?>> createBiConsumer(GameRuleBuilder builder) {
        return (server, rule) -> {
            builder.runEvent(GAMERULE_CHANGE, new FlexEventContext().with(SERVER, server).with(GAMERULE, rule),
                    () -> null);
        };
    }

    public static final FlexGameRuleType BOOLEAN = register("jsonmore:boolean", data -> {
        boolean defaultValue = data.get(DEFAULT_VALUE).getAsBoolean();
        return (builder) -> {
            var rule=getGameRule(builder, GameRuleType.BOOL, BoolArgumentType.bool(), GameRuleTypeVisitor::visitBoolean, Codec.BOOL, b -> b ? 1 : 0, defaultValue);
            events.put(rule,createBiConsumer(builder));
            return rule;
        };
    });

    public static final FlexGameRuleType INTEGER = register("jsonmore:integer", data -> {
        int defaultValue = data.get(DEFAULT_VALUE).getAsInt();
        int min = GsonHelper.getAsInt(data, MIN_VALUE, Integer.MIN_VALUE);
        int max = GsonHelper.getAsInt(data, MAX_VALUE, Integer.MAX_VALUE);
        return (builder) -> {
            var rule=getGameRule(builder, GameRuleType.INT, IntegerArgumentType.integer(min, max), GameRuleTypeVisitor::visitInteger, Codec.intRange(min, max), i->i, defaultValue);
            events.put(rule,createBiConsumer(builder));
            return rule;
        };
    });

    public static void init() {
        /* do nothing */
    }

    public static FlexGameRuleType register(String name,
            IGameRuleBuilderSerializer factory) {
        return Registry.register(INSTANCE, name, new FlexGameRuleType(factory));
    }

    private final IGameRuleBuilderSerializer factory;

    private FlexGameRuleType(IGameRuleBuilderSerializer factory) {
        this.factory = factory;
    }

    public IGameRuleBuilderFactory getFactory(JsonObject data) {
        return factory.createFactory(data);
    }

    public String toString() {
        return "FlexGameRuleType{" + INSTANCE.getKey(this) + "}";
    }
}
