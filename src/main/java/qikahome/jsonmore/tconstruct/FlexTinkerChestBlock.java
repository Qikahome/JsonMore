package qikahome.jsonmore.tconstruct;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;

import dev.gigaherz.jsonthings.things.IFlexBlock;
import dev.gigaherz.jsonthings.things.events.FlexEventContext;
import dev.gigaherz.jsonthings.things.events.FlexEventHandler;
import dev.gigaherz.jsonthings.things.events.FlexEventResult;
import dev.gigaherz.jsonthings.things.shapes.DynamicShape;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import slimeknights.tconstruct.tables.block.ChestBlock;

public class FlexTinkerChestBlock extends ChestBlock implements IFlexBlock {

    public FlexTinkerChestBlock(Properties properties, BlockEntitySupplier<? extends BlockEntity> be,
            boolean dropsItems, Map<Property<?>, Comparable<?>> propertyDefaultValues, ChestItemHandlerHelper helper) {
        super(properties, be, dropsItems);
        initializeFlex(propertyDefaultValues);
        this.helper = helper;
    }

    public static FlexTinkerChestBlock getRegisterSupplier() {
        return new FlexTinkerChestBlock(Properties.of(), FlexTinkerChestBlockEntity::new, true, Maps.newHashMap(),
                null);
    }

    public final ChestItemHandlerHelper helper;

    @Override
    public MutableComponent getName() {
        if (helper != null) {
            return Component.translatable(helper.defaultName);
        }
        return Component.translatable(this.getDescriptionId());
    }

    public static class ChestItemHandlerHelper {
        public final String defaultName;
        public final String slotMode;
        public final int maxSlots;
        public final int slotStackLimit;
        public final boolean allowDuplicateItem;
        public final List<String> filters;

        public ChestItemHandlerHelper(String defaultName, String slotMode, int maxSlots,
                int slotStackLimit, boolean allowDuplicateItem, List<String> filters) {
            this.defaultName = defaultName;
            this.slotMode = slotMode;
            this.maxSlots = maxSlots;
            this.slotStackLimit = slotStackLimit;
            this.allowDuplicateItem = allowDuplicateItem;
            this.filters = filters;
        }
    }

    // region IFlexBlock
    private DynamicShape generalShape;
    private DynamicShape collisionShape;
    private DynamicShape raytraceShape;
    private DynamicShape renderShape;
    private final Map<String, FlexEventHandler> eventHandlers = Maps.newHashMap();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void initializeFlex(Map<Property<?>, Comparable<?>> propertyDefaultValues) {
        if (propertyDefaultValues.size() > 0) {
            BlockState def = getStateDefinition().any();
            for (Map.Entry<Property<?>, Comparable<?>> entry : propertyDefaultValues.entrySet()) {
                Property prop = entry.getKey();
                Comparable value = entry.getValue();
                def = def.setValue(prop, value);
            }

            registerDefaultState(def);
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
                .withRayTrace(hit), () -> FlexEventResult.of(super.use(state, worldIn, pos, player, handIn, hit)))
                .result();
    }

    // endregion
}
