package qikahome.jsonmore.lib.recipe;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import net.minecraft.core.registries.Registries;
import qikahome.jsonmore.lib.ingredient.SelfConsumingIngredient;

@Mod.EventBusSubscriber
public class ItemApplicationRecipe implements Recipe<RecipeWrapper> {
    public static final ResourceLocation TYPE_ID = new ResourceLocation("jsonmore:item_application");
    public static final RecipeType<ItemApplicationRecipe> TYPE = new RecipeType<>() {
        @Override
        public String toString() {
            return "jsonmore:item_application";
        }
    };
    public static final TagKey<Item> TOOL_TAG = TagKey.create(Registries.ITEM,
            new ResourceLocation("jsonmore:item_application_tool"));

    private final ResourceLocation id;
    private final Ingredient block;
    private final Ingredient tool;
    private final ItemStack result;
    private final boolean dropContainer;
    private final boolean keepBlockState;
    private final boolean updateBlock;
    @Nullable
    private final Boolean sneaking;

    public ItemApplicationRecipe(ResourceLocation id, Ingredient block, Ingredient tool, ItemStack result,
            boolean dropContainer, boolean keepBlockState, boolean updateBlock, @Nullable Boolean sneaking) {
        this.id = id;
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

    public ItemStack getRecipeResult() {
        return result;
    }

    @Override
    public boolean matches(RecipeWrapper inv, Level level) {
        return block.test(inv.getItem(0)) && tool.test(inv.getItem(1));
    }

    @Override
    public ItemStack assemble(RecipeWrapper inv, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
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

        for (var recipe : level.getRecipeManager().getAllRecipesFor(TYPE)) {
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

            recipe.apply(level, pos, player, event.getHand());
            return;
        }
    }

    public void apply(Level level, BlockPos pos, Player player, InteractionHand hand) {
        ItemStack primaryResult = result.copy();
        ItemStack heldItem = player.getItemInHand(hand);
        BlockState oldState = level.getBlockState(pos);

        ItemStack blockStack = new ItemStack(oldState.getBlock().asItem());
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            blockStack.addTagElement("BlockEntityTag", blockEntity.saveWithFullMetadata());
        }

        SelfConsumingIngredient.outputModify(tool, heldItem, primaryResult);
        SelfConsumingIngredient.outputModify(block, blockStack, primaryResult);

        if (primaryResult.getItem() instanceof BlockItem blockItem) {
            BlockState newState = blockItem.getBlock().defaultBlockState();

            if (keepBlockState) {
                newState = copyCompatibleProperties(oldState, newState);
            }

            if (!dropContainer) {
                CompoundTag oldData = null;
                BlockEntity oldBlockEntity = level.getBlockEntity(pos);
                if (oldBlockEntity != null) {
                    oldData = oldBlockEntity.saveWithFullMetadata();
                }
                removeBlock(level, pos, oldState);
                level.setBlock(pos, newState, updateBlock ? 3 : 2);
                if (oldData != null) {
                    BlockEntity newBlockEntity = level.getBlockEntity(pos);
                    if (newBlockEntity != null) {
                        newBlockEntity.load(oldData);
                    }
                }
            } else {
                removeBlock(level, pos, oldState);
                level.setBlock(pos, newState, updateBlock ? 3 : 2);
            }
        } else {
            removeBlock(level, pos, oldState);
            Block.popResource(level, pos, primaryResult);
        }

        if (!player.isCreative()) {
            ItemStack consumeResult = SelfConsumingIngredient.consume(tool, heldItem);
            heldItem.shrink(1);
            if (heldItem.isEmpty()) {
                player.setItemInHand(hand, consumeResult);
            } else {
                if (!consumeResult.isEmpty()) {
                    if (ItemStack.isSameItemSameTags(heldItem, consumeResult)) {
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

    private void removeBlock(Level level, BlockPos pos, BlockState oldState) {
        level.removeBlockEntity(pos);
        if (updateBlock) {
            level.destroyBlock(pos, false);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
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
        DirectionProperty srcDir = findDirectionProperty(sourceDef);
        DirectionProperty tgtDir = findDirectionProperty(target.getBlock().getStateDefinition());
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
    private static DirectionProperty findDirectionProperty(StateDefinition<Block, BlockState> def) {
        for (Property<?> property : def.getProperties()) {
            if (property instanceof DirectionProperty dirProp) {
                return dirProp;
            }
        }
        return null;
    }

    public static class Serializer implements RecipeSerializer<ItemApplicationRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public ItemApplicationRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient block = Ingredient.fromJson(json.get("block"));
            Ingredient tool = Ingredient.fromJson(json.get("tool"));
            ItemStack result = CraftingHelper.getItemStack(json.getAsJsonObject("result"), true);
            boolean dropContainer = net.minecraft.util.GsonHelper.getAsBoolean(json, "drop_container", true);
            boolean keepBlockState = net.minecraft.util.GsonHelper.getAsBoolean(json, "keep_block_state", false);
            boolean updateBlock = net.minecraft.util.GsonHelper.getAsBoolean(json, "update_block", true);
            Boolean sneaking = json.has("sneaking") ? net.minecraft.util.GsonHelper.getAsBoolean(json, "sneaking") : null;

            return new ItemApplicationRecipe(id, block, tool, result, dropContainer, keepBlockState, updateBlock, sneaking);
        }

        @Override
        public ItemApplicationRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient block = Ingredient.fromNetwork(buffer);
            Ingredient tool = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            boolean dropContainer = buffer.readBoolean();
            boolean keepBlockState = buffer.readBoolean();
            boolean updateBlock = buffer.readBoolean();
            boolean hasSneaking = buffer.readBoolean();
            Boolean sneaking = hasSneaking ? buffer.readBoolean() : null;

            return new ItemApplicationRecipe(id, block, tool, result, dropContainer, keepBlockState, updateBlock, sneaking);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ItemApplicationRecipe recipe) {
            recipe.block.toNetwork(buffer);
            recipe.tool.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeBoolean(recipe.dropContainer);
            buffer.writeBoolean(recipe.keepBlockState);
            buffer.writeBoolean(recipe.updateBlock);
            buffer.writeBoolean(recipe.sneaking != null);
            if (recipe.sneaking != null) {
                buffer.writeBoolean(recipe.sneaking);
            }
        }
    }
}
