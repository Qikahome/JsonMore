package qikahome.jsonmore.cyclopscore;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.RegistryObject;
import qikahome.jsonmore.lib.ContainerScreenType;
import static qikahome.jsonmore.JsonMore.LOGGER;

import java.util.function.Supplier;

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
                (containerId, inventory, container, containerSize) -> new ScrollingContainerAdapter(containerId,
                        inventory, container),
                true);
    }
}