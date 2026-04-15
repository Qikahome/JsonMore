package qikahome.jsonmore.lib;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import qikahome.jsonmore.minecraft.FilteredChestMenu;
import qikahome.jsonmore.minecraft.FlexBarrelBlock.FlexBarrelBlockEntity;

public class ContainerScreenType {
    private static final Map<ResourceLocation, ContainerScreenType> TYPES = new HashMap<>();
    
    public static final ContainerScreenType VANILLA_CHEST = register(
            new ResourceLocation("minecraft:chest"),
            (containerId, inventory, container, containerSize) -> {
                int rows = containerSize / 9;
                return switch (rows) {
                    case 1 -> new ChestMenu(MenuType.GENERIC_9x1,containerId, inventory, container, 1);
                    case 2 -> new ChestMenu(MenuType.GENERIC_9x2,containerId, inventory, container, 2);
                    case 3 -> ChestMenu.threeRows(containerId, inventory, container);
                    case 4 -> new ChestMenu(MenuType.GENERIC_9x4,containerId, inventory, container, 4);
                    case 5 -> new ChestMenu(MenuType.GENERIC_9x5,containerId, inventory, container, 5);
                    case 6 -> ChestMenu.sixRows(containerId, inventory, container);
                    default -> throw new IllegalArgumentException("Invalid number of slots: " + containerSize);
                };
            },
            true
    );
    
    public static final ContainerScreenType CHEST = register(
            new ResourceLocation("jsonmore:chest"),
            (containerId, inventory, container, containerSize) -> {
                return FilteredChestMenu.create(containerId, inventory, container, containerSize);
            },
            true
    );
    
    private final ResourceLocation id;
    private final IMenuFactory menuFactory;
    private final boolean available;
    
    public ContainerScreenType(ResourceLocation id, IMenuFactory menuFactory, boolean available) {
        this.id = id;
        this.menuFactory = menuFactory;
        this.available = available;
    }
    
    public ResourceLocation getId() {
        return id;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, FlexBarrelBlockEntity container, int containerSize) {
        return menuFactory.create(containerId, inventory, container, containerSize);
    }
    
    public static ContainerScreenType register(ResourceLocation id, IMenuFactory menuFactory, boolean available) {
        ContainerScreenType type = new ContainerScreenType(id, menuFactory, available);
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
    
    public static Map<ResourceLocation, ContainerScreenType> getAll() {
        return new HashMap<>(TYPES);
    }
    
    @FunctionalInterface
    public interface IMenuFactory {
        AbstractContainerMenu create(int containerId, Inventory inventory, FlexBarrelBlockEntity container, int containerSize);
    }
}
