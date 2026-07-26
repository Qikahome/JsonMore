package qikahome.jsonmore;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;

import dev.gigaherz.jsonthings.things.ThingRegistries;
import dev.gigaherz.jsonthings.things.parsers.ThingResourceManager;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.crafting.CraftingHelper;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import qikahome.jsonmore.create.CreatePlugin;
import qikahome.jsonmore.cyclopscore.CyclopsCorePlugin;
import qikahome.jsonmore.cyclopscore.ScrollingContainerScreen;
import qikahome.jsonmore.lib.ContainerPart;
import qikahome.jsonmore.lib.recipe.ItemApplicationRecipe;
import qikahome.jsonmore.lib.recipe.ShapedConsumingRecipe;
import qikahome.jsonmore.lib.recipe.ShapelessConsumingRecipe;
import qikahome.jsonmore.minecraft.gamerule.GameRuleParser;
import qikahome.jsonmore.minecraft.BuiltInDatapackParser;
import qikahome.jsonmore.lib.MultiContainer;
import qikahome.jsonmore.lib.ingredient.ConditionIngredient;
import qikahome.jsonmore.lib.ingredient.CountedIngredient;
import qikahome.jsonmore.lib.ingredient.ItemDisplayOverrideIngredient;
import qikahome.jsonmore.lib.ingredient.KeepInventoryContainerIngredient;
import qikahome.jsonmore.lib.ingredient.NBTCopyIngredient;
import qikahome.jsonmore.lib.ingredient.NotIngredient;
import qikahome.jsonmore.lib.ingredient.RemainderOverrideIngredient;
import qikahome.jsonmore.lib.ingredient.ToolDamagingIngredient;
import qikahome.jsonmore.lib.ingredient.TrueIngredient;
import qikahome.jsonmore.minecraft.FlexBarrelBlock;
import qikahome.jsonmore.minecraft.MinecraftPlugin;
import qikahome.jsonmore.minecraft.StorageConnectorBlock;
import qikahome.jsonmore.minecraft.StorageConnectorBlock.ControllerBlockEntity;
import qikahome.jsonmore.musbox.AnvilMusBoxPlugin;
import qikahome.autosizedgui.screen.AutoSizedContainerScreen;
import qikahome.jsonmore.autosizedgui.AutoSizedGUIPlugin;
import qikahome.jsonmore.autosizedgui.AutoSizedMenu;
import qikahome.jsonmore.minecraft.gamerule.GameRuleCondition;

// 这里的值应该与 META-INF/neoforge.mods.toml 文件中的条目匹配
@Mod(JsonMore.MODID)
public class JsonMore {
    // 在一个公共位置定义 mod id，以便所有内容都可以引用
    public static final String MODID = "jsonmore";
    // 直接引用一个 slf4j 日志记录器
    public static final Logger LOGGER = LogUtils.getLogger();

    public JsonMore(IEventBus modEventBus) {
        // 注册 mod 加载的 commonSetup 方法
        modEventBus.addListener(this::commonSetup);
                modEventBus.addListener(this::registerCapabilities);
        // 为服务器和其他我们感兴趣的游戏事件注册自己
        NeoForge.EVENT_BUS.register(this);

        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        INGREDIENT_TYPES.register(modEventBus);

        var manager = ThingResourceManager.instance();
        manager.registerParser(new GameRuleParser(modEventBus));
        manager.registerParser(new BuiltInDatapackParser(modEventBus));

        onFlexTypesLoad();
    }

    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (var blk : BuiltInRegistries.BLOCK) {
            if (blk instanceof FlexBarrelBlock barrel) {
                event.registerBlock(
                        Capabilities.ItemHandler.BLOCK,
                        (level, pos, state, be, side) -> {
                            if (state.getBlock() instanceof FlexBarrelBlock flex) {
                                return new InvWrapper(MultiContainer.of(flex.getContainers(level, pos, state)));
                            }
                            LOGGER.warn("Wrong Block Type for ItemCapability!");
                            return null;
                        },
                        barrel);
            }
            if (blk instanceof StorageConnectorBlock scb) {
                event.registerBlock(
                        Capabilities.ItemHandler.BLOCK,
                        (level, pos, state, be, side) -> {
                            if (be instanceof ControllerBlockEntity cbe) {
                                return new InvWrapper(cbe);
                            }
                            return null;
                        },
                        scb);
            }
        }
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister
            .create(Registries.MENU, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister
            .create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.INGREDIENT_TYPES, MODID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister
            .create(NeoForgeRegistries.Keys.CONDITION_CODECS, MODID);
    public static final DeferredHolder<RecipeSerializer<?>, ShapelessConsumingRecipe.Serializer> SHAPELESS_CONSUMING_RECIPE = RECIPE_SERIALIZERS
            .register("shapeless_consuming", () -> ShapelessConsumingRecipe.Serializer.INSTANCE);
    public static final DeferredHolder<RecipeSerializer<?>, ShapedConsumingRecipe.Serializer> SHAPED_CONSUMING_RECIPE = RECIPE_SERIALIZERS
            .register("shaped_consuming", () -> ShapedConsumingRecipe.Serializer.INSTANCE);
    public static final DeferredHolder<RecipeSerializer<?>, ItemApplicationRecipe.Serializer> ITEM_APPLICATION_RECIPE = RECIPE_SERIALIZERS
            .register("item_application", () -> ItemApplicationRecipe.Serializer.INSTANCE);

    static {
        NotIngredient.register();
        KeepInventoryContainerIngredient.register();
        TrueIngredient.register();
        ToolDamagingIngredient.register();
        CountedIngredient.register();
        NBTCopyIngredient.register();
        RemainderOverrideIngredient.register();
        ItemDisplayOverrideIngredient.register();
        ConditionIngredient.register();
        GameRuleCondition.register();

        MinecraftPlugin.BARREL_TILE = BLOCK_ENTITY_TYPES.register("barrel",
                MinecraftPlugin.BARREL_SUPPLIER);
        MinecraftPlugin.STORAGE_CONNECTOR_TILE = BLOCK_ENTITY_TYPES.register("storage_connector",
                MinecraftPlugin.STORAGE_CONNECTOR_SUPPLIER);
        if (ModList.get().isLoaded("cyclopscore")) {
            CyclopsCorePlugin.SCROLLING_CONTAINER_MENU = MENU_TYPES.register("scrolling_container",
                    CyclopsCorePlugin.supplier);
        }
        if (ModList.get().isLoaded("autosizedgui")) {
            AutoSizedGUIPlugin.AUTO_SIZED_MENU = MENU_TYPES.register("autosized_menu",
                    AutoSizedGUIPlugin.supplier);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Start JsonMore common setup.");
        event.enqueueWork(() -> {
        });
    }

    // 您可以使用 SubscribeEvent，让事件总线发现要调用的方法
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // 当服务器启动时做一些事情
        // LOGGER.info("HELLO from server starting");
    }

    // 您可以使用 EventBusSubscriber 自动注册类中所有带有 @SubscribeEvent 注解的静态方法
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterMenuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
            if (ModList.get().isLoaded("cyclopscore")) {
                event.register(CyclopsCorePlugin.SCROLLING_CONTAINER_MENU.get(),
                        ScrollingContainerScreen::new);
            }
            if (ModList.get().isLoaded("autosizedgui")) {
                event.<AutoSizedMenu, AutoSizedContainerScreen<AutoSizedMenu>>register(
                        AutoSizedGUIPlugin.AUTO_SIZED_MENU.get(),
                        AutoSizedContainerScreen<AutoSizedMenu>::new);
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
        if (ModList.get().isLoaded("anvil_musbox")) {
            AnvilMusBoxPlugin.load();
        }
        if (ModList.get().isLoaded("create")) {
            CreatePlugin.load();
        }
        if (ModList.get().isLoaded("autosizedgui")) {
            AutoSizedGUIPlugin.load();
        }
        // if (modList.isLoaded("minecraft")) {
        MinecraftPlugin.load();
        // }
    }
}
