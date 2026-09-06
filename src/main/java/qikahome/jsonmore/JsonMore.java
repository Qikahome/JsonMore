package qikahome.jsonmore;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;


import dev.gigaherz.jsonthings.things.ThingRegistries;
import dev.gigaherz.jsonthings.things.parsers.ThingResourceManager;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;

import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.event.level.GameRuleChangedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
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
import qikahome.jsonmore.minecraft.gamerule.FlexGameRuleType;
import qikahome.jsonmore.minecraft.gamerule.GameRuleCondition;

// 这里的值应该与 META-INF/neoforge.mods.toml 文件中的条目匹配
@Mod(JsonMore.MODID)
public class JsonMore {
    // 在一个公共位置定义 mod id，以便所有内容都可以引用
    public static final String MODID = "jsonmore";
    // 直接引用一个 slf4j 日志记录器
    public static final Logger LOGGER = LogUtils.getLogger();

    public JsonMore(IEventBus modEventBus) {
        LOGGER.info("JsonMore mod loaded.");
        // 注册 mod 加载的 commonSetup 方法
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        // GameRuleChangedEvent 在 FORGE 事件总线上触发（仅服务端）
        NeoForge.EVENT_BUS.addListener(this::onGameRuleChanged);
        
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        INGREDIENT_TYPES.register(modEventBus);
        CONDITION_CODECS.register(modEventBus);

        var manager = ThingResourceManager.instance();
        manager.registerParser(new GameRuleParser(modEventBus));
        manager.registerParser(new BuiltInDatapackParser(modEventBus));

        onFlexTypesLoad();
    }

    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (var blk : BuiltInRegistries.BLOCK) {
            if (blk instanceof FlexBarrelBlock barrel) {
                event.registerBlock(
                        Capabilities.Item.BLOCK,
                        (level, pos, state, be, side) -> {
                            if (state.getBlock() instanceof FlexBarrelBlock flex) {
                                return VanillaContainerWrapper.of(MultiContainer.of(flex.getContainers(level, pos, state)));
                            }
                            LOGGER.warn("Wrong Block Type for ItemCapability!");
                            return null;
                        },
                        barrel);
            }
            if (blk instanceof StorageConnectorBlock scb) {
                event.registerBlock(
                        Capabilities.Item.BLOCK,
                        (level, pos, state, be, side) -> {
                            if (be instanceof ControllerBlockEntity cbe) {
                                return VanillaContainerWrapper.of(cbe);
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
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister
            .create(Registries.RECIPE_TYPE, MODID);
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.INGREDIENT_TYPES, MODID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister
            .create(NeoForgeRegistries.Keys.CONDITION_CODECS, MODID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapelessRecipe>> SHAPELESS_CONSUMING_RECIPE = RECIPE_SERIALIZERS
            .register("shapeless_consuming", () -> ShapelessConsumingRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedRecipe>> SHAPED_CONSUMING_RECIPE = RECIPE_SERIALIZERS
            .register("shaped_consuming", () -> ShapedConsumingRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ItemApplicationRecipe>> ITEM_APPLICATION_RECIPE = RECIPE_SERIALIZERS
            .register("item_application", () -> ItemApplicationRecipe.Serializer.INSTANCE);
    public static final DeferredHolder<RecipeType<?>, RecipeType<ItemApplicationRecipe>> ITEM_APPLICATION_TYPE = RECIPE_TYPES
            .register("item_application", () -> ItemApplicationRecipe.TYPE);

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


    public void onGameRuleChanged(GameRuleChangedEvent event)
    {
        var rule=event.getGameRule();
        var runner = FlexGameRuleType.events.get(rule);
        if(runner!=null)
            runner.accept(event.getServer(), rule);
    }

    // 您可以使用 EventBusSubscriber 自动注册类中所有带有 @SubscribeEvent 注解的静态方法
    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
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
        Registry.register(ThingRegistries.PROPERTY, "jsonmore:container_part", ContainerPart.PART);
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
