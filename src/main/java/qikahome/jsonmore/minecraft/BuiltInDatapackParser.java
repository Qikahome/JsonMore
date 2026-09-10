package qikahome.jsonmore.minecraft;

import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.builders.BaseBuilder;
import dev.gigaherz.jsonthings.things.parsers.ThingParser;
import dev.gigaherz.jsonthings.util.parse.JParse;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.RepositorySource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

public class BuiltInDatapackParser extends ThingParser<BuiltInDatapackParser.Builder> {

    public static class Builder extends BaseBuilder<RepositorySource, Builder> {

        protected Builder(ThingParser<Builder> ownerParser, ResourceLocation registryName) {
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
            var modContainer = FabricLoader.getInstance().getModContainer(regName.getNamespace());
            if (modContainer.isEmpty()) {
                LOGGER.warn(
                        "Cannot find mod {} to load built-in datapack, if it's a thingpack you may ignore this warning.",
                        regName.getNamespace());
            } else {
                // 上游 Neo 版本从 <mod>/datapacks/<name> 读 pack.mcmeta 后注入 SERVER_DATA 包库；
                // Fabric 等价通道是 ResourceManagerHelper 的内置包登记（同时覆盖资源包与数据包）。
                ResourcePackActivationType activationType = defaultEnable
                        ? ResourcePackActivationType.DEFAULT_ENABLED
                        : ResourcePackActivationType.NORMAL;
                if (!ResourceManagerHelper.registerBuiltinResourcePack(regName, modContainer.get(), displayName,
                        activationType)) {
                    LOGGER.warn("Fail to load built-in datapack because pack is null");
                }
            }
            // Fabric 侧不需要 RepositorySource，保留空实现仅为满足 Builder 的构建类型。
            return consumer -> {
            };
        }
    }

    public static final Logger LOGGER = LogManager.getLogger();

    public BuiltInDatapackParser() {
        super(GSON, "built_in_datapack");
    }

    /**
     * 上游 Neo 版在 {@code AddPackFindersEvent} 里注入包库；Fabric 无该事件，
     * 由 {@code JsonMore#onInitialize} 在 thingpack 解析完成后显式调用。
     */
    public void registerValues() {
        LOGGER.info("Started loading built-in datapack things...");
        for (var builder : this.getBuilders()) {
            builder.build();
        }
        LOGGER.info("Done processing thingpack built-in datapack things.");
    }

    @Override
    public Builder processThing(ResourceLocation key, JsonObject data, Consumer<Builder> builderModification) {
        final Builder builder = new Builder(this, key);
        JParse.begin(data)
                .ifKey("default_enable", val -> val.bool().handle(builder::defaultEnable))
                .ifKey("display_name",
                        val -> builder.displayName(Component.Serializer.fromJson(val.get(), RegistryAccess.EMPTY)));
        builderModification.accept(builder);
        return builder;
    }
}
