package qikahome.jsonmore.minecraft;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;

import dev.gigaherz.jsonthings.things.events.FlexEventContext;
import dev.gigaherz.jsonthings.things.events.FlexEventHandler;
import dev.gigaherz.jsonthings.things.events.FlexEventResult;
import dev.gigaherz.jsonthings.things.shapes.DynamicShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.crafting.CompoundIngredient;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import qikahome.jsonmore.lib.BlockedDirection;
import qikahome.jsonmore.lib.ContainerPart;
import qikahome.jsonmore.lib.ExpandableMode;
import qikahome.jsonmore.lib.FaceFilter;
import qikahome.jsonmore.lib.IFlexContainer;
import qikahome.jsonmore.lib.IFlexEntityBlock;
import net.minecraft.resources.ResourceLocation;
import qikahome.jsonmore.lib.ContainerScreenType;
import qikahome.jsonmore.lib.ItemFilter;
import qikahome.jsonmore.lib.KeepInventoryMode;
import qikahome.jsonmore.lib.MultiContainer;
import qikahome.jsonmore.lib.PlacingDirections;
import qikahome.jsonmore.lib.ingredient.KeepInventoryContainerIngredient;
import qikahome.jsonmore.lib.ingredient.NotIngredient;
import net.minecraft.world.level.block.Mirror;
import static qikahome.jsonmore.lib.ContainerPart.PART;

public class FlexBarrelBlock extends BaseEntityBlock
        implements IFlexEntityBlock<FlexBarrelBlock.FlexBarrelBlockEntity>, SimpleWaterloggedBlock {

    public FlexBarrelBlock(BlockBehaviour.Properties properties, Map<Property<?>, Comparable<?>> propertyDefaultValues,
            int containerSize, SoundEvent soundOpen, SoundEvent soundClose, boolean waterloggedIn,
            PlacingDirections facing, KeepInventoryMode keepInventory, boolean angerPiglins,
            BlockedDirection blocked, Map<FaceFilter, ItemFilter> insertFilters,
            Map<FaceFilter, ItemFilter> extractFilters,
            ContainerScreenType screenType, ContainerScreenType connectedScreenType,
            Set<ExpandableMode> expandableModes,
            ResourceLocation connectableContainers) {
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
    private final Map<String, FlexEventHandler> eventHandlers = Maps.newHashMap();

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
        registerDefaultState(def);
    }

    @Override
    public void addEventHandler(String eventName, FlexEventHandler eventHandler) {
        eventHandlers.put(eventName, eventHandler);
    }

    @Override
    public FlexEventHandler getEventHandler(String eventName) {
        return eventHandlers.get(eventName);
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
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter worldIn, BlockPos pos) {
        if (this.renderShape != null)
            return renderShape.getShape(state);
        return super.getOcclusionShape(state, worldIn, pos);
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn,
            BlockHitResult hit) {
        return runEvent("use", FlexEventContext.of(worldIn, pos, state)
                .withHand(player, handIn)
                .withRayTrace(hit), () -> FlexEventResult.of(fallbackUse(state, worldIn, pos, player, handIn, hit)))
                .result();
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
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
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
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        if (blockEntityTag != null && blockEntityTag.contains("Items")) {
            ListTag itemsList = blockEntityTag.getList("Items", 10);
            if (!itemsList.isEmpty()) {
                tooltip.add(Component.empty());
                // tooltip.add(Component.translatable("container.shulkerBox.contains",
                // itemsList.size(), containerSize));

                int shown = 0;
                for (int i = 0; i < itemsList.size() && shown < 5; i++) {
                    CompoundTag itemTag = itemsList.getCompound(i);
                    ItemStack itemStack = ItemStack.of(itemTag);
                    if (!itemStack.isEmpty()) {
                        tooltip.add(Component.literal(" ").append(itemStack.getDisplayName())
                                .append(Component.literal(" x"))
                                .append(Component.literal(String.valueOf(itemStack.getCount()))));
                        shown++;
                    }
                }
                if (itemsList.size() > 5) {
                    tooltip.add(Component.translatable("container.shulkerBox.more", itemsList.size() - 5));
                }
            }
        }
    }
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
    public final ContainerScreenType screenType;
    public final ContainerScreenType connectedScreenType;
    public final Set<ExpandableMode> expandableModes;
    public final TagKey<Block> connectableContainers;

    public static final ItemFilter DEFAULT_PLACE_FILTER;
    static {
        DEFAULT_PLACE_FILTER = new ItemFilter(
                NotIngredient.of(
                        CompoundIngredient.of(
                                Ingredient.of(TagKey.create(Registries.ITEM,
                                        new ResourceLocation("jsonmore:keep_inventory_containers"))),
                                new KeepInventoryContainerIngredient(KeepInventoryContainerIngredient.Mode.MAY))));
    }

    public boolean isConnectableBlock(BlockState neighbor) {
        return neighbor.getBlock() == this || neighbor.is(connectableContainers);
    }

    public InteractionResult fallbackUse(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
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
                    NetworkHooks.openScreen(serverPlayer, screen.createMenuProvider(containers, containerSize),
                            buffer -> screen.writeAdditionalData(buffer, this.getContainers(level, pos, state),
                                    containerSize));
                }
                if (angerPiglins) {
                    PiglinAi.angerNearbyPiglins(player, true);
                }
            }

            return InteractionResult.CONSUME;
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof FlexBarrelBlockEntity flexEntity) {
            if (shouldKeepInventory(player)) {
                if (!level.isClientSide) {
                    if (!player.isCreative() || !flexEntity.isEmpty()) {
                        ItemStack itemStack = new ItemStack(this);
                        blockEntity.saveToItem(itemStack);
                        if (flexEntity.hasCustomName()) {
                            itemStack.setHoverName(flexEntity.getCustomName());
                        }
                        popResource(level, pos, itemStack);
                    }
                    flexEntity.clearContent();
                }
            } else {
                flexEntity.unpackLootTable(player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    private boolean shouldKeepInventory(Player player) {
        return switch (keepInventory) {
            case ALWAYS -> true;
            case SILK_TOUCH -> player.getMainHandItem().getEnchantmentLevel(
                    net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH) > 0;
            case NEVER -> false;
        };
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (keepInventory == KeepInventoryMode.ALWAYS) {
            return Collections.emptyList();
        }
        return super.getDrops(state, builder);
    }

    public boolean retryConnection(LevelAccessor level, BlockPos pos, BlockState state) {
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
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            BlockEntity blockentity = level.getBlockEntity(pos);
            if (blockentity instanceof FlexBarrelBlockEntity flexEntity) {
                flexEntity.setCustomName(stack.getHoverName());
            }
        }
        var connection = state.getValue(PART);
        var facing = state.getValue(BlockStateProperties.FACING);
        if (connection.isConnected()) {
            if (!retryConnection(level, pos, state)) {
                level.setBlock(pos, state.setValue(PART, ContainerPart.NONE), 0);
            }
        }
    }

    @Override
    public boolean canPlaceLiquid(BlockGetter getter, BlockPos pos, BlockState state, Fluid fluid) {
        return waterloggedIn && state.hasProperty(BlockStateProperties.WATERLOGGED)
                && SimpleWaterloggedBlock.super.canPlaceLiquid(getter, pos, state, fluid);
    }

    @Override
    public ItemStack pickupBlock(net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state) {
        if (!waterloggedIn || !state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return ItemStack.EMPTY;
        }
        return SimpleWaterloggedBlock.super.pickupBlock(level, pos, state);
    }

    @Override
    public net.minecraft.world.level.material.FluidState getFluidState(BlockState state) {
        return waterloggedIn && state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED)
                        ? Fluids.WATER.getSource(false)
                        : super.getFluidState(state);
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
        builder1.add(BlockStateProperties.OPEN, BlockStateProperties.FACING, PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 基础状态：朝向 + 未连接
        BlockState state = this.defaultBlockState()
                .setValue(BlockStateProperties.FACING, facing.getDirection(context))
                .setValue(PART, ContainerPart.NONE)
                .setValue(BlockStateProperties.OPEN, false);

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
                        && neighborState.getValue(PART) == ContainerPart.NONE) {
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
                        && neighborState.getValue(PART) == ContainerPart.NONE) {
                    // 自动扫描时，将 dir 作为“虚拟点击面”传入
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
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState,
            boolean isMoving) {

        if (!oldState.is(newState.getBlock())) {
            BlockEntity blockentity = level.getBlockEntity(pos);
            if (blockentity instanceof Container) {
                Containers.dropContents(level, pos, (Container) blockentity);
                level.updateNeighbourForOutputSignal(pos, this);
            }

            super.onRemove(oldState, level, pos, newState, isMoving);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        ContainerPart part = state.getValue(PART);
        if (this.waterloggedIn && state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (part == ContainerPart.NONE)
            return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        if (direction == part.getWorldDirection(state.getValue(BlockStateProperties.FACING)).getOpposite())
            if (neighborState.getBlock() instanceof FlexBarrelBlock flex) {
                var neiPart = neighborState.getValue(PART);
                var neiFacing = neighborState.getValue(BlockStateProperties.FACING);
                if (neiPart.getWorldDirection(neiFacing) != direction) {
                    return super.updateShape(state, direction, neighborState, level, pos, neighborPos)
                            .setValue(PART, ContainerPart.NONE);
                }
            } else
                return super.updateShape(state, direction, neighborState, level, pos, neighborPos)
                        .setValue(PART, ContainerPart.NONE);
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    public BlockState disconnect(LevelAccessor level, BlockPos pos, BlockState state) {

        ContainerPart part = state.getValue(PART);
        if (!part.isConnected())
            return state;

        Direction facing = state.getValue(BlockStateProperties.FACING);
        Direction neighborDir = part.getWorldDirection(facing).getOpposite();
        if (neighborDir != null) {
            BlockPos neighborPos = pos.relative(neighborDir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (isConnectableBlock(neighborState) && neighborState.getValue(PART).isConnected()) {
                // 清除邻居的连接状态
                level.setBlock(neighborPos, neighborState.setValue(PART, ContainerPart.NONE), 3);
            }
        }
        return state.setValue(PART, ContainerPart.NONE);
    }
    // endregion

    public static class FlexBarrelBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
        private NonNullList<ItemStack> items;
        private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
            private boolean shouldPlaySound(Level level, BlockPos pos, BlockState state) {
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

            protected void onOpen(Level level, BlockPos pos, BlockState state) {
                if (shouldPlaySound(level, pos, state))
                    FlexBarrelBlockEntity.this.playSound(state, flexBlock.open);
                FlexBarrelBlockEntity.this.updateBlockState(state, true);
            }

            protected void onClose(Level level, BlockPos pos, BlockState state) {
                if (shouldPlaySound(level, pos, state))
                    FlexBarrelBlockEntity.this.playSound(state, flexBlock.close);
                FlexBarrelBlockEntity.this.updateBlockState(state, false);
            }

            protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
            }

            protected boolean isOwnContainer(Player player) {
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

        public FlexBarrelBlockEntity(BlockPos pos, BlockState state) {
            super(MinecraftPlugin.BARREL_TILE.get(), pos, state);
            if (state.getBlock() instanceof FlexBarrelBlock flexBlock) {
                this.items = NonNullList.withSize(flexBlock.containerSize, ItemStack.EMPTY);
                this.flexBlock = flexBlock;
            } else
                throw new IllegalArgumentException("Not a FlexBarrelBlock");
        }

        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (!this.trySaveLootTable(tag)) {
                ContainerHelper.saveAllItems(tag, this.items);
            }

        }

        public void load(CompoundTag tag) {
            super.load(tag);
            this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
            if (!this.tryLoadLootTable(tag)) {
                ContainerHelper.loadAllItems(tag, this.items);
            }
        }

        @Override
        public int getContainerSize() {
            return flexBlock.containerSize;
        }

        @Override
        public NonNullList<ItemStack> getItems() {
            return this.items;
        }

        @Override
        public void setItems(NonNullList<ItemStack> items) {
            this.items = items;
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
        public void startOpen(Player player) {
            if (!this.remove && !player.isSpectator()) {
                this.openersCounter.incrementOpeners(player, this.getLevel(), this.getBlockPos(),
                        this.getBlockState());
            }
        }

        @Override
        public void stopOpen(Player player) {
            if (!this.remove && !player.isSpectator()) {
                this.openersCounter.decrementOpeners(player, this.getLevel(), this.getBlockPos(),
                        this.getBlockState());
            }
        }

        private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandlerModifiable> itemHandler;

        @Override
        public void setBlockState(BlockState state) {
            super.setBlockState(state);
            if (this.itemHandler != null) {
                net.minecraftforge.common.util.LazyOptional<?> oldHandler = this.itemHandler;
                this.itemHandler = null;
                oldHandler.invalidate();
            }
        }

        @Override
        public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
                net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
            if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && !this.remove) {
                if (this.itemHandler == null) {
                    this.itemHandler = net.minecraftforge.common.util.LazyOptional.of(this::createHandler);
                }
                return this.itemHandler.cast();
            }
            return super.getCapability(cap, side);
        }

        private net.minecraftforge.items.IItemHandlerModifiable createHandler() {
            return new net.minecraftforge.items.wrapper.InvWrapper(
                    MultiContainer.of(flexBlock.getContainers(getLevel(), getBlockPos(), getBlockState())));
        }

        @Override
        public void invalidateCaps() {
            super.invalidateCaps();
            if (itemHandler != null) {
                itemHandler.invalidate();
                itemHandler = null;
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
            Vec3i vec3i = state.getValue(BlockStateProperties.FACING).getNormal();
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
                    this.level.random.nextFloat() * 0.1F + 0.9F);
        }

        @Override
        public int[] getSlotsForFace(Direction direction) {
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
            if (!stack.getItem().canFitInsideContainerItems()) {
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
}
