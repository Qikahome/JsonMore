package qikahome.jsonmore.cyclopscore;

import java.util.function.Supplier;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import qikahome.jsonmore.lib.ContainerScreenType;
import qikahome.jsonmore.lib.MultiContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import static qikahome.jsonmore.JsonMore.LOGGER;

import javax.annotation.Nullable;

public class CyclopsCorePlugin {
    public static final String MOD_ID = "cyclopscore";
    public static DeferredHolder<MenuType<?>, MenuType<ScrollingContainerAdapter>> SCROLLING_CONTAINER_MENU;
    public static Supplier<MenuType<ScrollingContainerAdapter>> supplier = () -> IMenuTypeExtension
            .create(ScrollingContainerAdapter::new);

    public static void load() {
        LOGGER.info("Loading JsonMore CyclopsCorePlugin");
        registerScrollingContainer();
    }

    private static void registerScrollingContainer() {
        ContainerScreenType.register(
                ResourceLocation.parse("cyclopscore:scrolling"),
                (containers, containerSize) -> {
                    var container = MultiContainer.of(containers);
                    return new MenuProvider() {
                        @Override
                        @Nullable
                        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p_39956_) {
                            return new ScrollingContainerAdapter(containerId,
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
