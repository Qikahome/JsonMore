package qikahome.jsonmore;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.MenuScreens;
import qikahome.autosizedgui.screen.AutoSizedContainerScreen;
import qikahome.jsonmore.autosizedgui.AutoSizedGUIPlugin;
import qikahome.jsonmore.autosizedgui.AutoSizedMenu;
import qikahome.jsonmore.cyclopscore.CyclopsCorePlugin;
import qikahome.jsonmore.cyclopscore.ScrollingContainerScreen;

/**
 * JsonMore Fabric 客户端入口（对应 NeoForge 的 {@code RegisterMenuScreensEvent} / {@code FMLClientSetupEvent}）。
 *
 * <p>必须与 {@link JsonMore} 拆开：{@code main} 入口点在专用服务端也要加载，而本类引用了
 * {@code net.minecraft.client.*}，混在同一个类里会导致服务端启动时报
 * {@code Cannot load class net.minecraft.client.gui.screens.MenuScreens in environment type SERVER}。
 */
public class JsonMoreClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Fabric API 的 ScreenRegistry 在 fabric-screen-handler-api 1.3.88 已移除，
        // 改为经 AccessWidener 开放的 MenuScreens.register 注册。
        if (FabricLoader.getInstance().isModLoaded("cyclopscore")) {
            MenuScreens.register(CyclopsCorePlugin.SCROLLING_CONTAINER_MENU.get(),
                    ScrollingContainerScreen::new);
        }
        if (FabricLoader.getInstance().isModLoaded("autosizedgui")) {
            MenuScreens.<AutoSizedMenu, AutoSizedContainerScreen<AutoSizedMenu>>register(
                    AutoSizedGUIPlugin.AUTO_SIZED_MENU.get(),
                    AutoSizedContainerScreen<AutoSizedMenu>::new);
        }
    }
}
