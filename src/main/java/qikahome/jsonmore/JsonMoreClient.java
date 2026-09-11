package qikahome.jsonmore;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.MenuScreens;
import qikahome.autosizedgui.screen.AutoSizedContainerScreen;
import qikahome.jsonmore.autosizedgui.AutoSizedGUIPlugin;
import qikahome.jsonmore.autosizedgui.AutoSizedMenu;

/**
 * JsonMore Fabric 客户端入口（对应 Forge 的 {@code FMLClientSetupEvent} + {@code MenuScreens.register}）。
 *
 * <p>必须与 {@link JsonMore} 拆开：{@code main} 入口点在专用服务端也要加载，而本类引用了
 * {@code net.minecraft.client.*}，混在同一个类里会导致服务端启动时报
 * {@code Cannot load class net.minecraft.client.gui.screens.MenuScreens in environment type SERVER}。
 */
public class JsonMoreClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("autosizedgui")) {
            MenuScreens.<AutoSizedMenu, AutoSizedContainerScreen<AutoSizedMenu>>register(
                    AutoSizedGUIPlugin.AUTO_SIZED_MENU.get(),
                    AutoSizedContainerScreen<AutoSizedMenu>::new);
        }
    }
}
