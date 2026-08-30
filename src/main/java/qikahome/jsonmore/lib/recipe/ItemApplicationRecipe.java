package qikahome.jsonmore.lib.recipe;

import java.util.Optional;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import qikahome.jsonmore.lib.ingredient.SelfConsumingIngredient;

@EventBusSubscriber(modid = "jsonmore")
public class ItemApplicationRecipe implements Recipe<RecipeInput> {
    public static final Identifier TYPE_ID = Identifier.parse("jsonmore:item_application");
    public static final RecipeType<ItemApplicationRecipe> TYPE = RecipeType.simple(TYPE_ID);
    public static final TagKey<Item> TOOL_TAG = TagKey.create(Registries.ITEM,
            Identifier.parse("jsonmore:item_application_tool"));

    private final Ingredient block;
    private final Ingredient tool;
    private final ItemStackTemplate result;
    private final boolean dropContainer;
    private final boolean keepBlockState;
    private final boolean updateBlock;
    @Nullable
    private final Boolean sneaking;

    public ItemApplicationRecipe(Ingredient block, Ingredient tool, ItemStackTemplate result,
            boolean dropContainer, boolean keepBlockState, boolean updateBlock, @Nullable Boolean sneaking) {
        this.block = block;
        this.tool = tool;
        this.result = result;
        this.dropContainer = dropContainer;
        this.keepBlockState = keepBlockState;
        this.updateBlock = updateBlock;
        this.sneaking = sneaking;
    }

    public boolean testBlock(BlockState state) {
        return block.test(new ItemStack(state.getBlock().asItem()));
    }

    public boolean testTool(ItemStack stack) {
        return tool.test(stack);
    }

    public boolean shouldDropContainer() {
        return dropContainer;
    }

    public boolean shouldKeepBlockState() {
        return keepBlockState;
    }

    public boolean shouldUpdateBlock() {
        return updateBlock;
    }

    @Nullable
    public Boolean getSneaking() {
        return sneaking;
    }

    public Ingredient getBlock() {
        return block;
    }

    public Ingredient getTool() {
        return tool;
    }

    public ItemStackTemplate getRecipeResult() {
        return result;
    }

    @Override
    public boolean matches(RecipeInput inv, Level level) {
        return block.test(inv.getItem(0)) && tool.test(inv.getItem(1));
    }

    @Override
    public ItemStack assemble(RecipeInput inv) {
        return result.create();
    }

    // @Override
    // public ItemStack getResultItem(HolderLookup.Provider registries) {
    //     return result;
    // }

    @Override
    public RecipeSerializer<ItemApplicationRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<ItemApplicationRecipe> getType() {
        return TYPE;
    }

    private static final RecipeBookCategory RECIPE_BOOK_CATEGORY = new RecipeBookCategory();

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RECIPE_BOOK_CATEGORY;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        ItemStack heldItem = event.getItemStack();
        BlockPos pos = event.getPos();
        BlockState blockState = level.getBlockState(pos);

        if (heldItem.isEmpty())
            return;
        if (blockState.isAir())
            return;
        if (event.isCanceled())
            return;

        if (!heldItem.is(TOOL_TAG))
            return;

        Player player = event.getEntity();

        MinecraftServer server = level.getServer();
        if (server == null)
            return;

        for (var holder : server.getRecipeManager().recipeMap().byType(TYPE)) {
            var recipe = holder.value();
            Boolean sneaking = recipe.getSneaking();
            if (sneaking != null && player.isShiftKeyDown() != sneaking)
                continue;

            if (!recipe.testBlock(blockState))
                continue;
            if (!recipe.testTool(heldItem))
                continue;

            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);

            if (level.isClientSide())
                return;

            level.playSound(null, pos, SoundEvents.COPPER_BREAK, SoundSource.PLAYERS, 1, 1.45f);

            recipe.apply((ServerLevel) level, pos, player, event.getHand());
            return;
        }
    }

    public void apply(ServerLevel level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack primaryResult = result.create();
        ItemStack heldItem = player.getItemInHand(hand);
        BlockState oldState = level.getBlockState(pos);
        var registries = level.registryAccess();

        ItemStack blockStack = new ItemStack(oldState.getBlock().asItem());
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            blockStack.set(DataComponents.BLOCK_ENTITY_DATA,
                    TypedEntityData.of(blockEntity.getType(), blockEntity.saveWithFullMetadata(registries)));
        }

        SelfConsumingIngredient.outputModify(tool, heldItem, primaryResult);
        SelfConsumingIngredient.outputModify(block, blockStack, primaryResult);

        if (primaryResult.getItem() instanceof BlockItem blockItem) {
            BlockState newState = blockItem.getBlock().defaultBlockState();

            if (keepBlockState) {
                newState = copyCompatibleProperties(oldState, newState);
            }

            if (!dropContainer) {
                BlockEntity oldBlockEntity = level.getBlockEntity(pos);
                var oldData = oldBlockEntity != null ? oldBlockEntity.saveWithFullMetadata(registries) : null;
                level.removeBlockEntity(pos);
                if (updateBlock) {
                    level.destroyBlock(pos, false);
                }
                level.setBlock(pos, newState, updateBlock ? 3 : 2);
                if (oldData != null) {
                    BlockEntity newBlockEntity = level.getBlockEntity(pos);
                    if (newBlockEntity != null) {
                        newBlockEntity.loadWithComponents(
                                TagValueInput.create(ProblemReporter.DISCARDING, registries, oldData));
                    }
                }
            } else {
                if (updateBlock) {
                    level.destroyBlock(pos, false);
                } else {
                    level.removeBlockEntity(pos);
                }
                level.setBlock(pos, newState, updateBlock ? 3 : 2);
            }
        } else {
            if (updateBlock) {
                level.destroyBlock(pos, false);
            } else {
                level.removeBlockEntity(pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            Block.popResource(level, pos, primaryResult);
        }

        if (!player.isCreative()) {
            ItemStack consumeResult = SelfConsumingIngredient.consume(tool, heldItem, level, player);
            heldItem.shrink(1);
            if (heldItem.isEmpty()) {
                player.setItemInHand(hand, consumeResult);
            } else {
                if (!consumeResult.isEmpty()) {
                    if (ItemStack.isSameItemSameComponents(heldItem, consumeResult)) {
                        int space = heldItem.getMaxStackSize() - heldItem.getCount();
                        if (space > 0) {
                            int toAdd = Math.min(space, consumeResult.getCount());
                            heldItem.grow(toAdd);
                            consumeResult.shrink(toAdd);
                        }
                    }
                    if (!consumeResult.isEmpty()) {
                        if (!player.getInventory().add(consumeResult)) {
                            player.drop(consumeResult, false);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static BlockState copyCompatibleProperties(BlockState source, BlockState target) {
        StateDefinition<Block, BlockState> sourceDef = source.getBlock().getStateDefinition();
        for (Property<?> property : sourceDef.getProperties()) {
            if (target.hasProperty(property)) {
                Comparable value = source.getValue((Property) property);
                if (property.getPossibleValues().contains(value)) {
                    target = target.setValue((Property) property, (Comparable) value);
                }
            }
        }
        EnumProperty<Direction> srcDir = findDirectionProperty(sourceDef);
        EnumProperty<Direction> tgtDir = findDirectionProperty(target.getBlock().getStateDefinition());
        if (srcDir != null && tgtDir != null && srcDir != tgtDir) {
            Direction dir = source.getValue(srcDir);
            if (tgtDir.getPossibleValues().contains(dir)) {
                target = target.setValue(tgtDir, dir);
            }
        }
        if (target.hasProperty(BlockStateProperties.OPEN)) {
            target = target.setValue(BlockStateProperties.OPEN, false);
        }
        return target;
    }

    @Nullable
    private static EnumProperty<Direction> findDirectionProperty(StateDefinition<Block, BlockState> def) {
        for (Property<?> property : def.getProperties()) {
            if (property instanceof EnumProperty<?> dirProp) {
                var values = dirProp.getPossibleValues();
                if (!values.isEmpty() && values.iterator().next() instanceof Direction) {
                    @SuppressWarnings("unchecked")
                    EnumProperty<Direction> casted = (EnumProperty<Direction>) dirProp;
                    return casted;
                }
            }
        }
        return null;
    }

    public static class Serializer{


        private static final MapCodec<ItemApplicationRecipe> CODEC = RecordCodecBuilder.mapCodec(
                inst -> inst.group(
                        Ingredient.CODEC.fieldOf("block").forGetter(r -> r.block),
                        Ingredient.CODEC.fieldOf("tool").forGetter(r -> r.tool),
                        ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
                        Codec.BOOL.optionalFieldOf("drop_container", true).forGetter(r -> r.dropContainer),
                        Codec.BOOL.optionalFieldOf("keep_block_state", false).forGetter(r -> r.keepBlockState),
                        Codec.BOOL.optionalFieldOf("update_block", true).forGetter(r -> r.updateBlock),
                        Codec.BOOL.optionalFieldOf("sneaking").forGetter(r -> Optional.ofNullable(r.sneaking)))
                        .apply(inst,
                                (block, tool, result, drop, keep, update, sneak) -> new ItemApplicationRecipe(block,
                                        tool, result, drop, keep, update, sneak.orElse(null))));

        private static final StreamCodec<RegistryFriendlyByteBuf, ItemApplicationRecipe> STREAM_CODEC = StreamCodec.of(
                Serializer::toNetwork,
                Serializer::fromNetwork);

        private static void toNetwork(RegistryFriendlyByteBuf buf, ItemApplicationRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.block);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.tool);
            ItemStackTemplate.STREAM_CODEC.encode(buf, recipe.result);
            buf.writeBoolean(recipe.dropContainer);
            buf.writeBoolean(recipe.keepBlockState);
            buf.writeBoolean(recipe.updateBlock);
            buf.writeBoolean(recipe.sneaking != null);
            if (recipe.sneaking != null) {
                buf.writeBoolean(recipe.sneaking);
            }
        }

        private static ItemApplicationRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            Ingredient block = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient tool = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            ItemStackTemplate result = ItemStackTemplate.STREAM_CODEC.decode(buf);
            boolean dropContainer = buf.readBoolean();
            boolean keepBlockState = buf.readBoolean();
            boolean updateBlock = buf.readBoolean();
            boolean hasSneaking = buf.readBoolean();
            Boolean sneaking = hasSneaking ? buf.readBoolean() : null;
            return new ItemApplicationRecipe(block, tool, result, dropContainer, keepBlockState, updateBlock, sneaking);
        }

        public static final RecipeSerializer<ItemApplicationRecipe> INSTANCE = new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }
}
