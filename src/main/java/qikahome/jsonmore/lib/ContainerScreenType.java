package qikahome.jsonmore.lib;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.parsers.ThingParseException;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import qikahome.jsonmore.Utils.IntRange;
import qikahome.jsonmore.minecraft.FilteredChestMenu;
import net.minecraft.network.chat.Component;
import static qikahome.jsonmore.Utils.*;

public class ContainerScreenType {
    private static final Map<ResourceLocation, ContainerScreenType> TYPES = new HashMap<>();

    public static final ContainerScreenType VANILLA_CHEST = register(
            ResourceLocation.parse("minecraft:chest"),
            (containers, containerSize) -> {
                MultiContainer container = MultiContainer.of(containers);
                int rows = containerSize / 9;
                return new MenuProvider() {
                    @Override
                    @Nullable
                    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p_39956_) {
                        return switch (rows) {
                            case 1 -> new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, container, 1);
                            case 2 -> new ChestMenu(MenuType.GENERIC_9x2, containerId, inventory, container, 2);
                            case 3 -> ChestMenu.threeRows(containerId, inventory, container);
                            case 4 -> new ChestMenu(MenuType.GENERIC_9x4, containerId, inventory, container, 4);
                            case 5 -> new ChestMenu(MenuType.GENERIC_9x5, containerId, inventory, container, 5);
                            case 6 -> ChestMenu.sixRows(containerId, inventory, container);
                            default -> throw new IllegalArgumentException("Invalid number of slots: " + containerSize);
                        };
                    }

                    @Override
                    public Component getDisplayName() {
                        return container.getDisplayName();
                    }
                };

            },
            true);

    public static final ContainerScreenType CHEST = register(ResourceLocation.parse("jsonmore:chest"),
            (containers, containerSize) -> {
                MultiContainer container = MultiContainer.of(containers);
                return new MenuProvider() {
                    @Override
                    @Nullable
                    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p_39956_) {
                        return FilteredChestMenu.create(containerId, inventory, container, containerSize);
                    }

                    @Override
                    public Component getDisplayName() {
                        return container.getDisplayName();
                    }
                };
            }, true);

    public static final ContainerScreenType NOT_SUPPORTED = register(
            ResourceLocation.parse("jsonmore:not_supported"),
            (containers, containerSize) -> new MenuProvider() {
                @Override
                @Nullable
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    player.sendSystemMessage(
                            Component.translatable("message.jsonmore.container_not_supported", containerSize));
                    return null;
                }

                @Override
                public Component getDisplayName() {
                    return Component.translatable("message.jsonmore.container_not_supported");
                }
            }, true);

    private final ResourceLocation id;
    private final IMenuFactory menuFactory;
    private final boolean available;
    private final TriConsumer<FriendlyByteBuf, List<Container>, Integer> additionalDataWriter;
    /**
     * 产出的菜单是否为扩展菜单（{@code ExtendedScreenHandlerType}）。
     * Fabric 规定原版菜单类型（{@code minecraft:generic_9xN} 等）**不能**用
     * {@code ExtendedScreenHandlerFactory} 打开，否则 {@code ServerPlayer#openMenu} 直接抛
     * {@code IllegalArgumentException}，因此只有 JsonMore/AutoSizedGUI/CyclopsCore 自己注册的
     * 扩展菜单类型才走扩展通道。
     */
    private final boolean extended;

    public ContainerScreenType(ResourceLocation id, IMenuFactory menuFactory, boolean available,
            TriConsumer<FriendlyByteBuf, List<Container>, Integer> additionalDataWriter, boolean extended) {
        this.id = id;
        this.menuFactory = menuFactory;
        this.available = available;
        this.additionalDataWriter = additionalDataWriter;
        this.extended = extended;
    }

    private ContainerScreenType() {
        this(ResourceLocation.parse("builtin:dynamic"), null, true, null, false);
    }

    public ContainerScreenType(ResourceLocation id, IMenuFactory menuFactory, boolean available) {
        this(id, menuFactory, available, (a, b, c) -> {
        }, false);
    }

    public ResourceLocation getId() {
        return id;
    }

    public boolean isAvailable() {
        return available;
    }

    public MenuProvider createMenuProvider(List<Container> containers, int containerSize) {
        MenuProvider delegate = menuFactory.create(containers, containerSize);
        return extended ? toExtended(delegate, containers, containerSize) : delegate;
    }

    /**
     * 上游 Neo 用 {@code ServerPlayer#openMenu(MenuProvider, Consumer<FriendlyByteBuf>)} 附带开屏数据；
     * Fabric 的等价通道是 {@code ExtendedScreenHandlerFactory}：由服务端写出、客户端
     * {@code ExtendedScreenHandlerType#create} 读回。Fabric 的数据必须能过 codec，
     * 因此这里把 {@link #writeAdditionalData} 写入的原始缓冲区整体以 byte[] 传输。
     * <p>
     * 仅适用于扩展菜单类型，见 {@link #extended}。
     */
    protected MenuProvider toExtended(MenuProvider delegate, List<Container> containers, int containerSize) {
        return new ExtendedScreenHandlerFactory<byte[]>() {
            @Override
            @Nullable
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return delegate.createMenu(containerId, inventory, player);
            }

            @Override
            public Component getDisplayName() {
                return delegate.getDisplayName();
            }

            @Override
            public byte[] getScreenOpeningData(ServerPlayer player) {
                FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                writeAdditionalData(buf, containers, containerSize);
                byte[] data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                return data;
            }
        };
    }

    protected void writeAdditionalData(FriendlyByteBuf buf, List<Container> containers, int size) {
        additionalDataWriter.accept(buf, containers, size);
    }

    public static ContainerScreenType register(ResourceLocation id, IMenuFactory menuFactory, boolean available) {
        ContainerScreenType type = new ContainerScreenType(id, menuFactory, available);
        TYPES.put(id, type);
        return type;
    }

    /** 带附加数据写出器的类型：附加数据本身就是扩展屏通道的一部分，故走 {@code ExtendedScreenHandlerFactory}。 */
    public static ContainerScreenType register(ResourceLocation id, IMenuFactory menuFactory, boolean available,
            TriConsumer<FriendlyByteBuf, List<Container>, Integer> additionalDataWriter) {
        ContainerScreenType type = new ContainerScreenType(id, menuFactory, available, additionalDataWriter, true);
        TYPES.put(id, type);
        return type;
    }

    public static ContainerScreenType getOrDefault(@Nullable ResourceLocation id) {
        if (id == null) {
            return CHEST;
        }
        ContainerScreenType type = TYPES.get(id);
        if (type == null || !type.isAvailable()) {
            return CHEST;
        }
        return type;
    }

    @Nullable
    public static ContainerScreenType get(ResourceLocation id) {
        if (id == null) {
            return CHEST;
        }
        return TYPES.get(id);
    }

    @Nullable
    public static ContainerScreenType getVanillaOrDefault(@Nullable ResourceLocation id) {
        ContainerScreenType type = get(id);
        if (type == null || !type.isAvailable()) {
            return VANILLA_CHEST;
        }
        return type;
    }

    @Nullable
    public static ContainerScreenType parse(@Nullable JsonElement json, String defaultId) {
        return parse(json, ResourceLocation.parse(defaultId));
    }

    @Nullable
    public static ContainerScreenType parse(@Nullable JsonElement json, ResourceLocation defaultId) {
        ContainerScreenType type = parse(json);
        if (type == null) {
            return getOrDefault(defaultId);
        }
        return type;
    }

    @Nullable
    public static ContainerScreenType parse(@Nullable JsonElement json) {
        if (json == null) {
            return null;
        }
        if (GsonHelper.isStringValue(json)) {
            ResourceLocation id = ResourceLocation.parse(json.getAsString());
            if (TYPES.containsKey(id))
                return getOrDefault(id);
            throw new ThingParseException("Screen type " + id + " is not available (mod may not be installed)");
        }
        if (json instanceof JsonObject jsonObject) {
            try {
                Set<String> keys = jsonObject.keySet();
                Map<IntRange, ContainerScreenType> types = new LinkedHashMap<>();
                for (String key : keys) {
                    IntRange range = IntRange.parse(key);
                    String val = jsonObject.get(key).getAsString();
                    ResourceLocation id = ResourceLocation.parse(val);
                    if (!TYPES.containsKey(id))
                        throw new ThingParseException(
                                "Screen type " + id + " is not available (mod may not be installed)");
                    types.put(range, getOrDefault(id));
                }
                return new ContainerScreenType() {
                    @Override
                    public MenuProvider createMenuProvider(
                            List<Container> containers, int containerSize) {
                        for (IntRange range : types.keySet()) {
                            if (range.contains(containerSize)) {
                                return types.get(range).createMenuProvider(containers, containerSize);
                            }
                        }
                        throw new IllegalArgumentException("Container size " + containerSize + " not supported");
                    }
                };
            } catch (Exception e) {
                if (e instanceof ThingParseException)
                    throw e;
                throw new ThingParseException("Failed to parse range mapping: " + e.getMessage(), e);
            }
        }
        throw new ThingParseException("Unsupport Container Screen Type Format");
    }

    public static Map<ResourceLocation, ContainerScreenType> getAll() {
        return new HashMap<>(TYPES);
    }

    @FunctionalInterface
    public interface IMenuFactory {
        MenuProvider create(List<Container> containers, int containerSize);
    }
}
