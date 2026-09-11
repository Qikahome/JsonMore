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
import net.minecraft.core.Direction;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class FlexStandingAndWallBlockItem extends StandingAndWallBlockItem
        implements IEventRunner, ToolActionItem, IFlexStandingAndWallBlockItem {

    /**
     * vanilla 的 {@link StandingAndWallBlockItem} 构造器要求立即传入方块实例，而 JsonThings 的
     * standing_and_wall 类型允许引用尚未注册完成的方块，因此这里传 {@code null} 并由
     * {@link #getBlock()}/{@link #getWallBlock()} 惰性解析（配合 MixinStandingAndWallBlockItem
     * 拦截对 {@code wallBlock} 字段的读取）。
     * <p>
     * Forge 侧用的是 {@code RegistryObject<Block>}，Fabric 侧改用 {@code Supplier<Block>}，
     * 由调用方给出 {@code () -> BuiltInRegistries.BLOCK.get(id)} 形式的懒加载。
     */
    public FlexStandingAndWallBlockItem(Supplier<Block> block, Supplier<Block> wallBlock,
            boolean useBlockName, Properties properties,
            ItemBuilder builder, Direction direction) {
        super(null, null, properties, direction);
        this.useBlockName = useBlockName;
        this.block = block;
        this.wallBlock = wallBlock;
        this.useAction = builder.getUseAnim();
        this.useTime = builder.getUseTime();
        this.useFinishMode = builder.getUseFinishMode();
        this.attributeModifiers = builder.getAttributeModifiers();
        this.lore = builder.getLore();
        this.toolActions = builder.getToolActions();
        initializeFlex();
    }

    // region BlockItem
    private final boolean useBlockName;
    private final Supplier<Block> block;

    // Recreation of ItemNameBlockItem's function
    @Override
    public String getDescriptionId() {
        return useBlockName ? getBlock().getDescriptionId() : super.getOrCreateDescriptionId();
    }

    @Override
    public Block getBlock() {
        Block resolved = block.get();
        return resolved != null ? resolved : Blocks.AIR;
    }

    public boolean canFitInsideContainerItems() {
        return true;
    }

    public void onDestroyed(ItemEntity itemEntity) {
    }
    // endregion

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

    // Forge 的 getAttributeModifiers(EquipmentSlot, ItemStack) 是 Forge 扩展方法，
    // vanilla/Fabric 的等价物是 getDefaultAttributeModifiers(EquipmentSlot)。
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

    // region IFlexStandingAndWallBlockItem

    private final Supplier<Block> wallBlock;

    @Override
    public Block getWallBlock() {
        Block resolved = wallBlock.get();
        return resolved != null ? resolved : Blocks.AIR;
    }
    // endregion
}
