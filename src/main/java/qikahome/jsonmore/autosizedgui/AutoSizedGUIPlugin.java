package qikahome.jsonmore.autosizedgui;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.RegistryObject;
import qikahome.jsonmore.lib.ContainerScreenType;
import qikahome.jsonmore.lib.MultiContainer;

import static qikahome.jsonmore.JsonMore.LOGGER;

public class AutoSizedGUIPlugin {
    public static RegistryObject<MenuType<AutoSizedMenu>> AUTO_SIZED_MENU;
    public static Supplier<MenuType<AutoSizedMenu>> supplier = () -> new MenuType<>(
            (IContainerFactory<AutoSizedMenu>) AutoSizedMenu::create,
            FeatureFlagSet.of());

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
