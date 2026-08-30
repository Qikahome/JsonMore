package qikahome.jsonmore.minecraft.gamerule;

import com.google.gson.JsonObject;
import dev.gigaherz.jsonthings.things.parsers.ThingParser;
import dev.gigaherz.jsonthings.util.parse.JParse;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.neoforged.bus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

public class GameRuleParser extends ThingParser<GameRule<?>,GameRuleBuilder> {
    public static final Logger LOGGER = LogManager.getLogger();

    public GameRuleParser(IEventBus bus) {
        super(GSON, "gamerule");
        register(bus, Registries.GAME_RULE);
    }

    @Override
    public GameRuleBuilder processThing(Identifier key, JsonObject data,
            Consumer<GameRuleBuilder> builderModification) {
        final GameRuleBuilder builder = new GameRuleBuilder(this, key);

        JParse.begin(data)
                .key("type", val -> val.string().map(Identifier::parse).handle(builder::setType))
                .ifKey("category", val -> val.string().map(Identifier::parse).map(GameRuleCategory::new).handle(builder::setCategory))
                .ifKey("events", val -> val.obj().map(super::parseEvents).handle(builder::setEventMap));
        builderModification.accept(builder);
        builder.setFactory(builder.getType().getFactory(data));
        return builder;
    }
}
