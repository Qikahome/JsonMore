package qikahome.jsonmore.cyclopscore;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import io.github.fabricators_of_create.porting_lib.registry.DeferredHolder;
import io.netty.buffer.Unpooled;
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
    // Fabric 侧开屏附加数据是 codec 序列化的对象，这里把原样写入的 FriendlyByteBuf 装进 byte[] 传输
    public static Supplier<MenuType<ScrollingContainerAdapter>> supplier = () -> new ExtendedScreenHandlerType<ScrollingContainerAdapter, byte[]>(
            (id, inventory, data) -> new ScrollingContainerAdapter(id, inventory,
                    new FriendlyByteBuf(Unpooled.wrappedBuffer(data))),
            ByteBufCodecs.BYTE_ARRAY);

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
