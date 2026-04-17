package qikahome.jsonmore;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.gigaherz.jsonthings.things.ThingRegistries;
import dev.gigaherz.jsonthings.things.parsers.ThingResourceManager;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.resource.ResourcePackLoader;
import qikahome.jsonmore.cyclopscore.CyclopsCorePlugin;
import qikahome.jsonmore.cyclopscore.ScrollingContainerScreen;
import qikahome.jsonmore.lib.ContainerPart;
import qikahome.jsonmore.minecraft.MinecraftPlugin;
import qikahome.jsonmore.musbox.AnvilMusBoxPlugin;
import qikahome.jsonmore.tconstruct.TConstructPlugin;
import slimeknights.tconstruct.TConstruct;

// 这里的值应该与META-INF/mods.toml文件中的条目匹配
@Mod(JsonMore.MODID)
@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)
public class JsonMore {
    // 在一个公共位置定义mod id，以便所有内容都可以引用
    public static final String MODID = "jsonmore";
    // 直接引用一个slf4j日志记录器
    public static final Logger LOGGER = LogUtils.getLogger();

    public JsonMore(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // 注册mod加载的commonSetup方法
        modEventBus.addListener(this::commonSetup);

        // 为服务器和其他我们感兴趣的游戏事件注册自己
        MinecraftForge.EVENT_BUS.register(this);

        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);

        if (ModList.get().isLoaded("tconstruct")) {
            TConstructPlugin.parser = TConstructPlugin.PARSER_SUPPLIER.apply(modEventBus);
        }

        // 注册我们mod的ForgeConfigSpec，以便Forge可以为我们创建和加载配置文件
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        onFlexTypesLoad();
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister
            .create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister
            .create(ForgeRegistries.MENU_TYPES, MODID);

    static {
        MinecraftPlugin.BARREL_TILE = BLOCK_ENTITY_TYPES.register("barrel",
                MinecraftPlugin.BARREL_SUPPLIER);
        if (ModList.get().isLoaded("tconstruct")) {
            LOGGER.info("Registering JsonMore TConstructPlugin Block Entities");
            TConstructPlugin.TINKER_CHEST_TILE = BLOCK_ENTITY_TYPES.register("tinker_chest",
                    TConstructPlugin.TINKER_CHEST_SUPPLIER);
        }
        if (ModList.get().isLoaded("cyclopscore")) {
            CyclopsCorePlugin.SCROLLING_CONTAINER_MENU = MENU_TYPES.register("scrolling_container",
                    CyclopsCorePlugin.supplier);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Start JsonMore common setup.");
        event.enqueueWork(() -> {
            qikahome.jsonmore.lib.NotIngredient.register();
            qikahome.jsonmore.lib.KeepInventoryContainerIngredient.register();
            qikahome.jsonmore.lib.TrueIngredient.register();
        });
        if (ModList.get().isLoaded("tconstruct")) {
            TConstructPlugin.onCommonSetup(event);
        }
    }

    // 您可以使用SubscribeEvent，让事件总线发现要调用的方法
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // 当服务器启动时做一些事情
        // LOGGER.info("HELLO from server starting");
    }

    // 您可以使用EventBusSubscriber自动注册类中所有带有@SubscribeEvent注解的静态方法
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                Registry.register(ThingRegistries.PROPERTIES, "jsonmore:container_part", ContainerPart.PART);
                if (ModList.get().isLoaded("cyclopscore")) {
                    net.minecraft.client.gui.screens.MenuScreens.register(
                            CyclopsCorePlugin.SCROLLING_CONTAINER_MENU.get(),
                            ScrollingContainerScreen::new);
                }
            });
        }
    }

    public static void onFlexTypesLoad() {
        // 联动
        ModList modList = ModList.get();
        if (modList.isLoaded("cyclopscore")) {
            CyclopsCorePlugin.load();
        }
        if (modList.isLoaded("tconstruct")) {
            TConstructPlugin.load();
        }
        if (ModList.get().isLoaded("anvil_musbox")) {
            AnvilMusBoxPlugin.load();
        }
        // if (modList.isLoaded("minecraft")) {
        MinecraftPlugin.load();
        // }
    }
}
