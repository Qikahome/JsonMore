package qikahome.jsonmore.minecraft.gamerule;

import java.util.function.BiConsumer;

import javax.annotation.Nonnull;

import com.google.gson.JsonObject;
import com.mojang.serialization.Lifecycle;

import dev.gigaherz.jsonthings.things.ThingRegistries;
import dev.gigaherz.jsonthings.things.events.ContextValue;
import dev.gigaherz.jsonthings.things.events.FlexEventContext;
import dev.gigaherz.jsonthings.things.events.FlexEventType;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import qikahome.jsonmore.minecraft.gamerule.GameRuleBuilder.IGameRuleBuilderSerializer;
import qikahome.jsonmore.minecraft.gamerule.GameRuleBuilder.IGameRuleBuilderFactory;

public class FlexGameRuleType<T extends GameRules.Type<?>> {
    public static final ResourceKey<Registry<FlexGameRuleType<?>>> key = ResourceKey
            .createRegistryKey(ResourceLocation.parse("jsonmore:gamerule"));

    public static final String DEFAULT_VALUE = "default_value";

    public static final ContextValue<MinecraftServer> SERVER = ContextValue.create("server", MinecraftServer.class);
    public static final ContextValue<GameRules.Value> GAMERULE = ContextValue.create("gamerule",
            GameRules.Value.class);
        public static final FlexEventType<Void> GAMERULE_CHANGE = new FlexEventType<>("gamerule_change");

    public static final Registry<FlexGameRuleType<?>> INSTANCE = Registry.register(
            ThingRegistries.THING_REGISTRIES, key.location().toString(),
            new MappedRegistry<>(key, Lifecycle.experimental(), false));

    @Nonnull
    public static <T extends GameRules.Value<T>> BiConsumer<MinecraftServer, T> createBiConsumer(
            GameRuleBuilder builder) {
        return (server, rule) -> {
            builder.runEvent(GAMERULE_CHANGE, new FlexEventContext().with(SERVER, server).with(GAMERULE, rule),
                    () -> null);
        };
    }

    public static final FlexGameRuleType<GameRules.Type<GameRules.BooleanValue>> BOOLEAN = register("jsonmore:boolean", data -> {
        boolean defaultValue = data.get(DEFAULT_VALUE).getAsBoolean();
        return (builder) -> {
            return GameRules.BooleanValue.create(defaultValue, createBiConsumer(builder));
        };
    });

    public static final FlexGameRuleType<GameRules.Type<GameRules.IntegerValue>> INTEGER = register("jsonmore:integer", data -> {
        int defaultValue = data.get(DEFAULT_VALUE).getAsInt();
        return (builder) -> {
            return GameRules.IntegerValue.create(defaultValue, createBiConsumer(builder));
        };
    });

    public static void init() {
        /* do nothing */
    }

    public static <T extends GameRules.Type<?>> FlexGameRuleType<T> register(String name,
            IGameRuleBuilderSerializer<T> factory) {
        return Registry.register(INSTANCE, name, new FlexGameRuleType<>(factory));
    }

    private final IGameRuleBuilderSerializer<T> factory;

    private FlexGameRuleType(IGameRuleBuilderSerializer<T> factory) {
        this.factory = factory;
    }

    public IGameRuleBuilderFactory<T> getFactory(JsonObject data) {
        return factory.createFactory(data);
    }

    public String toString() {
        return "FlexGameRuleType{" + INSTANCE.getKey(this) + "}";
    }
}
