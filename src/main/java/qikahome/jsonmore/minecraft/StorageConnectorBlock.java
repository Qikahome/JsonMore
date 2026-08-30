package qikahome.jsonmore.minecraft;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import static qikahome.jsonmore.JsonMore.LOGGER;
import qikahome.jsonmore.lib.ContainerScreenType;
import qikahome.jsonmore.lib.IFlexEntityBlock;
import qikahome.jsonmore.lib.IProtectedBlock;

public class StorageConnectorBlock extends BaseEntityBlock
        implements IFlexEntityBlock<StorageConnectorBlock.ControllerBlockEntity>, IProtectedBlock {

    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    public final int radius;
    public final TagKey<Block> connectableTag;
    public final ContainerScreenType screenType;
    public final SoundEvent soundAssemble;
    public final SoundEvent soundDisassemble;
    public final SoundEvent soundOpen;
    public final SoundEvent soundClose;

    public StorageConnectorBlock(BlockBehaviour.Properties properties,
            Map<Property<?>, Comparable<?>> propertyDefaultValues,
            int radius, Identifier connectable, ContainerScreenType screenType,
            SoundEvent soundAssemble, SoundEvent soundDisassemble,
            SoundEvent soundOpen, SoundEvent soundClose) {
        super(properties);
        initializeFlex(propertyDefaultValues);
        this.radius = radius;
        this.connectableTag = TagKey.create(Registries.BLOCK, connectable);
        this.screenType = screenType;
        this.soundAssemble = soundAssemble;
        this.soundDisassemble = soundDisassemble;
        this.soundOpen = soundOpen;
        this.soundClose = soundClose;
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
        for (Map.Entry<Property<?>, Comparable<?>> entry : propertyDefaultValues.entrySet()) {
            Property prop = entry.getKey();
            Comparable value = entry.getValue();
            def = def.setValue(prop, value);
        }
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

    // region BlockEntity
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ControllerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntityType<ControllerBlockEntity> getBlockEntityType() {
        return MinecraftPlugin.STORAGE_CONNECTOR_TILE.get();
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ControllerBlockEntity cbe) {
            return AbstractContainerMenu.getRedstoneSignalFromContainer(cbe.getControllerContainer());
        }
        return 0;
    }
    // endregion

    // region Interaction
    public InteractionResult fallbackUse(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {

        // Shift-right-click with empty hand: refresh (assemble)
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ControllerBlockEntity cbe) {
                    cbe.assemble(level, pos, state);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Open GUI if assembled
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ControllerBlockEntity cbe && cbe.isAssembled() && cbe.getContainerSize() > 0) {
            if (player instanceof ServerPlayer serverPlayer) {
                Container container = cbe.getControllerContainer();
                serverPlayer.openMenu(
                        screenType.createMenuProvider(Collections.singletonList(container),
                                container.getContainerSize()),
                        buffer -> screenType.writeAdditionalData(buffer,
                                Collections.singletonList(container), container.getContainerSize()));
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ControllerBlockEntity cbe && cbe.isAssembled()) {
            // Assembled controller: let onRemove handle disassembly, no drops.
            return state;
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public boolean maySetBlock(BlockState oldState, Level level, BlockPos pos, BlockState newState,
            @Block.UpdateFlags int updateFlags, int updateLimit) {
        if (!oldState.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ControllerBlockEntity cbe && cbe.isAssembled()) {
                if (!level.isClientSide()) {
                    // Disassemble (restores CONNECTED=false and plays sound), then cancel the removal
                    cbe.disassemble(level);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder1) {
        builder1.add(CONNECTED);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return MapCodec.unit(this);
    }

    // ============================================================
    //  BlockEntity (always controller mode)
    // ============================================================
    public static class ControllerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {

        protected NonNullList<ItemStack> items = NonNullList.create();
        private int totalSlots;
        private final List<ConnectorEntry> connectors = new java.util.ArrayList<>();
        private boolean disassembling = false;

        public ControllerBlockEntity(BlockPos pos, BlockState state) {
            super(MinecraftPlugin.STORAGE_CONNECTOR_TILE.get(), pos, state);
        }

        // ========================================================================
        //  Getters
        // ========================================================================

        public boolean isAssembled() {
            return !connectors.isEmpty();
        }

        public Container getControllerContainer() {
            return this;
        }

        // ========================================================================
        //  Assemble: scan nearby FlexBarrelBlocks, mark as captured, absorb items
        // ========================================================================

        public void assemble(Level level, BlockPos pos, BlockState state) {
            if (level.isClientSide()) return;
            if (!(state.getBlock() instanceof StorageConnectorBlock scb)) return;

            int r = scb.radius;
            List<BlockPos> targets = new java.util.ArrayList<>();
            Set<BlockPos> visited = new HashSet<>();
            Queue<BlockPos> queue = new ArrayDeque<>();

            // BFS from connector's adjacent blocks, following physical connections
            for (Direction dir : Direction.values()) {
                BlockPos p = pos.relative(dir);
                if (level.getBlockState(p).is(scb.connectableTag) && visited.add(p)) {
                    queue.add(p);
                }
            }

            while (!queue.isEmpty()) {
                BlockPos p = queue.poll();
                BlockState bs = level.getBlockState(p);

                // Not yet captured → potential target
                if (!bs.getValue(FlexBarrelBlock.CONNECTED)) {
                    targets.add(p);
                }

                // Continue BFS through neighbors if within radius
                for (Direction dir : Direction.values()) {
                    BlockPos np = p.relative(dir);
                    if (!visited.contains(np) && level.getBlockState(np).is(scb.connectableTag)) {
                        int ndist = Math.max(Math.abs(np.getX() - pos.getX()),
                                Math.max(Math.abs(np.getY() - pos.getY()),
                                        Math.abs(np.getZ() - pos.getZ())));
                        if (ndist <= r && visited.add(np)) {
                            queue.add(np);
                        }
                    }
                }
            }

            LOGGER.debug("Storage controller at {} found {} connectable blocks", pos, targets.size());

            if (targets.isEmpty()) return;

            // Ensure items list is growable (load() may have set it to fixed-size withSize())
            java.util.ArrayList<ItemStack> savedItems = new java.util.ArrayList<>(items);
            items = NonNullList.create();
            items.addAll(savedItems);

            for (BlockPos target : targets) {
                BlockEntity be = level.getBlockEntity(target);
                if (!(be instanceof Container container)) {
                    continue;
                }
                if (container instanceof ControllerBlockEntity) {
                    continue;
                }
                if (!(be instanceof FlexBarrelBlock.FlexBarrelBlockEntity fbe)) {
                    continue;
                }
                if (level.getBlockState(target).getValue(FlexBarrelBlock.CONNECTED)) {
                    continue;
                }
                if (fbe.getController() != null) {
                    continue;
                }

                int size = container.getContainerSize();
                BlockState bs = level.getBlockState(target);

                // Capture display name
                Component displayName = be instanceof net.minecraft.world.Nameable n
                        ? n.getDisplayName()
                        : bs.getBlock().getName();

                // Read all items and clear original container
                NonNullList<ItemStack> containerItems = NonNullList.withSize(size, ItemStack.EMPTY);
                int itemCount = 0;
                for (int i = 0; i < size; i++) {
                    ItemStack stack = container.getItem(i);
                    containerItems.set(i, stack);
                    if (!stack.isEmpty()) itemCount++;
                }
                LOGGER.debug("  Absorbed {} items from {} ({} slots)", itemCount, target, size);
                container.clearContent();

                // Mark FlexBarrelBlockEntity as captured (no block replacement)
                BlockPos relativePos = pos.subtract(target);
                fbe.setCaptured(true, relativePos);

                // Set CONNECTED=true on the block state
                if (bs.hasProperty(FlexBarrelBlock.CONNECTED)) {
                    level.setBlock(target, bs.setValue(FlexBarrelBlock.CONNECTED, true), 2);
                    level.invalidateCapabilities(target);
                }

                // Absorb items into controller
                int nonEmpty = 0;
                for (ItemStack stack : containerItems) {
                    if (!stack.isEmpty()) {
                        items.add(stack);
                        nonEmpty++;
                    }
                }
                int emptySlots = size - nonEmpty;
                for (int i = 0; i < emptySlots; i++) {
                    items.add(ItemStack.EMPTY);
                }

                totalSlots += size;

                connectors.add(new ConnectorEntry(relativePos, size, displayName));
            }

            if (isAssembled()) {
                // Play assemble sound
                BlockState bs2 = level.getBlockState(pos);
                level.playSound(null, pos, scb.soundAssemble, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!state.getValue(CONNECTED)) {
                    level.setBlock(pos, state.setValue(CONNECTED, true), 2);
                }
                level.invalidateCapabilities(pos);
            }
            setChanged();
            LOGGER.debug("Storage controller at {} assembled: {} items, {} slots, {} connectors",
                    pos, items.size(), totalSlots, connectors.size());
        }

        // ========================================================================
        //  Disassemble: restore captured FlexBarrelBlocks, distribute items back
        // ========================================================================

        public void disassemble(Level level) {
            if (level.isClientSide() || disassembling) return;
            disassembling = true;
            try {
                // 1. Save connector list before clearing
                var savedConnectors = new java.util.ArrayList<>(connectors);
                connectors.clear();

                // 2. Restore captured flex barrels (unmark captured, toggle CONNECTED)
                for (ConnectorEntry entry : savedConnectors) {
                    BlockPos targetPos = getBlockPos().subtract(entry.relativePos);
                    BlockEntity be = level.getBlockEntity(targetPos);
                    if (be instanceof FlexBarrelBlock.FlexBarrelBlockEntity fbe) {
                        fbe.setCaptured(false, null);
                    }
                    BlockState bs = level.getBlockState(targetPos);
                    if (bs.hasProperty(FlexBarrelBlock.CONNECTED) && bs.getValue(FlexBarrelBlock.CONNECTED)) {
                        level.setBlock(targetPos, bs.setValue(FlexBarrelBlock.CONNECTED, false), 2);
                        level.invalidateCapabilities(targetPos);
                    }
                }

                // 3. Distribute items back into restored containers in order
                int itemIndex = 0;
                for (ConnectorEntry entry : savedConnectors) {
                    BlockPos targetPos = getBlockPos().subtract(entry.relativePos);
                    BlockEntity restoredBe = level.getBlockEntity(targetPos);
                    if (restoredBe instanceof Container container) {
                        int slots = Math.min(container.getContainerSize(), entry.containerSize);
                        for (int i = 0; i < slots && itemIndex < items.size(); i++) {
                            container.setItem(i, items.get(itemIndex++));
                        }
                    }
                }

                // 4. Spill any items that don't fit back
                BlockPos thisPos = getBlockPos();
                while (itemIndex < items.size()) {
                    ItemStack stack = items.get(itemIndex++);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, thisPos.getX(), thisPos.getY(), thisPos.getZ(), stack);
                    }
                }

                // 5. Reset controller state
                items.clear();
                totalSlots = 0;

                // Play disassemble sound
                BlockPos selfPos = getBlockPos();
                BlockState currentState = level.getBlockState(selfPos);
                if (currentState.getBlock() instanceof StorageConnectorBlock scb) {
                    level.playSound(null, selfPos, scb.soundDisassemble, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                // Clear CONNECTED block state
                if (currentState.hasProperty(CONNECTED) && currentState.getValue(CONNECTED)) {
                    level.setBlock(selfPos, currentState.setValue(StorageConnectorBlock.CONNECTED, false), 2);
                }
                level.invalidateCapabilities(selfPos);

                setChanged();
            } finally {
                disassembling = false;
            }
        }

        // ========================================================================
        //  Container overrides (flat item storage)
        // ========================================================================

        @Override
        public int getContainerSize() {
            return Math.max(totalSlots, items.size());
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot < items.size()) {
                items.set(slot, stack);
            } else {
                // Grow item list
                while (items.size() < slot) {
                    items.add(ItemStack.EMPTY);
                }
                items.add(stack);
                totalSlots = Math.max(totalSlots, items.size());
            }
            setChanged();
        }

        // ========================================================================
        //  WorldlyContainer (hopper/pipe support)
        // ========================================================================

        @Override
        public int[] getSlotsForFace(Direction side) {
            int size = getContainerSize();
            int[] slots = new int[size];
            for (int i = 0; i < size; i++)
                slots[i] = i;
            return slots;
        }

        @Override
        public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
            return true;
        }

        @Override
        public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
            return true;
        }

        // ========================================================================
        //  Display Name
        // ========================================================================

        @Override
        public Component getDisplayName() {
            if (!isAssembled())
                return super.getDisplayName();

            // Count by display name
            Map<Component, Integer> nameCount = new java.util.HashMap<>();
            for (ConnectorEntry e : connectors) {
                nameCount.put(e.displayName, nameCount.getOrDefault(e.displayName, 0) + 1);
            }

            var collector = Component.literal("");
            boolean first = true;
            for (var entry : nameCount.entrySet()) {
                if (!first) collector.append(", ");
                first = false;
                if (entry.getValue() > 1)
                    collector.append(Component.literal(entry.getValue().toString() + " "));
                collector.append(entry.getKey());
            }
            return collector;
        }

        // ========================================================================
        //  NBT
        // ========================================================================

        @Override
        protected void saveAdditional(ValueOutput output) {
            ContainerHelper.saveAllItems(output, items);
            output.putInt("TotalSlots", totalSlots);
            var connectorList = output.childrenList("Connectors");
            for (ConnectorEntry e : connectors) {
                ValueOutput child = connectorList.addChild();
                child.putLong("RelPos", e.relativePos.asLong());
                child.putInt("Size", e.containerSize);
                child.storeNullable("DisplayName", ComponentSerialization.CODEC, e.displayName);
            }
        }

        @Override
        public void loadAdditional(ValueInput input) {
            totalSlots = input.getIntOr("TotalSlots", 0);
            items = NonNullList.withSize(totalSlots, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, items);
            connectors.clear();
            for (ValueInput child : input.childrenListOrEmpty("Connectors")) {
                BlockPos rel = BlockPos.of(child.getLongOr("RelPos", 0L));
                int size = child.getIntOr("Size", 0);
                Component displayName = child.read("DisplayName", ComponentSerialization.CODEC).orElse(null);
                connectors.add(new ConnectorEntry(rel, size, displayName));
            }
        }

        // ========================================================================
        //  RandomizableContainerBlockEntity abstract methods
        // ========================================================================

        @Override
        protected NonNullList<ItemStack> getItems() {
            return items;
        }

        @Override
        protected void setItems(NonNullList<ItemStack> items) {
            this.items = items;
        }

        // ========================================================================
        //  Internal data class
        // ========================================================================

        private static class ConnectorEntry {
            final BlockPos relativePos;
            final int containerSize;
            @Nullable
            final Component displayName;

            ConnectorEntry(BlockPos relativePos, int containerSize, @Nullable Component displayName) {
                this.relativePos = relativePos;
                this.containerSize = containerSize;
                this.displayName = displayName;
            }
        }

        @Override
        protected Component getDefaultName() {
            return getBlockState().getBlock().getName();
        }

        @Override
        protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
            if (getBlockState().getBlock() instanceof StorageConnectorBlock scb) {
                return scb.screenType.createMenuProvider(
                        java.util.Collections.singletonList(this), getContainerSize())
                        .createMenu(containerId, inventory, inventory.player);
            }
            return null;
        }

        @Override
        public void startOpen(ContainerUser user) {
            if (!remove && !user.getLivingEntity().isSpectator()) {
                if (level != null && getBlockState().getBlock() instanceof StorageConnectorBlock scb) {
                    level.playSound(null, worldPosition, scb.soundOpen, SoundSource.BLOCKS, 0.5F,
                            level.getRandom().nextFloat() * 0.1F + 0.9F);
                }
            }
        }

        @Override
        public void stopOpen(ContainerUser user) {
            if (!remove && !user.getLivingEntity().isSpectator()) {
                if (level != null && getBlockState().getBlock() instanceof StorageConnectorBlock scb) {
                    level.playSound(null, worldPosition, scb.soundClose, SoundSource.BLOCKS, 0.5F,
                            level.getRandom().nextFloat() * 0.1F + 0.9F);
                }
            }
        }

    }
}
