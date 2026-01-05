package qikahome.jsonmore.tconstruct.parts;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.builders.BaseBuilder;
import dev.gigaherz.jsonthings.things.parsers.ThingParser;
import dev.gigaherz.jsonthings.util.parse.JParse;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import slimeknights.tconstruct.library.materials.MaterialRegistry;

public class MaterialStatTypeParser extends ThingParser<MaterialStatTypeBuilder> {

    public static final Logger LOGGER = LogManager.getLogger();

    public MaterialStatTypeParser(IEventBus bus) {
        super(GSON, "material_stat_type");
    }

    public void onCommonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("JsonMore: Start registering material stat types.");
        processAndConsumeErrors(getThingType(), getBuilders(),
                thing -> MaterialRegistry.getInstance().registerStatType(thing.build()),
                BaseBuilder::getRegistryName);
        LOGGER.info("JsonMore: Finished registering material stat types.");
    }

    @Override
    public MaterialStatTypeBuilder processThing(ResourceLocation key, JsonObject data,
            Consumer<MaterialStatTypeBuilder> builderModification) {
        LOGGER.info("JsonMore: Processing material stat type {}", key);
        final MaterialStatTypeBuilder builder = MaterialStatTypeBuilder.begin(this, key);

        JParse.begin(data)
                .ifKey("parent", val -> val.string().map(ResourceLocation::new).handle(builder::setParent))
                .ifKey("type", val -> val.string().handle(builder::setType))
                .ifKey("can_repair", val -> val.bool().handle(builder::setCanRepair))
                .ifKey("stats", val -> val.ifObj(obj -> builder.setStats(obj.getAsJsonObject())).typeError());

        builderModification.accept(builder);

        builder.setFactory(builder.getMaterialStatTypeType().getFactory(data));

        return builder;
    }
}
