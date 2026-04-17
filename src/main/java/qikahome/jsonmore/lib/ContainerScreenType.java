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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import qikahome.jsonmore.Utils.IntRange;
import qikahome.jsonmore.minecraft.FilteredChestMenu;

import static qikahome.jsonmore.Utils.IntRange;

public class ContainerScreenType {
    private static final Map<ResourceLocation, ContainerScreenType> TYPES = new HashMap<>();

    public static final ContainerScreenType VANILLA_CHEST = register(
            new ResourceLocation("minecraft:chest"),
            (containerId, inventory, containers, containerSize) -> {
                Container inv = MultiContainer.of(containers);
                int rows = containerSize / 9;
                return switch (rows) {
                    case 1 -> new ChestMenu(MenuType.GENERIC_9x1, containerId, inventory, inv, 1);
                    case 2 -> new ChestMenu(MenuType.GENERIC_9x2, containerId, inventory, inv, 2);
                    case 3 -> ChestMenu.threeRows(containerId, inventory, inv);
                    case 4 -> new ChestMenu(MenuType.GENERIC_9x4, containerId, inventory, inv, 4);
                    case 5 -> new ChestMenu(MenuType.GENERIC_9x5, containerId, inventory, inv, 5);
                    case 6 -> ChestMenu.sixRows(containerId, inventory, inv);
                    default -> throw new IllegalArgumentException("Invalid number of slots: " + containerSize);
                };
            },
            true);

    public static final ContainerScreenType CHEST = register(
            new ResourceLocation("jsonmore:chest"),
            (containerId, inventory, containers, containerSize) -> {
                Container inv = MultiContainer.of(containers);
                return FilteredChestMenu.create(containerId, inventory, inv, containerSize);
            },
            true);

    private final ResourceLocation id;
    private final IMenuFactory menuFactory;
    private final boolean available;
    private final BiConsumer<FriendlyByteBuf, List<Container>> additionalDataWriter;

    public ContainerScreenType(ResourceLocation id, IMenuFactory menuFactory, boolean available,
            BiConsumer<FriendlyByteBuf, List<Container>> additionalDataWriter) {
        this.id = id;
        this.menuFactory = menuFactory;
        this.available = available;
        this.additionalDataWriter = additionalDataWriter;
    }

    private ContainerScreenType() {
        this(new ResourceLocation("builtin:dynamic"), null, true, null);
    }

    public ContainerScreenType(ResourceLocation id, IMenuFactory menuFactory, boolean available) {
        this(id, menuFactory, available, (a, b) -> {
        });
    }

    public ResourceLocation getId() {
        return id;
    }

    public boolean isAvailable() {
        return available;
    }

    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, List<Container> containers,
            int containerSize) {
        return menuFactory.create(containerId, inventory, containers, containerSize);
    }

    public void writeAdditionalData(FriendlyByteBuf buf, List<Container> containers) {
        additionalDataWriter.accept(buf, containers);
    }

    public static ContainerScreenType register(ResourceLocation id, IMenuFactory menuFactory, boolean available) {
        ContainerScreenType type = new ContainerScreenType(id, menuFactory, available);
        TYPES.put(id, type);
        return type;
    }

    public static ContainerScreenType register(ResourceLocation id, IMenuFactory menuFactory, boolean available,
            BiConsumer<FriendlyByteBuf, List<Container>> additionalDataWriter) {
        ContainerScreenType type = new ContainerScreenType(id, menuFactory, available, additionalDataWriter);
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
        return parse(json, new ResourceLocation(defaultId));
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
            ResourceLocation id = new ResourceLocation(json.getAsString());
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
                    ResourceLocation id = new ResourceLocation(val);
                    if (!TYPES.containsKey(id))
                        throw new ThingParseException("MenuType not found: " + id);
                    types.put(range, getOrDefault(id));
                }
                return new ContainerScreenType() {
                    @Override
                    public AbstractContainerMenu createMenu(int containerId, Inventory inventory,
                            List<Container> containers,
                            int containerSize) {
                        for (IntRange range : types.keySet()) {
                            if (range.contains(containerSize)) {
                                return types.get(range).createMenu(containerId, inventory, containers, containerSize);
                            }
                        }
                        throw new IllegalArgumentException("Container size " + containerSize + " not supported");
                    }

                    @Override
                    public void writeAdditionalData(FriendlyByteBuf buf, List<Container> containers) {
                        int size = MultiContainer.of(containers).getContainerSize();
                        for (IntRange range : types.keySet()) {
                            if (range.contains(size)) {
                                types.get(range).writeAdditionalData(buf, containers);
                                return;
                            }
                        }
                        throw new IllegalArgumentException("Container size " + size + " not supported");
                    }
                };
            } catch (Exception e) {
                if (e instanceof ThingParseException)
                    throw e;
                throw new ThingParseException("Failed to parse range mapping: ", e);
            }
        }
        throw new ThingParseException("Unsupport Container Screen Type Format");
    }

    public static Map<ResourceLocation, ContainerScreenType> getAll() {
        return new HashMap<>(TYPES);
    }

    @FunctionalInterface
    public interface IMenuFactory {
        AbstractContainerMenu create(int containerId, Inventory inventory, List<Container> containers,
                int containerSize);
    }
}
