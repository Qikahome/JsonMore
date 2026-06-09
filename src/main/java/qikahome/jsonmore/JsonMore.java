package qikahome.jsonmore;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.gigaherz.jsonthings.things.ThingRegistries;
import dev.gigaherz.jsonthings.things.parsers.BlockParser;
import dev.gigaherz.jsonthings.things.parsers.ThingResourceManager;
import dev.gigaherz.jsonthings.things.scripting.ScriptParser;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.Registry;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import qikahome.jsonmore.cyclopscore.CyclopsCorePlugin;
import qikahome.jsonmore.cyclopscore.ScrollingContainerScreen;
import qikahome.jsonmore.lib.ContainerPart;
import qikahome.jsonmore.lib.recipe.ItemApplicationRecipe;
import qikahome.jsonmore.lib.recipe.ShapedConsumingRecipe;
import qikahome.jsonmore.lib.recipe.ShapelessConsumingRecipe;
import qikahome.jsonmore.mantle.MantlePlugin;
import qikahome.jsonmore.minecraft.MinecraftPlugin;
import qikahome.jsonmore.minecraft.gamerule.GameRuleParser;
import qikahome.jsonmore.musbox.AnvilMusBoxPlugin;
import qikahome.jsonmore.tconstruct.TConstructPlugin;
import slimeknights.tconstruct.TConstruct;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.crafting.CraftingHelper;
import qikahome.jsonmore.minecraft.gamerule.GameRuleCondition;

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
        RECIPE_SERIALIZERS.register(modEventBus);

        if (ModList.get().isLoaded("tconstruct")) {
            TConstructPlugin.parser = TConstructPlugin.PARSER_SUPPLIER.apply(modEventBus);
        }

        // 注册我们mod的ForgeConfigSpec，以便Forge可以为我们创建和加载配置文件
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        var manager = ThingResourceManager.instance();
        manager.registerParser(new GameRuleParser(modEventBus));

        onFlexTypesLoad();
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister
            .create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister
            .create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister
            .create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);

    public static final RegistryObject<RecipeSerializer<ShapelessConsumingRecipe>> SHAPELESS_CONSUMING_RECIPE = 
            RECIPE_SERIALIZERS.register("shapeless_consuming", () -> ShapelessConsumingRecipe.Serializer.INSTANCE);
    public static final RegistryObject<RecipeSerializer<ShapedConsumingRecipe>> SHAPED_CONSUMING_RECIPE = 
            RECIPE_SERIALIZERS.register("shaped_consuming", () -> ShapedConsumingRecipe.Serializer.INSTANCE);
    public static final RegistryObject<RecipeSerializer<ItemApplicationRecipe>> ITEM_APPLICATION_RECIPE = 
            RECIPE_SERIALIZERS.register("item_application", () -> ItemApplicationRecipe.Serializer.INSTANCE);

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
            qikahome.jsonmore.lib.ingredient.NotIngredient.register();
            qikahome.jsonmore.lib.ingredient.KeepInventoryContainerIngredient.register();
            qikahome.jsonmore.lib.ingredient.TrueIngredient.register();
            qikahome.jsonmore.lib.ingredient.ToolDamagingIngredient.register();
            qikahome.jsonmore.lib.ingredient.CountedIngredient.register();
            qikahome.jsonmore.lib.ingredient.NBTCopyIngredient.register();
            qikahome.jsonmore.lib.ingredient.RemainderOverrideIngredient.register();
            qikahome.jsonmore.lib.ingredient.ItemDisplayOverrideIngredient.register();
            CraftingHelper.register(GameRuleCondition.Serializer.INSTANCE);
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
                if (ModList.get().isLoaded("cyclopscore")) {
                    net.minecraft.client.gui.screens.MenuScreens.register(
                            CyclopsCorePlugin.SCROLLING_CONTAINER_MENU.get(),
                            ScrollingContainerScreen::new);
                }
            });
            if (ModList.get().isLoaded("mantle")) {
                MantlePlugin.onClientSetup();
            }
        }
    }

    public static void onFlexTypesLoad() {
        Registry.register(ThingRegistries.PROPERTIES, "jsonmore:container_part", ContainerPart.PART);
        // 联动
        ModList modList = ModList.get();
        if (modList.isLoaded("cyclopscore")) {
            CyclopsCorePlugin.load();
        }
        if (modList.isLoaded("mantle")) {
            MantlePlugin.load();
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
