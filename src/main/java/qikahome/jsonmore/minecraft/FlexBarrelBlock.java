package qikahome.jsonmore.minecraft;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;

import dev.gigaherz.jsonthings.things.events.FlexEventContext;
import dev.gigaherz.jsonthings.things.events.FlexEventHandler;
import dev.gigaherz.jsonthings.things.events.FlexEventType;
import dev.gigaherz.jsonthings.things.shapes.DynamicShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import qikahome.jsonmore.JsonMore;
import qikahome.jsonmore.minecraft.StorageConnectorBlock.ControllerBlockEntity;
import qikahome.jsonmore.lib.BlockedDirection;
import qikahome.jsonmore.lib.ContainerPart;
import static qikahome.jsonmore.lib.ContainerPart.PART;
import qikahome.jsonmore.lib.ContainerScreenType;
import qikahome.jsonmore.lib.ExpandableMode;
import qikahome.jsonmore.lib.FaceFilter;
import qikahome.jsonmore.lib.IFlexContainer;
import qikahome.jsonmore.lib.IFlexEntityBlock;
import qikahome.jsonmore.lib.IProtectedBlock;
import qikahome.jsonmore.lib.ItemFilter;
import qikahome.jsonmore.lib.KeepInventoryMode;
import qikahome.jsonmore.lib.MultiContainer;
import qikahome.jsonmore.lib.PlacingDirections;
import qikahome.jsonmore.lib.ingredient.KeepInventoryContainerIngredient;
import qikahome.jsonmore.lib.ingredient.NotIngredient;

public class FlexBarrelBlock extends BaseEntityBlock
        implements IFlexEntityBlock<FlexBarrelBlock.FlexBarrelBlockEntity>, SimpleWaterloggedBlock, IProtectedBlock {

    public FlexBarrelBlock(BlockBehaviour.Properties properties, Map<Property<?>, Comparable<?>> propertyDefaultValues,
            int containerSize, SoundEvent soundOpen, SoundEvent soundClose, boolean waterloggedIn,
            PlacingDirections facing, KeepInventoryMode keepInventory, boolean angerPiglins,
            BlockedDirection blocked, Map<FaceFilter, ItemFilter> insertFilters,
            Map<FaceFilter, ItemFilter> extractFilters,
            ContainerScreenType screenType, ContainerScreenType connectedScreenType,
            Set<ExpandableMode> expandableModes,
            Identifier connectableContainers) {
        super(properties);
        initializeFlex(propertyDefaultValues);
        this.containerSize = containerSize;
        this.open = soundOpen == null ? SoundEvents.BARREL_OPEN : soundOpen;
        this.close = soundClose == null ? SoundEvents.BARREL_CLOSE : soundClose;
        this.waterloggedIn = waterloggedIn;
        this.facing = facing;
        this.keepInventory = keepInventory;
        this.angerPiglins = angerPiglins;
        this.blocked = blocked;
        this.insertFilters = insertFilters;
        this.extractFilters = extractFilters;
        this.screenType = screenType;
        this.connectedScreenType = connectedScreenType != null ? connectedScreenType : this.screenType;
        this.expandableModes = expandableModes != null ? expandableModes : Collections.emptySet();
        this.connectableContainers = TagKey.create(Registries.BLOCK, connectableContainers);
    }

    // region IFlexBlock
    private DynamicShape generalShape;
    private DynamicShape collisionShape;
    private DynamicShape raytraceShape;
    private DynamicShape renderShape;
    private final Map<FlexEventType, FlexEventHandler> eventHandlers = Maps.newHashMap();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void initializeFlex(Map<Property<?>, Comparable<?>> propertyDefaultValues) {
        BlockState def = getStateDefinition().any();
        if (propertyDefaultValues.size() > 0) {
            for (Map.Entry<Property<?>, Comparable<?>> entry : propertyDefaultValues.entrySet()) {
                Property prop = entry.getKey();
                Comparable value = entry.getValue();
                def = def.setValue(prop, value);
            }
        }
        def = def.setValue(BlockStateProperties.OPEN, false);
        def = def.setValue(CONNECTED, false);
        registerDefaultState(def);
    }

    @Override
    public <T> void addEventHandler(FlexEventType<T> event, FlexEventHandler<T> eventHandler) {
        eventHandlers.put(event, eventHandler);
    }

    @Override
    @Nullable
    public <T> FlexEventHandler<T> getEventHandler(FlexEventType<T> event) {
        return eventHandlers.get(event);
    }

    @Override
    public void setGeneralShape(@Nullable DynamicShape shape) {
        this.generalShape = shape;
    }

    @Override
    public void setCollisionShape(@Nullable DynamicShape shape) {
        this.collisionShape = shape;
    }

    @Override
    public void setRaytraceShape(@Nullable DynamicShape shape) {
        this.raytraceShape = shape;
    }

    @Override
    public void setRenderShape(@Nullable DynamicShape shape) {
        this.renderShape = shape;
    }
    // endregion

    // region Block
    @Deprecated
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        if (this.generalShape != null)
            return generalShape.getShape(state);
        return super.getShape(state, worldIn, pos, context);
    }

    @Deprecated
    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter worldIn, BlockPos pos) {
        if (this.raytraceShape != null)
            return raytraceShape.getShape(state);
        return super.getInteractionShape(state, worldIn, pos);
    }

    @Deprecated
    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter reader, BlockPos pos) {
        if (this.collisionShape != null)
            return collisionShape.getShape(state);
        return super.getBlockSupportShape(state, reader, pos);
    }

    @Deprecated
    @Override
    public VoxelShape getOcclusionShape(BlockState state) {
        if (this.renderShape != null)
            return renderShape.getShape(state);
        return super.getOcclusionShape(state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        return runEvent(FlexEventType.USE_BLOCK_WITHOUT_ITEM, FlexEventContext.of(level, pos, state)
                .with(FlexEventContext.USER, player)
                .withRayTrace(hitResult), () -> fallbackUse(state, level, pos, player, hitResult));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        return runEvent(FlexEventType.USE_BLOCK_WITH_ITEM, FlexEventContext.of(level, pos, state)
                .with(FlexEventContext.USER, player)
                .withRayTrace(hitResult), () -> super.useItemOn(stack, state, level, pos, player, hand, hitResult));
    }

    // endregion
    // region BarrelBlock

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof FlexBarrelBlockEntity flexEntity) {
            flexEntity.recheckOpen();
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FlexBarrelBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        List<Container> containers = getContainers(level, pos, state);
        if (containers.isEmpty()) {
            return 0;
        }
        int signal = 0;
        for (Container c : containers) {
            signal += AbstractContainerMenu.getRedstoneSignalFromContainer(c);
        }
        return signal / containers.size();
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.FACING,
                rotation.rotate(state.getValue(BlockStateProperties.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState rotated = state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.FACING)));
        return mirror == Mirror.NONE ? rotated
                : rotated.setValue(PART, rotated.getValue(PART).getOpposite());
    }
    // endregion

    // region ShulkerBoxBlock
    // @Override
    // public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
    //         TooltipFlag flag) {
    //     super.appendHoverText(stack, context, display, tooltip, flag);
    //     ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
    //     if (contents != null) {
    //         var items = NonNullList.withSize(contents.getSlots(), ItemStack.EMPTY);
    //         contents.copyInto(items);
    //         var nonEmpty = items.stream().filter(s -> !s.isEmpty()).toList();
    //         if (!nonEmpty.isEmpty()) {
    //             tooltip.accept(Component.empty());
    //             int shown = 0;
    //             for (ItemStack itemStack : nonEmpty) {
    //                 if (shown >= 5)
    //                     break;
    //                 tooltip.accept(Component.literal(" ").append(itemStack.getDisplayName())
    //                         .append(Component.literal(" x"))
    //                         .append(Component.literal(String.valueOf(itemStack.getCount()))));
    //                 shown++;
    //             }
    //             if (nonEmpty.size() > 5) {
    //                 tooltip.accept(Component.translatable("container.shulkerBox.more", nonEmpty.size() - 5));
    //             }
    //         }
    //     }
    // }
    // endregion

    // region FlexBarrelBlock
    public final boolean waterloggedIn;
    public final int containerSize;
    public final SoundEvent open;
    public final SoundEvent close;
    public final PlacingDirections facing;
    public final KeepInventoryMode keepInventory;
    public final boolean angerPiglins;
    public final BlockedDirection blocked;
    public final Map<FaceFilter, ItemFilter> insertFilters;
    public final Map<FaceFilter, ItemFilter> extractFilters;
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");
    public final ContainerScreenType screenType;
    public final ContainerScreenType connectedScreenType;
    public final Set<ExpandableMode> expandableModes;
    public final TagKey<Block> connectableContainers;

    public static final ItemFilter DEFAULT_PLACE_FILTER;
    static {
        DEFAULT_PLACE_FILTER = new ItemFilter(
                NotIngredient.of(
                        KeepInventoryContainerIngredient.MAY.toVanilla()));
    }

    public boolean isConnectableBlock(BlockState neighbor) {
        return neighbor.getBlock() == this || neighbor.is(connectableContainers);
    }

    public InteractionResult fallbackUse(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        // When captured by a storage connector, proxy to controller GUI
        if (state.getValue(CONNECTED)) {
            if (level.isClientSide())
                return InteractionResult.SUCCESS;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FlexBarrelBlockEntity fbe) {
                var controller = fbe.getController();
                if (controller != null && controller.isAssembled() && player instanceof ServerPlayer serverPlayer) {
                    Container container = controller.getControllerContainer();
                    BlockState controllerState = level.getBlockState(controller.getBlockPos());
                    if (controllerState.getBlock() instanceof StorageConnectorBlock scb) {
                        serverPlayer.openMenu(
                                scb.screenType.createMenuProvider(Collections.singletonList(container),
                                        container.getContainerSize()),
                                buffer -> scb.screenType.writeAdditionalData(buffer,
                                        Collections.singletonList(container), container.getContainerSize()));
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        } else {
            var part = state.getValue(PART);
            var facing = state.getValue(BlockStateProperties.FACING);
            if (blocked.isBlocked(level, pos, facing,
                    part)) {
                return InteractionResult.CONSUME;
            }
            if (part.isConnected()) {
                Direction neighborDir = part.getWorldDirection(facing).getOpposite();
                if (neighborDir != null) {
                    BlockPos neighborPos = pos.relative(neighborDir);
                    BlockState neighborState = level.getBlockState(neighborPos);
                    if (neighborState.getBlock() instanceof FlexBarrelBlock neighbor) {
                        if (neighbor.blocked.isBlocked(level, neighborPos,
                                neighborState.getValue(BlockStateProperties.FACING),
                                neighborState.getValue(PART))) {
                            return InteractionResult.CONSUME;
                        }
                    }
                }
            }
            BlockEntity blockentity = level.getBlockEntity(pos);
            if (blockentity instanceof FlexBarrelBlockEntity flexEntity) {
                if (player instanceof ServerPlayer serverPlayer) {
                    ContainerScreenType screen = state.getValue(PART).isConnected()
                            ? connectedScreenType
                            : screenType;
                    var containers = getContainers(level, pos, state);
                    var containerSize = MultiContainer.of(containers).getContainerSize();
                    serverPlayer.openMenu(screen.createMenuProvider(containers, containerSize),
                            buffer -> screen.writeAdditionalData(buffer, this.getContainers(level, pos, state),
                                    containerSize));
                }
                if (angerPiglins && level instanceof ServerLevel sl) {
                    PiglinAi.angerNearbyPiglins(sl,player, true);
                }
            }

            return InteractionResult.CONSUME;
        }
    }

    // BarrelBlockEntity
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (state.getValue(CONNECTED) && !level.isClientSide()) {
            return super.playerWillDestroy(level, pos, state, player);
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof FlexBarrelBlockEntity flexEntity) {
            if (shouldKeepInventory(player)) 
                flexEntity.shouldKeepInventory=true;
            if (flexEntity.shouldKeepInventory && !level.isClientSide() && player.preventsBlockDrops() && !flexEntity.isEmpty()) {
            {

                        ItemStack itemStack = new ItemStack(state.getBlock());
                itemStack.applyComponents(blockEntity.collectComponents());
                ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, itemStack);
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);

                }
            } else {
                flexEntity.unpackLootTable(player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private boolean shouldKeepInventory(Player player) {
        return switch (keepInventory) {
            case ALWAYS -> true;
            case SILK_TOUCH -> player!=null && 
                player.getMainHandItem().getEnchantmentLevel(player.level().registryAccess()
                        .holderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH)) > 0;
            case NEVER -> false;
        };
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof FlexBarrelBlockEntity fbe && fbe.shouldKeepInventory) {
            builder = builder.withDynamicDrop(ShulkerBoxBlock.CONTENTS, output -> {
                for (int i = 0; i < fbe.getContainerSize(); i++) {
                    output.accept(fbe.getItem(i));
                }
            });
        }
        return super.getDrops(state, builder);
    }

    public boolean retryConnection(Level level, BlockPos pos, BlockState state) {
        var connection = state.getValue(PART);
        var facing = state.getValue(BlockStateProperties.FACING);
        boolean connected = false;
        Direction neighborDir = connection.getWorldDirection(facing).getOpposite();
        if (neighborDir != null) {
            BlockPos neighborPos = pos.relative(neighborDir);
            BlockState neighborState = level.getBlockState(neighborPos);
            for (ExpandableMode mode : expandableModes) {
                if (isConnectableBlock(neighborState)
                        && neighborState.getValue(PART) == ContainerPart.NONE) {
                    connected = mode.connect(neighborState, neighborPos, level, neighborDir.getOpposite());
                }
                if (connected) {
                    level.invalidateCapabilities(neighborPos);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        var connection = state.getValue(PART);
        var facing = state.getValue(BlockStateProperties.FACING);
        if (connection.isConnected()) {
            if (!retryConnection(level, pos, state)) {
                level.setBlock(pos, state.setValue(PART, ContainerPart.NONE), 0);
            }
        }
    }

    @Override
    public net.minecraft.world.level.material.FluidState getFluidState(BlockState state) {
        return waterloggedIn && state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED)
                        ? Fluids.WATER.getSource(false)
                        : super.getFluidState(state);
    }

    public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, net.minecraft.world.level.material.Fluid fluid) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, net.minecraft.world.level.material.FluidState fluidState) {
        if (!state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return false;
        }
        return SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);
    }

    @Override
    public BlockEntityType<FlexBarrelBlockEntity> getBlockEntityType() {
        return MinecraftPlugin.BARREL_TILE.get();
    }

    public List<Container> getContainers(FlexBarrelBlockEntity flexBlockEntity) {
        BlockState state = flexBlockEntity.getBlockState();
        return getContainers(flexBlockEntity.getLevel(), flexBlockEntity.getBlockPos(), state);
    }

    @Nullable
    public List<Container> getContainers(Level level, BlockPos pos, BlockState state) {
        // When captured by a controller, return self (methods delegate to the controller)
        if (state.hasProperty(CONNECTED) && state.getValue(CONNECTED)) {
            BlockEntity be = level.getBlockEntity(pos);
            return be instanceof Container ? Collections.singletonList((Container) be)
                    : Collections.emptyList();
        }
        ContainerPart part = state.getValue(PART);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!part.isConnected()) {
            return blockEntity instanceof Container ? Collections.singletonList((Container) blockEntity)
                    : Collections.emptyList();
        }

        Direction facing = state.getValue(BlockStateProperties.FACING);
        Direction otherDir = part.getWorldDirection(facing).getOpposite();
        if (otherDir == null)
            return blockEntity instanceof Container ? Collections.singletonList((Container) blockEntity)
                    : Collections.emptyList();

        BlockPos otherPos = pos.relative(otherDir);
        BlockEntity otherEntity = level.getBlockEntity(otherPos);

        if (otherEntity instanceof FlexBarrelBlockEntity otherBarrel) {
            BlockEntity thisEntity = level.getBlockEntity(pos);
            if (thisEntity instanceof FlexBarrelBlockEntity thisBarrel) {
                boolean thisIsFirst = part == ContainerPart.LEFT
                        || part == ContainerPart.TOP
                        || part == ContainerPart.FRONT;

                if (thisIsFirst) {
                    return List.of(thisBarrel, otherBarrel);
                } else {
                    return List.of(otherBarrel, thisBarrel);
                }
            }
        }

        return blockEntity instanceof Container ? Collections.singletonList((Container) blockEntity)
                : Collections.emptyList();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder1) {
        builder1.add(BlockStateProperties.OPEN, BlockStateProperties.FACING, PART, CONNECTED);
    }

    private static boolean isNotCaptured(BlockState neighborState) {
        return !neighborState.hasProperty(CONNECTED) || !neighborState.getValue(CONNECTED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 基础状态：朝向 + 未连接
        BlockState state = this.defaultBlockState()
                .setValue(BlockStateProperties.FACING, facing.getDirection(context))
                .setValue(PART, ContainerPart.NONE)
                .setValue(BlockStateProperties.OPEN, false)
                .setValue(CONNECTED, false);

        // 处理含水
        if (waterloggedIn && state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            boolean hasWater = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
            state = state.setValue(BlockStateProperties.WATERLOGGED, hasWater);
        }

        if (expandableModes.isEmpty()) {
            return state;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        Direction clickedFace = context.getClickedFace();
        boolean sneaking = player != null && player.isShiftKeyDown();
        Direction thisFacing = state.getValue(BlockStateProperties.FACING);

        // 潜行模式：精准连接（只检查点击面反方向的邻居）
        if (sneaking) {
            for (ExpandableMode mode : expandableModes) {
                Direction neighborDir = clickedFace.getOpposite();
                BlockPos neighborPos = pos.relative(neighborDir);
                BlockState neighborState = level.getBlockState(neighborPos);

                if (isConnectableBlock(neighborState)
                        && neighborState.getValue(PART) == ContainerPart.NONE
                        && isNotCaptured(neighborState)) {
                    BlockState newState = mode.tryForceConnect(state, neighborState, neighborPos, level, clickedFace,
                            true);
                    if (newState != state) {
                        return newState; // 连接成功
                    }
                }
            }
            return state;
        }

        // 非潜行模式：自动扫描所有可能的方向
        for (ExpandableMode mode : expandableModes) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                BlockState neighborState = level.getBlockState(neighborPos);

                if (isConnectableBlock(neighborState)
                        && neighborState.getValue(PART) == ContainerPart.NONE
                        && isNotCaptured(neighborState)) {
                    // 自动扫描时，将 dir 作为"虚拟点击面"传入
                    BlockState newState = mode.tryConnect(state, neighborState, neighborPos, level, dir.getOpposite(),
                            true);
                    if (newState != state) {
                        return newState;
                    }
                }
            }
        }

        return state;
    }
    @Override
    public boolean maySetBlock(BlockState oldState, Level level, BlockPos pos, BlockState newState, @Block.UpdateFlags int updateFlags, int updateLimit)
    {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity blockentity = level.getBlockEntity(pos);
            if (blockentity instanceof FlexBarrelBlockEntity fbe && oldState.getValue(CONNECTED)) {
                if (!level.isClientSide()) {
                    var controller = fbe.getController();
                    if (controller != null && controller.isAssembled()) {
                        JsonMore.LOGGER.debug("FlexBarrelBlock.onRemove: found controller at {}, disassembling", pos);
                        controller.disassemble(level);
                        level.setBlock(pos, oldState.setValue(CONNECTED, false), 2);
                        JsonMore.LOGGER.debug("FlexBarrelBlock.onRemove: disassembled, restored to {}", oldState);
                        return false;
                    }
                    JsonMore.LOGGER.debug("FlexBarrelBlock.onRemove: controller not found at {}, allowing removal", pos);
                }
            }
        }
        return true;
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        ContainerPart part = state.getValue(PART);
        if (this.waterloggedIn && state.getValue(BlockStateProperties.WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (part == ContainerPart.NONE)
            return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
        if (direction == part.getWorldDirection(state.getValue(BlockStateProperties.FACING)).getOpposite())
            if (neighborState.getBlock() instanceof FlexBarrelBlock flex) {
                var neiPart = neighborState.getValue(PART);
                var neiFacing = neighborState.getValue(BlockStateProperties.FACING);
                if (neiPart.getWorldDirection(neiFacing) != direction) {
                    if (level instanceof Level l) l.invalidateCapabilities(pos);
                    return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random)
                            .setValue(PART, ContainerPart.NONE);
                }
            } else {
                if (level instanceof Level l) l.invalidateCapabilities(pos);
                return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random)
                        .setValue(PART, ContainerPart.NONE);
            }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }
    // endregion

    public static class FlexBarrelBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
        private NonNullList<ItemStack> items;
        private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
            protected void onOpen(Level level, BlockPos pos, BlockState state) {
                if (isMajor(level, pos, state))
                    FlexBarrelBlockEntity.this.playSound(state, flexBlock.open);
                FlexBarrelBlockEntity.this.updateBlockState(state, true);
            }

            protected void onClose(Level level, BlockPos pos, BlockState state) {
                if (isMajor(level, pos, state))
                    FlexBarrelBlockEntity.this.playSound(state, flexBlock.close);
                FlexBarrelBlockEntity.this.updateBlockState(state, false);
            }

            protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
            }

            public boolean isOwnContainer(Player player) {
                if (player.containerMenu instanceof ChestMenu chestMenu) {
                    Container container = chestMenu.getContainer();
                    return container == FlexBarrelBlockEntity.this
                            || container instanceof CompoundContainer compoundContainer
                                    && compoundContainer.contains(FlexBarrelBlockEntity.this);
                } else if (player.containerMenu instanceof IFlexContainer adapterContainer) {
                    return adapterContainer.isThisContainer(FlexBarrelBlockEntity.this);
                } else {
                    return false;
                }
            }
        };
        private final FlexBarrelBlock flexBlock;
        private boolean captured = false;
        @Nullable
        private BlockPos controllerPos;

        private transient boolean shouldKeepInventory;

        public FlexBarrelBlockEntity(BlockPos pos, BlockState state) {
            super(MinecraftPlugin.BARREL_TILE.get(), pos, state);
            if (state.getBlock() instanceof FlexBarrelBlock flexBlock) {
                this.items = NonNullList.withSize(flexBlock.containerSize, ItemStack.EMPTY);
                this.flexBlock = flexBlock;
                this.shouldKeepInventory = flexBlock.keepInventory == KeepInventoryMode.ALWAYS;
            } else
                throw new IllegalArgumentException("Not a FlexBarrelBlock");
        }

        @Nullable
        public StorageConnectorBlock.ControllerBlockEntity getController() {
            if (controllerPos == null || level == null)
                return null;
            BlockPos absolute = worldPosition.offset(controllerPos);
            BlockEntity be = level.getBlockEntity(absolute);
            return be instanceof StorageConnectorBlock.ControllerBlockEntity cbe ? cbe : null;
        }

        public boolean isConnected() {
            return captured;
        }

        public void setCaptured(boolean captured, @Nullable BlockPos controllerPos) {
            this.captured = captured;
            this.controllerPos = controllerPos;
            setChanged();
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            output.putBoolean("Captured", captured);
            if (controllerPos != null) {
                output.putIntArray("CtrlPos", new int[]{controllerPos.getX(), controllerPos.getY(), controllerPos.getZ()});
                super.saveAdditional(output);
                output.discard("LootTable");
                output.discard("LootTableSeed");
                output.discard("Items");
            } else {
                super.saveAdditional(output);
                if (!this.trySaveLootTable(output)) {
                    ContainerHelper.saveAllItems(output, this.items);
                }
            }
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            captured = input.getBooleanOr("Captured", false);
            int[] ctrlArr = input.getIntArray("CtrlPos").orElse(new int[0]);
            controllerPos = ctrlArr.length == 3 ? new BlockPos(ctrlArr[0], ctrlArr[1], ctrlArr[2]) : null;
            if (controllerPos != null) {
                super.loadAdditional(input);
            } else {
                super.loadAdditional(input);
                this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
                if (!this.tryLoadLootTable(input)) {
                    ContainerHelper.loadAllItems(input, this.items);
                }
            }
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level != null && !level.isClientSide()) {
                BlockState state = level.getBlockState(worldPosition);
                if (state.hasProperty(CONNECTED)) {
                    captured = state.getValue(CONNECTED);
                    if (!captured)
                        controllerPos = null;
                }
            }
        }

        @Override
        public int getContainerSize() {
            if (isConnected()) {
                ControllerBlockEntity controller = getController();
                if (controller != null)
                    return controller.getContainerSize();
                return 0;
            }
            return flexBlock.containerSize;
        }

        @Override
        public NonNullList<ItemStack> getItems() {
            if (isConnected()) {
                ControllerBlockEntity controller = getController();
                if (controller != null)
                    return controller.getItems();
                return NonNullList.create();
            }
            return this.items;
        }

        @Override
        public void setItems(NonNullList<ItemStack> items) {
            if (isConnected()) {
                ControllerBlockEntity controller = getController();
                if (controller != null) {
                    controller.setItems(items);
                    return;
                }
                return;
            }
            this.items = items;
        }

        @Override
        public void clearContent() {
            if (!isConnected()) {
                super.clearContent();
            }
        }

        @Override
        protected Component getDefaultName() {
            return flexBlock.getName();
        }

        @Override
        protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
            return null; // Block should parse this
        }

        @Override
public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    if(!shouldKeepInventory)
        super.preRemoveSideEffects(pos, state);
}

        @Override
        public void startOpen(ContainerUser user) {
            if (isConnected()) {
                ControllerBlockEntity controller = getController();
                if (controller != null) {
                    controller.startOpen(user);
                }
                return;
            }
            var living = user.getLivingEntity();
            if (!this.remove && !living.isSpectator()) {
                this.openersCounter.incrementOpeners(living, this.getLevel(), this.getBlockPos(),
                        this.getBlockState(),user.getContainerInteractionRange());
            }
        }

        @Override
        public void stopOpen(ContainerUser user) {
            if (isConnected()) {
                ControllerBlockEntity controller = getController();
                if (controller != null) {
                    controller.stopOpen(user);
                }
                return;
            }
            var living = user.getLivingEntity();
            if (!this.remove && !living.isSpectator()) {
                this.openersCounter.decrementOpeners(living, this.getLevel(), this.getBlockPos(),
                        this.getBlockState());
            }
        }

        public void recheckOpen() {
            if (!this.remove) {
                this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
            }
        }

        void updateBlockState(BlockState state, boolean open) {
            this.level.setBlock(this.getBlockPos(), state.setValue(BlockStateProperties.OPEN, Boolean.valueOf(open)),
                    3);
        }

        void playSound(BlockState state, SoundEvent sound) {
            Vec3i vec3i = state.getValue(BlockStateProperties.FACING).getUnitVec3i();
            ContainerPart part = state.getValue(PART);
            double d0 = (double) this.worldPosition.getX() + 0.5D + (double) vec3i.getX() / 2.0D;
            double d1 = (double) this.worldPosition.getY() + 0.5D + (double) vec3i.getY() / 2.0D;
            double d2 = (double) this.worldPosition.getZ() + 0.5D + (double) vec3i.getZ() / 2.0D;
            switch (part) {
                case FRONT: {
                    d0 += (double) vec3i.getX() * -0.5d;
                    d1 += (double) vec3i.getY() * -0.5d;
                    d2 += (double) vec3i.getZ() * -0.5d;
                    break;
                }
                case LEFT: {
                    d0 += (double) vec3i.getZ() * -0.5d;
                    d2 += (double) vec3i.getX() * 0.5d;
                    break;
                }
                case TOP:
                    d1 -= 0.5d;
            }
            this.level.playSound((Player) null, d0, d1, d2, sound, SoundSource.BLOCKS, 0.5F,
                    this.level.getRandom().nextFloat() * 0.1F + 0.9F);
        }

        @Override
        public int[] getSlotsForFace(Direction direction) {
            if (isConnected()) {
                ControllerBlockEntity controller = getController();
                if (controller != null)
                    return controller.getSlotsForFace(direction);
                return new int[0];
            }
            int[] slots = new int[getContainerSize()];
            for (int i = 0; i < slots.length; i++) {
                slots[i] = i;
            }
            return slots;
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return canPlaceItemThroughFace(index, stack, null);
        }

        @Override
        public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
            if (isConnected()) {
                ControllerBlockEntity controller = getController();
                if (controller != null)
                    return controller.canPlaceItemThroughFace(index, stack, direction);
                return false;
            }
            if (flexBlock.insertFilters.isEmpty()) {
                return true;
            }

            Direction facing = getBlockState().getValue(BlockStateProperties.FACING);

            for (Map.Entry<FaceFilter, ItemFilter> entry : flexBlock.insertFilters.entrySet()) {
                if (entry.getKey().test(facing, direction)) {
                    if (!entry.getValue().test(stack)) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
            if (isConnected()) {
                ControllerBlockEntity controller = getController();
                if (controller != null)
                    return controller.canTakeItemThroughFace(index, stack, direction);
                return false;
            }
            Direction facing = getBlockState().getValue(BlockStateProperties.FACING);

            if (flexBlock.extractFilters.isEmpty()) {
                return true;
            }

            for (Map.Entry<FaceFilter, ItemFilter> entry : flexBlock.extractFilters.entrySet()) {
                if (entry.getKey().test(facing, direction)) {
                    if (!entry.getValue().test(stack)) {
                        return false;
                    }
                }
            }

            return true;
        }

    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }

    public static boolean isMajor(Level level, BlockPos pos, BlockState state) {
        if(!(state.getBlock() instanceof FlexBarrelBlock))
        {
            JsonMore.LOGGER.warn("Block is not FlexBarrelBlock, idk if its major.");
            return false;
        }
        var part = state.getValue(PART);
        var facing = state.getValue(BlockStateProperties.FACING);
        switch (part) {
            case FRONT: {
                if (facing == Direction.DOWN || facing == Direction.SOUTH || facing == Direction.EAST)
                    if (level.getBlockState(pos.relative(facing.getOpposite()))
                            .getValue(PART) == ContainerPart.FRONT)
                        return false;
            }
            case LEFT, TOP, NONE:
                return true;
        }
        return false;
    }
}
