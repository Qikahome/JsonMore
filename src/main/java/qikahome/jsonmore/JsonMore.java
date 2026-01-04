package qikahome.jsonmore;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.gigaherz.jsonthings.things.serializers.FlexBlockType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent.RegisterStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import qikahome.jsonmore.tconstruct.FlexTinkerChestBlockEntity;
import qikahome.jsonmore.tconstruct.TConstructPlugin;
import slimeknights.mantle.registration.deferred.BlockEntityTypeDeferredRegister;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;

// 这里的值应该与META-INF/mods.toml文件中的条目匹配
@Mod(JsonMore.MODID)
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

        // 注册我们mod的ForgeConfigSpec，以便Forge可以为我们创建和加载配置文件
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister
            .create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    static{
        if (ModList.get().isLoaded("tconstruct")) {
            LOGGER.info("Registering JsonMore TConstructPlugin Block Entities");
            TConstructPlugin.TINKER_CHEST_TILE = BLOCK_ENTITY_TYPES.register("tinker_chest",
                    TConstructPlugin.TINKER_CHEST_SUPPLIER);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
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
            // 一些客户端设置代码
            // LOGGER.info("HELLO FROM CLIENT SETUP");
            // LOGGER.info("MINECRAFT NAME >> {}",
            // Minecraft.getInstance().getUser().getName());
        }
    }

    public static void onFlexTypesLoad() {
        // 联动
        ModList modList = ModList.get();
        if (modList.isLoaded("tconstruct")) {
            TConstructPlugin.load();
        }
    }
}
