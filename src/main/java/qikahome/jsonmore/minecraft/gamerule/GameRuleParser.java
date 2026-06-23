package qikahome.jsonmore.minecraft.gamerule;

import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.parsers.ThingParser;
import dev.gigaherz.jsonthings.util.parse.JParse;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

public class GameRuleParser extends ThingParser<GameRuleBuilder> {
    public static final Logger LOGGER = LogManager.getLogger();
    private static boolean registered = false;

    public GameRuleParser(IEventBus bus) {
        super(GSON, "gamerule");
        bus.addListener(this::register);
    }

    public void register(RegisterEvent event) {
        if (registered) {
            return;
        }
        registered = true;
        LOGGER.info("Started registering GameRule things, errors about unexpected registry domains are harmless...");
        for (var builder : this.getBuilders()) {
            GameRules.register(
                    builder.getRegistryName().toString().replace(':', '.'),
                    builder.getCategory(),
                    builder.build());
        }
        LOGGER.info("Done processing thingpack GameRule things.");
    }

    @Override
    public GameRuleBuilder processThing(ResourceLocation key, JsonObject data,
            Consumer<GameRuleBuilder> builderModification) {
        final GameRuleBuilder builder = new GameRuleBuilder(this, key);

        JParse.begin(data)
                .key("type", val -> val.string().map(ResourceLocation::parse).handle(builder::setType))
                .ifKey("category", val -> val.string().map(String::toUpperCase).map(GameRules.Category::valueOf).handle(builder::setCategory))
                .ifKey("events", val -> val.obj().map(super::parseEvents).handle(builder::setEventMap));
        builderModification.accept(builder);
        builder.setFactory(builder.getType().getFactory(data));
        return builder;
    }
}
