package qikahome.jsonmore.minecraft.gamerule;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.builders.BaseBuilder;
import dev.gigaherz.jsonthings.things.events.FlexEventHandler;
import dev.gigaherz.jsonthings.things.events.FlexEventType;
import dev.gigaherz.jsonthings.things.events.IEventRunner;
import dev.gigaherz.jsonthings.things.parsers.ThingParser;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRules;

public class GameRuleBuilder extends BaseBuilder<GameRule<?>, GameRuleBuilder> implements IEventRunner {

    protected GameRuleBuilder(ThingParser<GameRule<?>, GameRuleBuilder> ownerParser, Identifier registryName) {
        super(ownerParser, registryName);
    }

    private GameRuleCategory category = GameRuleCategory.MISC;
    private FlexGameRuleType type;

    private IGameRuleBuilderFactory factory;

    public void setFactory(IGameRuleBuilderFactory factory) {
        this.factory = factory;
    }

    @Override
    protected String getThingTypeDisplayName() {
        return "Game Rule";
    }

    public void setType(Identifier typeName) {
        this.type = FlexGameRuleType.INSTANCE.getOptional(typeName).orElseThrow(()->new IllegalStateException("No known game rule type with name " + typeName));
    }

    public void setType(FlexGameRuleType type) {
        if (FlexGameRuleType.INSTANCE.getKey(type) == null)
            throw new IllegalStateException("Game rule type not registered!");
        this.type = type;
    }

    public FlexGameRuleType getType() {
        return type;
    }

    public void setCategory(GameRuleCategory category) {
        this.category = category;
    }

    public GameRuleCategory getCategory() {
        return category;
    }

    @Override
    protected GameRule<?> buildInternal() {
        constructEventHandlers(getParent());
        return factory.create(this);
    }

    @FunctionalInterface
    public static interface IGameRuleBuilderFactory {
        GameRule<?> create(GameRuleBuilder builder);
    }

    @FunctionalInterface
    public interface IGameRuleBuilderSerializer {
        IGameRuleBuilderFactory createFactory(JsonObject data);
    }

    private final Map<FlexEventType, FlexEventHandler> eventHandlers = new HashMap<>();

    @Override
    public <T> void addEventHandler(FlexEventType<T> event, FlexEventHandler<T> eventHandler) {
        eventHandlers.put(event, eventHandler);
    }

    @Override
    @Nullable
    public <T> FlexEventHandler<T> getEventHandler(FlexEventType<T> event) {
        return eventHandlers.get(event);
    }

    @Override
    public void validate()
    {
    }
}
