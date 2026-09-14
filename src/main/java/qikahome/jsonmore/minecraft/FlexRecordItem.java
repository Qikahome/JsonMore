package qikahome.jsonmore.minecraft;

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
import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import io.github.fabricators_of_create.porting_lib.tool.addons.ToolActionItem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 唱片物品（{@code jsonmore:record}）。
 *
 * <p>Fabric/vanilla 的 {@link RecordItem} 只有「SoundEvent + 以秒为单位的长度」构造器
 * （Forge 才有 {@code Supplier<SoundEvent>} 且以 tick 计长的重载），因此这里传入解析期
 * 已解析好的 SoundEvent，并把长度按 tick 自己保存、覆盖 {@link #getLengthInTicks()}，
 * 让 JSON 里的 {@code length} 在两端保持一致（都是 tick）。
 */
public class FlexRecordItem extends RecordItem implements IEventRunner, ToolActionItem {

    public FlexRecordItem(int comparatorValue, SoundEvent soundEvent, Properties properties, int lengthInTicks,
            ItemBuilder builder) {
        // 长度参数单位为秒（父类内部 ×20），这里只作占位，实际长度由 getLengthInTicks() 提供
        super(comparatorValue, soundEvent, properties, 1);
        this.lengthInTicks = lengthInTicks;
        this.useAction = builder.getUseAnim();
        this.useTime = builder.getUseTime();
        this.useFinishMode = builder.getUseFinishMode();
        this.attributeModifiers = builder.getAttributeModifiers();
        this.lore = builder.getLore();
        this.toolActions = builder.getToolActions();
        initializeFlex();
    }

    private final int lengthInTicks;

    @Override
    public int getLengthInTicks() {
        return lengthInTicks;
    }

    // region IFlexItem
    private final Map<String, FlexEventHandler> eventHandlers = Maps.newHashMap();

    private final Map<EquipmentSlot, Multimap<Attribute, AttributeModifier>> attributeModifiers;
    private final UseAnim useAction;
    private final Integer useTime;
    private final UseFinishMode useFinishMode;
    private final List<MutableComponent> lore;
    private final Set<ToolAction> toolActions;

    private void initializeFlex() {
        for (EquipmentSlot slot1 : EquipmentSlot.values()) {
            attributeModifiers.computeIfAbsent(slot1, key -> ArrayListMultimap.create())
                    .putAll(super.getDefaultAttributeModifiers(slot1));
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
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return Utils.orElseGet(attributeModifiers.get(slot), HashMultimap::create);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        if (toolActions != null)
            return toolActions.contains(toolAction);
        return false;
    }

    // 燃烧时长（burn_duration）由 JsonThings 的 ItemParser 经 Fabric FuelRegistry 注册
    // （上游 Forge 借扩展注入的 Item#getBurnTime 在 vanilla/Fabric 不存在）。

    // endregion
}
