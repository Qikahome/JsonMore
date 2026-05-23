package qikahome.jsonmore.minecraft.gamerule;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.builders.BaseBuilder;
import dev.gigaherz.jsonthings.things.events.FlexEventHandler;
import dev.gigaherz.jsonthings.things.events.IEventRunner;
import dev.gigaherz.jsonthings.things.parsers.ThingParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

public class GameRuleBuilder extends BaseBuilder<GameRules.Type<?>, GameRuleBuilder> implements IEventRunner{

    protected GameRuleBuilder(ThingParser<GameRuleBuilder> ownerParser, ResourceLocation registryName) {
        super(ownerParser, registryName);
    }

    private GameRules.Category category = GameRules.Category.MISC;
    private FlexGameRuleType<?> type;

    private IGameRuleBuilderFactory<?> factory;

    public void setFactory(IGameRuleBuilderFactory<?> factory) {
        this.factory = factory;
    }

    @Override
    protected String getThingTypeDisplayName() {
        return "Game Rule";
    }

    public void setType(ResourceLocation typeName) {
        var type = FlexGameRuleType.INSTANCE.get(typeName);
        if (type == null)
            throw new IllegalStateException("No known game rule type with name " + typeName);
        this.type = type;
    }

    public void setType(FlexGameRuleType<?> type) {
        if (FlexGameRuleType.INSTANCE.getKey(type) == null)
            throw new IllegalStateException("Game rule type not registered!");
        this.type = type;
    }

    public FlexGameRuleType<?> getType() {
        return type;
    }

    public void setCategory(GameRules.Category category) {
        this.category = category;
    }

    public GameRules.Category getCategory() {
        return category;
    }

    @Override
    protected GameRules.Type<?> buildInternal() {
        constructEventHandlers(getParent());
        return factory.create(this);
    }

    @FunctionalInterface
    public static interface IGameRuleBuilderFactory<T extends GameRules.Type<?>> {
        T create(GameRuleBuilder builder);
    }

    @FunctionalInterface
    public interface IGameRuleBuilderSerializer<T extends GameRules.Type<?>> {
        IGameRuleBuilderFactory<T> createFactory(JsonObject data);
    }

    private final Map<String, FlexEventHandler> eventHandlers = new HashMap<>();

    @Override
    public void addEventHandler(String eventName, FlexEventHandler eventHandler) {
        eventHandlers.put(eventName, eventHandler);
    }

    @Override
    @Nullable
    public FlexEventHandler getEventHandler(String eventName) {
        return eventHandlers.get(eventName);
    }
}
