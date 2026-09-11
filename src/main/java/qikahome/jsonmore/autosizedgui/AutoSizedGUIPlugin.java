package qikahome.jsonmore.autosizedgui;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import qikahome.jsonmore.lib.ContainerScreenType;
import qikahome.jsonmore.lib.MultiContainer;
import qikahome.jsonmore.lib.registration.DeferredHolder;

import static qikahome.jsonmore.JsonMore.LOGGER;

/**
 * AutoSizedGUI 联动（对应 1.21.1 Fabric 侧同名类）。
 *
 * <p>与 1.21.1 的差异只有菜单类型：1.20.1 的 {@code ExtendedScreenHandlerType} 是非泛型的，
 * 工厂直接拿到开屏数据缓冲区（{@code FriendlyByteBuf}），无需 codec。
 */
public class AutoSizedGUIPlugin {
    public static DeferredHolder<MenuType<?>, MenuType<AutoSizedMenu>> AUTO_SIZED_MENU;
    public static Supplier<MenuType<AutoSizedMenu>> supplier = () -> new ExtendedScreenHandlerType<>(
            AutoSizedMenu::create);

    public static void load() {
        LOGGER.info("Loading JsonMore AutoSizedGUIPlugin");
        registerAutoSizedContainer();
    }

    private static void registerAutoSizedContainer() {
        ContainerScreenType.register(
                new ResourceLocation("autosizedgui:auto"),
                (containers, containerSize) -> {
                    var container = MultiContainer.of(containers);
                    return new MenuProvider() {
                        @Override
                        @Nullable
                        public AbstractContainerMenu createMenu(int containerId,
                                Inventory inventory, Player p_39956_) {
                            return AutoSizedMenu.create(containerId,
                                    inventory, container);
                        }

                        @Override
                        public Component getDisplayName() {
                            return container.getDisplayName();
                        }
                    };
                }, true,
                (buf, containers, size) -> {
                    buf.writeVarInt(size);
                });
    }
}
