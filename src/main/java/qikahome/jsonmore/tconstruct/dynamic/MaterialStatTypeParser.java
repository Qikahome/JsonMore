package qikahome.jsonmore.tconstruct.dynamic;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.builders.BaseBuilder;
import dev.gigaherz.jsonthings.things.parsers.ThingParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import slimeknights.tconstruct.library.materials.MaterialRegistry;

public class MaterialStatTypeParser extends ThingParser<MaterialStatTypeParser.MaterialStatTypeBuilder> {
    public static class MaterialStatTypeBuilder extends BaseBuilder<DynamicMaterialStatType, MaterialStatTypeBuilder> {
        private final DynamicMaterialStatType statType;
        protected MaterialStatTypeBuilder(ThingParser<MaterialStatTypeBuilder> ownerParser, JsonObject data,
                ResourceLocation registryName) {
            super(ownerParser, registryName);
            data.addProperty("id", registryName.toString());
            this.statType = DynamicMaterialStatType.LOADER.deserialize(data);
        }

        @Override
        protected String getThingTypeDisplayName() {
            return "Dynamic Material Stat Type";
        }

        @Override
        protected DynamicMaterialStatType buildInternal() {
            return statType;
        }
    }
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
        final MaterialStatTypeBuilder builder = new MaterialStatTypeBuilder(this, data, key);
        builderModification.accept(builder);
        return builder;
    }
}
