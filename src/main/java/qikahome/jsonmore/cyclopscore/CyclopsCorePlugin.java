package qikahome.jsonmore.cyclopscore;

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
import net.minecraft.network.chat.Component;

import static qikahome.jsonmore.JsonMore.LOGGER;

import java.util.function.Supplier;

import javax.annotation.Nullable;

public class CyclopsCorePlugin {
    public static final String MOD_ID = "cyclopscore";
    public static RegistryObject<MenuType<ScrollingContainerAdapter>> SCROLLING_CONTAINER_MENU;
    public static Supplier<MenuType<ScrollingContainerAdapter>> supplier = () -> new MenuType<>(
            (IContainerFactory<ScrollingContainerAdapter>) ScrollingContainerAdapter::new, FeatureFlagSet.of());

    public static void load() {
        LOGGER.info("Loading JsonMore CyclopsCorePlugin");
        registerScrollingContainer();
    }

    private static void registerScrollingContainer() {
        ContainerScreenType.register(
                new ResourceLocation("cyclopscore:scrolling"),
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