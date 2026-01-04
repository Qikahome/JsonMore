package qikahome.jsonmore.tconstruct;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import dev.gigaherz.jsonthings.things.UseFinishMode;
import dev.gigaherz.jsonthings.things.builders.ItemBuilder;
import dev.gigaherz.jsonthings.things.events.FlexEventContext;
import dev.gigaherz.jsonthings.things.events.FlexEventHandler;
import dev.gigaherz.jsonthings.things.events.FlexEventResult;
import dev.gigaherz.jsonthings.things.events.IEventRunner;
import dev.gigaherz.jsonthings.util.Utils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import slimeknights.tconstruct.smeltery.item.CopperCanItem;

public class FlexCanItem extends CopperCanItem
        implements IEventRunner {
    public FlexCanItem(Properties properties, ItemBuilder builder, int capacity) {
        super(properties);
        this.capacity = capacity;
        this.useAction = builder.getUseAnim();
        this.useTime = builder.getUseTime();
        this.useFinishMode = builder.getUseFinishMode();
        this.attributeModifiers = builder.getAttributeModifiers();
        this.lore = builder.getLore();
        this.toolActions = builder.getToolActions();
        this.burnTime = Utils.orElse(builder.getBurnDuration(), -1);
        initializeFlex();
    }

    private final int capacity;

    public class FlexCanFluidHandler implements IFluidHandlerItem, ICapabilityProvider {
        private final LazyOptional<IFluidHandlerItem> holder = LazyOptional.of(() -> this);

        public FlexCanFluidHandler(ItemStack container, int capacity) {
            this.container = container;
            this.capacity = capacity;
        }

        private final ItemStack container;
        private final int capacity;

        public ItemStack getContainer() {
            return container;
        }

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return ForgeCapabilities.FLUID_HANDLER_ITEM.orEmpty(cap, holder);
        }

        /* Tank properties */

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        /** Gets the stack size sensitive capacity of the container */
        private int getCapacity() {
            // scale up by the stack size to prevent dupes with people trying to fill a
            // stack of containers
            return capacity * container.getCount();
        }

        @Override
        public int getTankCapacity(int tank) {
            return getCapacity();
        }

        /** Gets the contained fluid */
        private Fluid getFluid() {
            return CopperCanItem.getFluid(container);
        }

        /** Gets the contained fluid */
        @Nullable
        private CompoundTag getFluidTag() {
            return CopperCanItem.getFluidTag(container);
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank) {
            Fluid fluid = getFluid();
            if (fluid == Fluids.EMPTY) {
                return FluidStack.EMPTY;
            }
            return new FluidStack(getFluid(), getCapacity(), getFluidTag());
        }

        /* Interaction */

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // must not be filled, must have enough
            int capacity = getCapacity();
            if (getFluid() != Fluids.EMPTY || resource.getAmount() < capacity) {
                return 0;
            }
            // update fluid and return
            if (action.execute()) {
                // this is not size sensitive so no need to shrink resource for stack size
                CopperCanItem.setFluid(container, resource);
            }
            return capacity;
        }

        @Nonnull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            // must be draining at least an ingot
            int capacity = getCapacity();
            if (resource.isEmpty() || resource.getAmount() < capacity) {
                return FluidStack.EMPTY;
            }
            // must have a fluid, must match what they are draining
            Fluid fluid = getFluid();
            if (fluid == Fluids.EMPTY || fluid != resource.getFluid()) {
                return FluidStack.EMPTY;
            }
            // make sure NBT matches the requested NBT
            FluidStack output = new FluidStack(fluid, capacity, getFluidTag());
            if (!FluidStack.areFluidStackTagsEqual(resource, output)) {
                return FluidStack.EMPTY;
            }
            // output 1 ingot times stack size
            if (action.execute()) {
                CopperCanItem.setFluid(container, FluidStack.EMPTY);
            }
            return output;
        }

        @Nonnull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            // must be draining at least an ingot
            int capacity = getCapacity();
            if (maxDrain < capacity) {
                return FluidStack.EMPTY;
            }
            // must have a fluid
            Fluid fluid = getFluid();
            if (fluid == Fluids.EMPTY) {
                return FluidStack.EMPTY;
            }
            // output 1 ingot
            FluidStack output = new FluidStack(fluid, capacity, getFluidTag());
            if (action.execute()) {
                CopperCanItem.setFluid(container, FluidStack.EMPTY);
            }
            return output;
        }

    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FlexCanFluidHandler(stack, capacity);
    }

    // region IFlexItem
    private final Map<String, FlexEventHandler> eventHandlers = Maps.newHashMap();

    private final Map<EquipmentSlot, Multimap<Attribute, AttributeModifier>> attributeModifiers;
    private final UseAnim useAction;
    private final Integer useTime;
    private final UseFinishMode useFinishMode;
    private final List<MutableComponent> lore;
    private final Set<ToolAction> toolActions;
    private final int burnTime;

    private void initializeFlex() {
        for (EquipmentSlot slot1 : EquipmentSlot.values()) {
            attributeModifiers.computeIfAbsent(slot1, key -> ArrayListMultimap.create())
                    .putAll(super.getAttributeModifiers(slot1, ItemStack.EMPTY));
        }
    }

    @Override
    public void addEventHandler(String eventName, FlexEventHandler eventHandler) {
        eventHandlers.put(eventName, eventHandler);
    }

    @Override
    public FlexEventHandler getEventHandler(String eventName) {
        return eventHandlers.get(eventName);
    }
    // endregion

    // region Item

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack heldItem = playerIn.getItemInHand(handIn);
        if (useTime != null && useTime > 0)
            return runEvent("begin_using", FlexEventContext.of(worldIn, playerIn, handIn, heldItem), () -> {
                playerIn.startUsingItem(handIn);
                return FlexEventResult.consume(heldItem);
            }).holder();
        else
            return runEvent("use_on_air", FlexEventContext.of(worldIn, playerIn, handIn, heldItem),
                    () -> FlexEventResult.of(super.use(worldIn, playerIn, handIn))).holder();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack heldItem = context.getItemInHand();

        FlexEventResult result = runEvent("use_on_block", FlexEventContext.of(context),
                () -> new FlexEventResult(super.useOn(context), heldItem));

        if (result.stack() != heldItem && context.getPlayer() != null) {
            context.getPlayer().setItemInHand(context.getHand(), result.stack());
        }

        return result.result();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return Utils.orElseGet(useAction, () -> super.getUseAnimation(stack));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return Utils.orElseGet(useTime, () -> super.getUseDuration(stack));
    }

    @Override
    public boolean useOnRelease(ItemStack stack) {
        if (useFinishMode != null)
            return useFinishMode.isUseOnRelease();
        return super.useOnRelease(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) {
        runEvent("stopped_using",
                FlexEventContext.of(worldIn, entityLiving, stack).with(FlexEventContext.TIME_LEFT, timeLeft),
                () -> {
                    super.releaseUsing(stack, worldIn, entityLiving, timeLeft);
                    return FlexEventResult.pass(stack);
                });
    }

    @Override
    public ItemStack finishUsingItem(ItemStack heldItem, Level worldIn, LivingEntity entityLiving) {
        Supplier<FlexEventResult> resultSupplier = () -> FlexEventResult
                .success(super.finishUsingItem(heldItem, worldIn, entityLiving));

        FlexEventResult result = runEvent("end_using", FlexEventContext.of(worldIn, entityLiving, heldItem),
                resultSupplier);
        if (result.result() != InteractionResult.SUCCESS)
            return result.stack();

        return runEvent("use", FlexEventContext.of(worldIn, entityLiving, heldItem),
                () -> FlexEventResult.success(result.stack())).stack();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        if (lore != null)
            tooltip.addAll(lore);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        FlexEventResult result = runEvent("update",
                FlexEventContext.of(worldIn, entityIn, stack).with(FlexEventContext.SLOT, itemSlot)
                        .with(FlexEventContext.SELECTED, isSelected),
                () -> {
                    super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
                    return FlexEventResult.pass(stack);
                });
        if (result.stack() != stack) {
            entityIn.getSlot(itemSlot).set(result.stack());
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return Utils.orElseGet(attributeModifiers.get(slot), HashMultimap::create);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        if (toolActions != null)
            return toolActions.contains(toolAction);
        return super.canPerformAction(stack, toolAction);
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @org.jetbrains.annotations.Nullable RecipeType<?> recipeType) {
        return burnTime;
    }

    // endregion
}
