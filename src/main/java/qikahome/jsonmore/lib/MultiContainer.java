package qikahome.jsonmore.lib;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MultiContainer implements Container, IFlexContainer {
    private final List<Container> subContainers;

    private MultiContainer(List<Container> subContainers) {

        this.subContainers = new ArrayList<>();
        for (Container subContainer : subContainers) {
            if (subContainer instanceof MultiContainer multiContainer) {
                this.subContainers.addAll(multiContainer.subContainers);
            } else {
                this.subContainers.add(subContainer);
            }
        }
    }

    public static Container of(Container... subContainers) {
        return of(List.of(subContainers));
    }

    public static Container of(List<Container> subContainers) {
        if (subContainers.isEmpty()) {
            return new SimpleContainer(new ItemStack[0]);
        }
        if (subContainers.size() == 1) {
            return subContainers.get(0);
        }
        if (subContainers.size() == 2) {
            return new CompoundContainer(subContainers.get(0), subContainers.get(1));
        }
        return new MultiContainer(subContainers);
    }

    public int getContainerSize() {
        return this.subContainers.stream().mapToInt(Container::getContainerSize).sum();
    }

    public boolean isEmpty() {
        return this.subContainers.stream().allMatch(Container::isEmpty);
    }

    public boolean contains(Container container) {
        return this.subContainers.stream().anyMatch(subContainer -> subContainer == container
                || subContainer instanceof CompoundContainer cc && cc.contains(container)
                || subContainer instanceof IFlexContainer flexContainer && flexContainer.isThisContainer(container));
    }

    public static boolean contains(Container bigger, Container smaller) {
        if (bigger instanceof MultiContainer multiContainer) {
            return multiContainer.contains(smaller);
        }
        if (bigger instanceof CompoundContainer compoundContainer) {
            return compoundContainer.contains(smaller);
        }
        return bigger == smaller;
    }

    public ItemStack getItem(int index) {
        int left = index;
        for (Container subContainer : this.subContainers) {
            if (left >= subContainer.getContainerSize()) {
                left -= subContainer.getContainerSize();
            } else {
                return subContainer.getItem(left);
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack removeItem(int index, int count) {
        int left = index;
        for (Container subContainer : this.subContainers) {
            if (left >= subContainer.getContainerSize()) {
                left -= subContainer.getContainerSize();
            } else {
                return subContainer.removeItem(left, count);
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack removeItemNoUpdate(int index) {
        int left = index;
        for (Container subContainer : this.subContainers) {
            if (left >= subContainer.getContainerSize()) {
                left -= subContainer.getContainerSize();
            } else {
                return subContainer.removeItemNoUpdate(left);
            }
        }
        return ItemStack.EMPTY;
    }

    public void setItem(int index, ItemStack stack) {
        int left = index;
        for (Container subContainer : this.subContainers) {
            if (left >= subContainer.getContainerSize()) {
                left -= subContainer.getContainerSize();
            } else {
                subContainer.setItem(left, stack);
                return;
            }
        }

    }

    public int getMaxStackSize() {
        return this.subContainers.stream().mapToInt(Container::getMaxStackSize).min().orElse(0);
    }

    public void setChanged() {
        this.subContainers.forEach(Container::setChanged);
    }

    public boolean stillValid(Player player) {
        return this.subContainers.stream().allMatch(container -> container.stillValid(player));
    }

    public void startOpen(Player player) {
        this.subContainers.forEach(container -> container.startOpen(player));
    }

    public void stopOpen(Player player) {
        this.subContainers.forEach(container -> container.stopOpen(player));
    }

    public boolean canPlaceItem(int index, ItemStack stack) {
        int left = index;
        for (Container subContainer : this.subContainers) {
            if (left >= subContainer.getContainerSize()) {
                left -= subContainer.getContainerSize();
            } else {
                return subContainer.canPlaceItem(left, stack);
            }
        }
        return false;
    }

    public void clearContent() {
        this.subContainers.forEach(Container::clearContent);
    }

    @Override
    public boolean isThisContainer(Container container) {
        return this.contains(container);
    }
}
