package qikahome.jsonmore.minecraft;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.gigaherz.jsonthings.things.builders.BaseBuilder;
import dev.gigaherz.jsonthings.things.parsers.ThingParseException;
import dev.gigaherz.jsonthings.things.parsers.ThingParser;
import dev.gigaherz.jsonthings.util.parse.JParse;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.PathPackResources.PathResourcesSupplier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.Pack.Metadata;
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public class BuiltInDatapackParser extends ThingParser<RepositorySource,BuiltInDatapackParser.Builder> {

    public static class Builder extends BaseBuilder<RepositorySource, Builder> {

        protected Builder(ThingParser<RepositorySource,Builder> ownerParser, Identifier registryName) {
            super(ownerParser, registryName);
            this.displayName = Component
                    .translatable("pack." + registryName.toString().replace(":", ".").replace("/", "."));
        }

        private Boolean defaultEnable = false;
        private Component displayName;

        public Builder defaultEnable(Boolean defaultEnable) {
            this.defaultEnable = defaultEnable;
            return this;
        }

        public Builder displayName(Component displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        protected String getThingTypeDisplayName() {
            return "BuiltInDatapack";
        }

        @Override
        protected RepositorySource buildInternal() {
            var regName = this.getRegistryName();
            var modContainer = ModList.get().getModContainerById(regName.getNamespace());
            if (!modContainer.isPresent()) {
                LOGGER.warn(
                        "Cannot find mod {} to load built-in datapack, if it's a thingpack you may ignore this warning.",
                        regName.getNamespace());
                return c -> {
                };
            }
            Path packPath = modContainer.get()
                    .getModInfo()
                    .getOwningFile()
                    .getFile()
                    .getFilePath()
                    .resolve("datapacks", regName.getPath());
            return consumer -> {
                try {
                    var loc = new PackLocationInfo(regName.toString().replace(":", "/"), displayName,
                            PackSource.create(PackSource.NO_DECORATION, defaultEnable), Optional.empty());
                    var pack = Pack.readMetaAndCreate(
                            loc, new PathResourcesSupplier(packPath),
                            PackType.SERVER_DATA, new PackSelectionConfig(false, Pack.Position.TOP, false));
                    if (pack != null)
                        consumer.accept(pack);
                    else
                        LOGGER.warn("Fail to load built-in datapack because pack is null");
                } catch (Exception e) {
                    LOGGER.warn("Fail to load built-in datapack " + regName.toString(), e);
                }
            };
        }

        @Override
        public void validate()
        {
        }
    }

    public static final Logger LOGGER = LogManager.getLogger();

    public BuiltInDatapackParser(IEventBus bus) {
        super(GSON, "built_in_datapack");
        bus.addListener(AddPackFindersEvent.class,this::register);
    }

    public void register(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            LOGGER.info(
                    "Started loading built-in datapack things...");
            for (var builder : this.getBuilders()) {
                event.addRepositorySource(builder.build());
            }
            LOGGER.info("Done processing thingpack built-in datapack things.");
        }
    }

    @Override
    public Builder processThing(Identifier key, JsonObject data, Consumer<Builder> builderModification) {
        final Builder builder = new Builder(this, key);
        JParse.begin(data)
                .ifKey("default_enable", val -> val.bool().handle(builder::defaultEnable))
                .ifKey("display_name",
                        val -> ComponentSerialization.CODEC.parse(JsonOps.INSTANCE,val.get()).ifSuccess(builder::displayName).ifError(error->{throw new ThingParseException("Fail tp parse display name:"+error.message());}));
        builderModification.accept(builder);
        return builder;
    }
}
