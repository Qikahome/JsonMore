package qikahome.jsonmore.tconstruct;

import static qikahome.jsonmore.JsonMore.*;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import dev.gigaherz.jsonthings.things.serializers.FlexBlockType;
import dev.gigaherz.jsonthings.things.serializers.FlexItemType;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TConstructPlugin {
    public static void load() {
        LOGGER.info("Loading JsonMore TConstructPlugin");
        FlexBlockType.register("jsonmore:fluid_tank", data -> {
            int capacity = GsonHelper.getAsInt(data, "capacity", 4000);
            return (props, builder) -> {
                List<Property<?>> _properties = builder.getProperties();
                Map<Property<?>, Comparable<?>> propertyDefaultValues = builder.getPropertyDefaultValues();
                return new FlexFluidTankBlock(props, capacity, propertyDefaultValues) {
                    @Override
                    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder1) {
                        super.createBlockStateDefinition(builder1);
                        _properties.forEach(builder1::add);
                    }
                };
            };
        }, "cutout_mipped", false, false, false);

        FlexBlockType.register("jsonmore:tinker_chest", data -> {
            boolean dropItems = GsonHelper.getAsBoolean(data, "drop_items", true);
            String gotTranslationKey = GsonHelper.getAsString(data, "translation_key", null);
            String slotMode = GsonHelper.getAsString(data, "slot_mode", "scaling");
            int maxSlots = GsonHelper.getAsInt(data, "max_slots", 27);
            int slotStackLimit = GsonHelper.getAsInt(data, "slot_stack_limit", 64);
            boolean allowDuplicateItem = GsonHelper.getAsBoolean(data, "allow_duplicate_item", false);
            List<String> filters = NonNullList.create();
            if (data.has("filters")) {
                data.get("filters").getAsJsonArray().asList().stream().map(e -> e.getAsString()).forEach(filters::add);
            }
            return (props, builder) -> {
                List<Property<?>> _properties = builder.getProperties();
                Map<Property<?>, Comparable<?>> propertyDefaultValues = builder.getPropertyDefaultValues();
                String translationKey = gotTranslationKey == null
                        ? "block." + builder.getRegistryName().toString().replace(":", ".")
                        : gotTranslationKey;
                BlockEntitySupplier<FlexTinkerChestBlockEntity> be = FlexTinkerChestBlockEntity
                        .getSupplier(translationKey, slotMode, maxSlots, slotStackLimit, allowDuplicateItem, filters);

                return new FlexTinkerChestBlock(props, be, dropItems, propertyDefaultValues) {
                    @Override
                    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder1) {
                        super.createBlockStateDefinition(builder1);
                        _properties.forEach(builder1::add);
                    }
                };
            };
        }, "cutout_mipped", false, false, false);

        FlexItemType.register("jsonmore:fluid_tank", data -> {
            final String name = GsonHelper.getAsString(data, "places", null);
            boolean useBlockName = GsonHelper.getAsBoolean(data, "use_block_name", true);
            boolean limitStackSize = GsonHelper.getAsBoolean(data, "limit_stack_size", true);
            return (props, builder) -> {
                ResourceLocation blockName = name != null ? new ResourceLocation(name) : builder.getRegistryName();
                return new FlexFluidTankItem(RegistryObject.create(blockName, ForgeRegistries.BLOCKS), useBlockName,
                        props, limitStackSize, builder);
            };
        });
        FlexItemType.register("jsonmore:copper_can", data -> {
            final int capacity = GsonHelper.getAsInt(data, "capacity", 90);
            return (props, builder) -> {
                return new FlexCanItem(props, builder, capacity);
            };
        });
    }

    public static RegistryObject<BlockEntityType<FlexTinkerChestBlockEntity>> TINKER_CHEST_TILE;
    public static final Supplier<BlockEntityType<FlexTinkerChestBlockEntity>> TINKER_CHEST_SUPPLIER = () -> BlockEntityType.Builder
            .of(FlexTinkerChestBlockEntity::new)
            .build(null);
}
