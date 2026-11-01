package qikahome.jsonmore;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.gigaherz.jsonthings.things.ThingRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import qikahome.jsonmore.autosizedgui.AutoSizedGUIPlugin;
import qikahome.jsonmore.lib.ContainerPart;
import qikahome.jsonmore.lib.registration.DeferredRegister;
import qikahome.jsonmore.minecraft.FlexBarrelBlock;
import qikahome.jsonmore.minecraft.MinecraftPlugin;
import qikahome.jsonmore.minecraft.StorageConnectorBlock.ControllerBlockEntity;

/**
 * JsonMore Fabric 主入口（common 部分），对应上游 Forge 1.20.1 的 {@code @Mod JsonMore}。
 *
 * <p>Forge → Fabric 的对应关系：
 * <ul>
 *   <li>{@code @Mod} 构造器 + {@code FMLCommonSetupEvent} → {@link #onInitialize()}；</li>
 *   <li>{@code DeferredRegister#register(IEventBus)} → {@link DeferredRegister#register()}；</li>
 *   <li>自定义原料序列化器 / 配方序列化器 / {@code gamerule} 资源条件由 QikaLib 统一注册
 *       （同时注册 {@code qikalib:*} 与旧 {@code jsonmore:*} id），本模组不再重复注册；</li>
 *   <li>{@code ForgeConfigSpec} → 本项目当前无实际配置项，已移除；</li>
 *   <li>{@code RegisterCapabilitiesEvent}/{@code getCapability} + {@code InvWrapper} →
 *       {@code ItemStorage.SIDED.registerFallback} + {@code InventoryStorage.of}；</li>
 *   <li>{@code PlayerInteractEvent.RightClickBlock} → {@code UseBlockCallback}（由 QikaLib 注册）；</li>
 *   <li>{@code ServerLifecycleHooks} → {@code ServerLifecycleEvents}（见 {@link Utils}）；</li>
 *   <li>{@code FMLClientSetupEvent} → {@link JsonMoreClient}（客户端入口必须分开，否则专用服务端加载
 *       main 入口时会因引用禁用类而失败，见 JsonMoreClient 注释）。</li>
 * </ul>
 *
 * <p>thingpack 相关（自定义 parser、Flex 类型、游戏规则）的注册时机见 {@link JsonMoreThingPlugin}：
 * Fabric 各 mod 的 main 入口点是并行调用的，必须经 JsonThings 的 {@code "jsonthings"} 入口点回调。
 */
public class JsonMore implements ModInitializer {
    // 在一个公共位置定义 mod id，以便所有内容都可以引用
    public static final String MODID = "jsonmore";
    // 直接引用一个 slf4j 日志记录器
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister
            .create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister
            .create(BuiltInRegistries.MENU, MODID);

    static {
        MinecraftPlugin.BARREL_TILE = BLOCK_ENTITY_TYPES.register("barrel",
                MinecraftPlugin.BARREL_SUPPLIER);
        MinecraftPlugin.STORAGE_CONNECTOR_TILE = BLOCK_ENTITY_TYPES.register("storage_connector",
                MinecraftPlugin.STORAGE_CONNECTOR_SUPPLIER);
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

        // 原料类型 / 配方序列化器 / gamerule 条件由 QikaLib 统一注册（含 jsonmore: 旧 id）

        // 平台事件注册。
        // 不能用 registerForBlocks + 按类扫描方块数组：JsonThings 的方块要等 thingpack 解析完才注册，
        // 而 Fabric 是并行调用各 mod 入口点的，JsonMore 与 JsonThings 的先后顺序不确定；
        // registerFallback 适用于所有未显式注册的方块，没有这个时序问题。
        ItemStorage.SIDED.registerFallback((world, pos, state, be, side) -> {
            if (be instanceof FlexBarrelBlock.FlexBarrelBlockEntity barrel)
                return barrel.getInventoryWrapper();
            if (be instanceof ControllerBlockEntity cbe)
                return cbe.getInventoryWrapper();
            return null;
        });

        ServerLifecycleEvents.SERVER_STARTED.register(Utils::setCurrentServer);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> Utils.setCurrentServer(null));
    }

    /** 对应 Forge 构造器里在 thingpack 解析前做的那部分（见 {@link JsonMoreThingPlugin}）。 */
    public static void onFlexTypesLoad() {
        Registry.register(ThingRegistries.PROPERTIES, "jsonmore:container_part", ContainerPart.PART);
        // 联动
        if (FabricLoader.getInstance().isModLoaded("autosizedgui")) {
            AutoSizedGUIPlugin.load();
        }
        MinecraftPlugin.load();
    }
}
