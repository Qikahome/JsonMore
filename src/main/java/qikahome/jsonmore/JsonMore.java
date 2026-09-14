package qikahome.jsonmore;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.gigaherz.jsonthings.things.ThingRegistries;
import io.github.fabricators_of_create.porting_lib.registry.DeferredHolder;
import io.github.fabricators_of_create.porting_lib.registry.DeferredRegister;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import qikahome.jsonmore.autosizedgui.AutoSizedGUIPlugin;
import qikahome.jsonmore.cyclopscore.CyclopsCorePlugin;
import qikahome.jsonmore.lib.ContainerPart;
import qikahome.jsonmore.lib.MultiContainer;
import qikahome.jsonmore.lib.ingredient.ConditionIngredient;
import qikahome.jsonmore.lib.ingredient.CountedIngredient;
import qikahome.jsonmore.lib.ingredient.ItemDisplayOverrideIngredient;
import qikahome.jsonmore.lib.ingredient.KeepInventoryContainerIngredient;
import qikahome.jsonmore.lib.ingredient.NBTCopyIngredient;
import qikahome.jsonmore.lib.ingredient.NotIngredient;
import qikahome.jsonmore.lib.ingredient.RegexIngredient;
import qikahome.jsonmore.lib.ingredient.RemainderOverrideIngredient;
import qikahome.jsonmore.lib.ingredient.ToolDamagingIngredient;
import qikahome.jsonmore.lib.ingredient.TrueIngredient;
import qikahome.jsonmore.lib.recipe.ItemApplicationRecipe;
import qikahome.jsonmore.lib.recipe.ShapedConsumingRecipe;
import qikahome.jsonmore.lib.recipe.ShapelessConsumingRecipe;
import qikahome.jsonmore.minecraft.FlexBarrelBlock;
import qikahome.jsonmore.minecraft.MinecraftPlugin;
import qikahome.jsonmore.minecraft.StorageConnectorBlock.ControllerBlockEntity;
import qikahome.jsonmore.minecraft.gamerule.GameRuleCondition;

/**
 * JsonMore Fabric 主入口（common 部分），对应上游 NeoForge 的 {@code @Mod JsonMore}。
 *
 * <p>Neo → Fabric 的对应关系：
 * <ul>
 *   <li>{@code DeferredRegister#register(IEventBus)} → {@link DeferredRegister#register()}；</li>
 *   <li>{@code IngredientType} 注册表 → {@code CustomIngredientSerializer.register}；</li>
 *   <li>{@code CONDITION_CODECS} → {@code ResourceConditions.register}；</li>
 *   <li>{@code RegisterCapabilitiesEvent} + {@code InvWrapper} →
 *       {@code ItemStorage.SIDED} + {@code InventoryStorage.of}；</li>
 *   <li>{@code PlayerInteractEvent.RightClickBlock} → {@code UseBlockCallback}；</li>
 *   <li>{@code ServerLifecycleHooks} → {@code ServerLifecycleEvents}（见 {@code Utils}）；</li>
 *   <li>{@code RegisterMenuScreensEvent} → {@link JsonMoreClient}（客户端入口必须分开，
 *       否则专用服务端加载 main 入口时会因引用客户端类而失败）。</li>
 * </ul>
 */
public class JsonMore implements ModInitializer {
    // 在一个公共位置定义 mod id，以便所有内容都可以引用
    public static final String MODID = "jsonmore";
    // 直接引用一个 slf4j 日志记录器
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister
            .create(Registries.MENU, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister
            .create(Registries.RECIPE_SERIALIZER, MODID);

    public static final DeferredHolder<RecipeSerializer<?>, ShapelessConsumingRecipe.Serializer> SHAPELESS_CONSUMING_RECIPE = RECIPE_SERIALIZERS
            .register("shapeless_consuming", () -> ShapelessConsumingRecipe.Serializer.INSTANCE);
    public static final DeferredHolder<RecipeSerializer<?>, ShapedConsumingRecipe.Serializer> SHAPED_CONSUMING_RECIPE = RECIPE_SERIALIZERS
            .register("shaped_consuming", () -> ShapedConsumingRecipe.Serializer.INSTANCE);
    public static final DeferredHolder<RecipeSerializer<?>, ItemApplicationRecipe.Serializer> ITEM_APPLICATION_RECIPE = RECIPE_SERIALIZERS
            .register("item_application", () -> ItemApplicationRecipe.Serializer.INSTANCE);

    static {
        NotIngredient.register();
        RegexIngredient.register();
        KeepInventoryContainerIngredient.register();
        TrueIngredient.register();
        ToolDamagingIngredient.register();
        CountedIngredient.register();
        NBTCopyIngredient.register();
        RemainderOverrideIngredient.register();
        ItemDisplayOverrideIngredient.register();
        ConditionIngredient.register();
        GameRuleCondition.register();
        ItemApplicationRecipe.register();

        MinecraftPlugin.BARREL_TILE = BLOCK_ENTITY_TYPES.register("barrel",
                MinecraftPlugin.BARREL_SUPPLIER);
        MinecraftPlugin.STORAGE_CONNECTOR_TILE = BLOCK_ENTITY_TYPES.register("storage_connector",
                MinecraftPlugin.STORAGE_CONNECTOR_SUPPLIER);
        if (FabricLoader.getInstance().isModLoaded("cyclopscore")) {
            CyclopsCorePlugin.SCROLLING_CONTAINER_MENU = MENU_TYPES.register("scrolling_container",
                    CyclopsCorePlugin.supplier);
        }
        if (FabricLoader.getInstance().isModLoaded("autosizedgui")) {
            AutoSizedGUIPlugin.AUTO_SIZED_MENU = MENU_TYPES.register("autosized_menu",
                    AutoSizedGUIPlugin.supplier);
        }
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Start JsonMore common setup.");

        BLOCK_ENTITY_TYPES.register();
        MENU_TYPES.register();
        RECIPE_SERIALIZERS.register();

        // 平台事件注册。
        // 不能用 registerForBlocks + 按类扫描方块数组：JsonThings 的方块要等 thingpack 解析完才注册，
        // 而 Fabric 是并行调用各 mod 入口点的，JsonMore 与 JsonThings 的先后顺序不确定；
        // registerFallback 适用于所有未显式注册的方块，没有这个时序问题。
        ItemStorage.SIDED.registerFallback((world, pos, state, be, side) -> {
            if (state.getBlock() instanceof FlexBarrelBlock flex) {
                return InventoryStorage.of(MultiContainer.of(flex.getContainers(world, pos, state)), side);
            }
            if (be instanceof ControllerBlockEntity cbe) {
                return InventoryStorage.of(cbe, side);
            }
            return null;
        });

        UseBlockCallback.EVENT.register(ItemApplicationRecipe::onRightClickBlock);
        ServerLifecycleEvents.SERVER_STARTED.register(Utils::setCurrentServer);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> Utils.setCurrentServer(null));
    }

    public static void onFlexTypesLoad() {
        Registry.register(ThingRegistries.PROPERTIES, "jsonmore:container_part", ContainerPart.PART);
        // 联动
        if (FabricLoader.getInstance().isModLoaded("cyclopscore")) {
            CyclopsCorePlugin.load();
        }
        if (FabricLoader.getInstance().isModLoaded("autosizedgui")) {
            AutoSizedGUIPlugin.load();
        }
        MinecraftPlugin.load();
    }
}
