package qikahome.jsonmore.tconstruct;

import static qikahome.jsonmore.JsonMore.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import dev.gigaherz.jsonthings.things.parsers.ThingResourceManager;
import dev.gigaherz.jsonthings.things.serializers.FlexBlockType;
import dev.gigaherz.jsonthings.things.serializers.FlexItemType;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import qikahome.jsonmore.mantle.MantlePlugin;
import qikahome.jsonmore.tconstruct.FlexTinkerChestBlock.ChestItemHandlerHelper;
import qikahome.jsonmore.tconstruct.dynamic.FlexMaterialStatTypeType;
import qikahome.jsonmore.tconstruct.dynamic.MaterialStatTypeParser;
import slimeknights.tconstruct.library.client.book.sectiontransformer.FluidEffectInjectingTransformer;
import slimeknights.tconstruct.library.client.book.sectiontransformer.ModifierSectionTransformer;
import slimeknights.tconstruct.library.client.book.sectiontransformer.ModifierTagInjectorTransformer;
import slimeknights.tconstruct.library.client.book.sectiontransformer.ToolSectionTransformer;
import slimeknights.tconstruct.library.client.book.sectiontransformer.ToolTagInjectorTransformer;
import slimeknights.tconstruct.library.client.book.sectiontransformer.materials.TierRangeMaterialSectionTransformer;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;

public class TConstructPlugin {
    public static void load() {
        LOGGER.info("Loading JsonMore TConstructPlugin");
        MantlePlugin.registerTransformer(new ResourceLocation("tconstruct:tool_tag_injector"), (data, book) -> {
            book.addTransformer(ToolTagInjectorTransformer.INSTANCE);
        });
        MantlePlugin.registerTransformer(new ResourceLocation("tconstruct:modifier_tag_injector"), (data, book) -> {
            book.addTransformer(ModifierTagInjectorTransformer.INSTANCE);
        });
        MantlePlugin.registerTransformer(new ResourceLocation("tconstruct:tool_section"), (data, book) -> {
            if (data == null) {
                book.addTransformer(ToolSectionTransformer.INSTANCE);
            } else if (data.has("tool_type")) {
                String toolType = GsonHelper.getAsString(data, "tool_type");
                boolean largeTitle = GsonHelper.getAsBoolean(data, "large_title", false);
                boolean centerTitle = GsonHelper.getAsBoolean(data, "center_title", false);
                book.addTransformer(new ToolSectionTransformer(toolType, largeTitle, centerTitle));
            } else {
                LOGGER.warn(
                        "ToolSectionTransformer: object form requires 'tool_type' field, e.g. {\"id\":\"tconstruct:tool_section\", \"tool_type\":\"armor\"}. Using default.");
                book.addTransformer(ToolSectionTransformer.INSTANCE);
            }
        });
        MantlePlugin.registerTransformer(new ResourceLocation("tconstruct:modifier_section"), (data, book) -> {
            if (data == null) {
                book.addTransformer(ModifierSectionTransformer.INSTANCE);
            } else if (data.has("modifier_type")) {
                String modifierType = GsonHelper.getAsString(data, "modifier_type");
                boolean largeTitle = GsonHelper.getAsBoolean(data, "large_title", false);
                boolean centerTitle = GsonHelper.getAsBoolean(data, "center_title", false);
                book.addTransformer(new ModifierSectionTransformer(modifierType, largeTitle, centerTitle));
            } else {
                LOGGER.warn(
                        "ModifierSectionTransformer: object form requires 'modifier_type' field, e.g. {\"id\":\"tconstruct:modifier_section\", \"modifier_type\":\"armor\"}. Using default.");
                book.addTransformer(ModifierSectionTransformer.INSTANCE);
            }
        });
        MantlePlugin.registerTransformer(new ResourceLocation("tconstruct:tier_range_material_section"),
                (data, book) -> {
                    book.addTransformer(TierRangeMaterialSectionTransformer.INSTANCE);
                });
        MantlePlugin.registerTransformer(new ResourceLocation("tconstruct:fluid_effect_injector"), (data, book) -> {
            book.addTransformer(FluidEffectInjectingTransformer.INSTANCE);
        });

        FlexBlockType.register("jsonmore:fluid_tank", data -> {
            int capacity = GsonHelper.getAsInt(data, "capacity", 4000);
            return (props, builder) -> {
                props.isValidSpawn((a, b, c, d) -> false).isRedstoneConductor((a, b, c) -> false)
                        .isSuffocating((a, b, c) -> false).isViewBlocking((a, b, c) -> false).noOcclusion()
                        .lightLevel(SearedTankBlock.LIGHT_GETTER);
                List<Property<?>> _properties = builder.getProperties();
                Map<Property<?>, Comparable<?>> propertyDefaultValues = builder.getPropertyDefaultValues();
                return new FlexFluidTankBlock(props, capacity, propertyDefaultValues) {
                    @Override
                    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder1) {
                        super.createBlockStateDefinition(builder1);
                        for (Property<?> property : _properties) {
                            try {
                                builder1.add(property);
                            } catch (IllegalArgumentException e) {
                                // pass
                            }
                        }
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
                ChestItemHandlerHelper helper = new ChestItemHandlerHelper(translationKey, slotMode, maxSlots,
                        slotStackLimit, allowDuplicateItem, filters);

                return new FlexTinkerChestBlock(props, FlexTinkerChestBlockEntity::new, dropItems,
                        propertyDefaultValues, helper) {
                    @Override
                    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder1) {
                        super.createBlockStateDefinition(builder1);
                        for (Property<?> property : _properties) {
                            try {
                                builder1.add(property);
                            } catch (IllegalArgumentException e) {
                                // pass
                            }
                        }
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

        FlexMaterialStatTypeType.load();
    }

    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        parser.onCommonSetup(event);
    }

    public static MaterialStatTypeParser parser = null;
    public static Function<IEventBus, MaterialStatTypeParser> PARSER_SUPPLIER = bus -> {
        LOGGER.info("JsonMore: Start registering material stat types parser.");
        return ThingResourceManager.instance().registerParser(new MaterialStatTypeParser(bus));
    };

    public static RegistryObject<BlockEntityType<FlexTinkerChestBlockEntity>> TINKER_CHEST_TILE;
    public static final Supplier<BlockEntityType<FlexTinkerChestBlockEntity>> TINKER_CHEST_SUPPLIER = () -> BlockEntityType.Builder
            .of(FlexTinkerChestBlockEntity::new)
            .build(null);
}
