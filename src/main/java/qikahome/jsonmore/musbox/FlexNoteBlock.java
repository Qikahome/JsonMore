package qikahome.jsonmore.musbox;

import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;

import dev.gigaherz.jsonthings.things.IFlexBlock;
import dev.gigaherz.jsonthings.things.events.FlexEventContext;
import dev.gigaherz.jsonthings.things.events.FlexEventHandler;
import dev.gigaherz.jsonthings.things.events.FlexEventType;
import dev.gigaherz.jsonthings.things.shapes.DynamicShape;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import qikahome.anvil_musbox.block.ExtendNoteBlock;

public class FlexNoteBlock extends ExtendNoteBlock implements IFlexBlock {
    public FlexNoteBlock(Properties props, Map<Property<?>, Comparable<?>> propertyDefaultValues, TagKey<Block> tag,
            String instrumentName, SoundEvent soundEvent, float volume) {
        super(props);
        initializeFlex(propertyDefaultValues);
        this.TAG = tag;
        this.instrumentName = instrumentName;
        this.soundEvent = soundEvent;
        this.volume = volume;
    }

    public final TagKey<Block> TAG;
    public final String instrumentName;
    public final SoundEvent soundEvent;
    public final float volume;

    protected void playSound(Level level, BlockPos pos, float pitch) {
        level.playSound((Player) null, pos, soundEvent, SoundSource.RECORDS, volume, pitch);
    }

    public Boolean blockMatches(BlockState below) {
        return below.is(TAG);
    }

    public String getInstrumentName() {
        return instrumentName;
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
                .withRayTrace(hitResult), () -> super.useWithoutItem(state, level, pos, player, hitResult));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        return runEvent(FlexEventType.USE_BLOCK_WITH_ITEM, FlexEventContext.of(level, pos, state)
                .with(FlexEventContext.USER, player)
                .withRayTrace(hitResult), () -> super.useItemOn(stack, state, level, pos, player, hand, hitResult));
    }

    // endregion
}