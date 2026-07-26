package qikahome.jsonmore.minecraft;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.lang3.mutable.MutableObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.gigaherz.jsonthings.things.parsers.ThingParseException;
import dev.gigaherz.jsonthings.things.serializers.FlexBlockType;
import dev.gigaherz.jsonthings.things.serializers.FlexItemType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import qikahome.jsonmore.Utils;
import qikahome.jsonmore.lib.BlockedDirection;
import qikahome.jsonmore.lib.ContainerScreenType;
import qikahome.jsonmore.lib.ExpandableMode;
import qikahome.jsonmore.lib.FaceFilter;
import qikahome.jsonmore.lib.ItemFilter;
import qikahome.jsonmore.lib.KeepInventoryMode;
import qikahome.jsonmore.lib.PlacingDirections;
import qikahome.jsonmore.minecraft.FlexBarrelBlock.FlexBarrelBlockEntity;
import qikahome.jsonmore.minecraft.StorageConnectorBlock.ControllerBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WoodType;

public class MinecraftPlugin {
    public static void load() {
        FlexItemType.register("jsonmore:sign", data -> {
            String signName = data.get("standing_sign").getAsString();
            String wallSignName = data.get("wall_sign").getAsString();
            boolean useBlockName = GsonHelper.getAsBoolean(data, "use_block_name", true);
            return (props, builder) -> {
                ResourceLocation blockName = new ResourceLocation(signName);
                ResourceLocation wallBlockName = new ResourceLocation(wallSignName);
                return new FlexSignItem(RegistryObject.create(blockName, ForgeRegistries.BLOCKS),
                        RegistryObject.create(wallBlockName, ForgeRegistries.BLOCKS), useBlockName,
                        props, builder);
            };
        });
        FlexBlockType.register("jsonmore:wall_sign", data -> {
            var blockSetType = new MutableObject<ResourceLocation>();
            var woodTypeName = Utils.getOrInfo(() -> data.get("wood_type").getAsString(), "oak");
            blockSetType.setValue(new ResourceLocation(woodTypeName));
            return (props, builder) -> {
                List<Property<?>> _properties = builder.getProperties();
                Map<Property<?>, Comparable<?>> propertyDefaultValues = builder.getPropertyDefaultValues();
                var woodType = WoodType.values().filter(w -> Objects.equals(w.name(), woodTypeName)).findFirst()
                        .orElseThrow(() -> new ThingParseException("Error parsing block "
                                + builder.getRegistryName().toString() + ": wood type not found " + woodTypeName));
                return new FlexWallSignBlock(props, woodType, propertyDefaultValues) {
                    @Override
                    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder1) {
                        super.createBlockStateDefinition(builder1);
                                  for (Property<?> property : _properties) {
                            try {
                                builder1.add(property);
                            } catch (IllegalArgumentException e) {
                                //pass
                            }
                        }
                    }
                };
            };
        }, "cutout", false, false, false);
        FlexBlockType.register("jsonmore:standing_sign", data -> {
            var blockSetType = new MutableObject<ResourceLocation>();
            var woodTypeName = Utils.getOrInfo(() -> data.get("wood_type").getAsString(), "oak");
            blockSetType.setValue(new ResourceLocation(woodTypeName));
            return (props, builder) -> {
                List<Property<?>> _properties = builder.getProperties();
                Map<Property<?>, Comparable<?>> propertyDefaultValues = builder.getPropertyDefaultValues();
                var woodType = WoodType.values().filter(w -> Objects.equals(w.name(), woodTypeName)).findFirst()
                        .orElseThrow(() -> new ThingParseException("Error parsing block "
                                + builder.getRegistryName().toString() + ": wood type not found " + woodTypeName));
                return new FlexStandingSignBlock(props, woodType, propertyDefaultValues) {
                    @Override
                    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder1) {
                        super.createBlockStateDefinition(builder1);
                        for (Property<?> property : _properties) {
                            try {
                                builder1.add(property);
                            } catch (IllegalArgumentException e) {
                                throw new ThingParseException("Property " + property + " not found", e);
                            }
                        }
                    }
                };
            };
        }, "cutout", false, false, false);
        FlexBlockType.register("jsonmore:container", data -> {
            int slots = GsonHelper.getAsInt(data, "slots", 27);
            ResourceLocation openSound = new ResourceLocation(
                    GsonHelper.getAsString(data, "open_sound", "none:none"));
            ResourceLocation closeSound = new ResourceLocation(
                    GsonHelper.getAsString(data, "close_sound", "none:none"));
            boolean waterlogged = GsonHelper.getAsBoolean(data, "can_waterlogged", false);
            String facing = GsonHelper.getAsString(data, "directions", "facing_horizontal");
            String keepInventoryStr = GsonHelper.getAsString(data, "keep_inventory", "never");
            boolean angerPiglins = GsonHelper.getAsBoolean(data, "anger_piglins", false);
            String blockedStr = GsonHelper.getAsString(data, "blocked", "never");
            Map<FaceFilter, ItemFilter> insertFilters = parseFaceFilterMap(data, "insert_filters");
            Map<FaceFilter, ItemFilter> extractFilters = parseFaceFilterMap(data, "extract_filters");
            ContainerScreenType screenType = ContainerScreenType.parse(data.get("screen"), "jsonmore:chest");
            ContainerScreenType connectedScreenType = data.has("connected_screen")
                    ? ContainerScreenType.parse(data.get("connected_screen"))
                    : null;
            Set<ExpandableMode> expandableModes;
            if (data.has("expandable")) {
                Set<ExpandableMode> modes = new LinkedHashSet<>();
                for (com.google.gson.JsonElement element : GsonHelper.getAsJsonArray(data, "expandable")) {
                    String mode = element.getAsString();
                    try {
                        modes.add(ExpandableMode.valueOf(mode.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        throw new ThingParseException("Invalid expandable mode: " + mode, e);
                    }
                }
                expandableModes = modes;
            } else {
                expandableModes = Collections.emptySet();
            }
            KeepInventoryMode keepInventoryMode;
            try {
                keepInventoryMode = KeepInventoryMode.valueOf(keepInventoryStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ThingParseException("Keep inventory mode " + keepInventoryStr + " not found", e);
            }
            BlockedDirection blockedDirection;
            try {
                blockedDirection = BlockedDirection.valueOf(blockedStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ThingParseException("Blocked direction " + blockedStr + " not found", e);
            }
            ItemFilter placeFilter;
            if (!data.has("place_filter")) {
                if (keepInventoryMode != KeepInventoryMode.NEVER) {
                    placeFilter = FlexBarrelBlock.DEFAULT_PLACE_FILTER;
                } else {
                    placeFilter = ItemFilter.EMPTY;
                }
            } else {
                try {
                    placeFilter = ItemFilter.parse(data.get("place_filter"));
                } catch (IllegalArgumentException e) {
                    throw new ThingParseException("Invalid place_filter", e);
                }
            }
            insertFilters.put(FaceFilter.ANY, placeFilter);
            ResourceLocation connectableContainers = new ResourceLocation(
                    GsonHelper.getAsString(data, "connectable", "none:none"));
            return (props, builder) -> {
                List<Property<?>> _properties = builder.getProperties();
                Map<Property<?>, Comparable<?>> propertyDefaultValues = builder.getPropertyDefaultValues();
                SoundEvent openSoundEvent = ForgeRegistries.SOUND_EVENTS.getValue(openSound);
                SoundEvent closeSoundEvent = ForgeRegistries.SOUND_EVENTS.getValue(closeSound);
                PlacingDirections facingDirection;
                try {
                    facingDirection = PlacingDirections.valueOf(facing.toUpperCase());
                                } catch (IllegalArgumentException e) {
                    throw new ThingParseException("Direction " + facing + " not found", e);
                }
                if (openSoundEvent == null && !openSound.toString().equals("none:none")
                        || closeSoundEvent == null && !closeSound.toString().equals("none:none"))
                    throw new ThingParseException("Sound event " + openSound + " or " + closeSound + " not found");
                return new FlexBarrelBlock(props, propertyDefaultValues, slots, openSoundEvent, closeSoundEvent,
                        waterlogged, facingDirection, keepInventoryMode, angerPiglins, blockedDirection,
                        insertFilters, extractFilters, screenType, connectedScreenType, expandableModes,
                        connectableContainers) {
                    @Override
                    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder1) {
                        super.createBlockStateDefinition(builder1);
                        for (Property<?> property : _properties) {
                            try {
                                builder1.add(property);
                            } catch (IllegalArgumentException e) {
                                //pass
                            }
                        }
                        if (waterlogged) {
                            builder1.add(BlockStateProperties.WATERLOGGED);
                        }
                    }
                };
            };
        }, "solid", false, false, false);

        // ========== Storage Connector ==========
        FlexBlockType.register("jsonmore:storage_connector", data -> {
            int radius = GsonHelper.getAsInt(data, "radius", 4);
            if (!data.has("connectable")) {
                throw new ThingParseException("storage_connector requires 'connectable' field");
            }
            String connectableStr = GsonHelper.getAsString(data, "connectable");
            if (connectableStr.startsWith("#")) connectableStr = connectableStr.substring(1);
            ResourceLocation connectable = new ResourceLocation(connectableStr);
            ContainerScreenType screenType = ContainerScreenType.parse(data.get("screen"), "autosizedgui:auto");

            ResourceLocation assembleSoundId = new ResourceLocation(
                    GsonHelper.getAsString(data, "assemble_sound", "minecraft:block.beacon.activate"));
            ResourceLocation disassembleSoundId = new ResourceLocation(
                    GsonHelper.getAsString(data, "disassemble_sound", "minecraft:block.beacon.deactivate"));
            ResourceLocation openSoundId = new ResourceLocation(
                    GsonHelper.getAsString(data, "open_sound", "minecraft:block.barrel.open"));
            ResourceLocation closeSoundId = new ResourceLocation(
                    GsonHelper.getAsString(data, "close_sound", "minecraft:block.barrel.close"));

            return (props, builder) -> {
                List<Property<?>> _properties = builder.getProperties();
                Map<Property<?>, Comparable<?>> propertyDefaultValues = builder.getPropertyDefaultValues();
                SoundEvent assembleSound = ForgeRegistries.SOUND_EVENTS.getValue(assembleSoundId);
                SoundEvent disassembleSound = ForgeRegistries.SOUND_EVENTS.getValue(disassembleSoundId);
                SoundEvent openSound = ForgeRegistries.SOUND_EVENTS.getValue(openSoundId);
                SoundEvent closeSound = ForgeRegistries.SOUND_EVENTS.getValue(closeSoundId);
                return new StorageConnectorBlock(props, propertyDefaultValues, radius, connectable, screenType,
                        assembleSound, disassembleSound, openSound, closeSound) {
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
        }, "solid", false, false, false);
    }

    private static Map<FaceFilter, ItemFilter> parseFaceFilterMap(JsonObject data, String key) {
        if (!data.has(key)) {
            return new HashMap<>();
        }
        JsonObject obj = data.getAsJsonObject(key);
        Map<FaceFilter, ItemFilter> filters = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            FaceFilter faceFilter = FaceFilter.fromString(entry.getKey());
            if (faceFilter != null) {
                ItemFilter filter = ItemFilter.parse(entry.getValue());
                filters.put(faceFilter, filter);
            }
        }
        return filters;
    }

    public static RegistryObject<BlockEntityType<FlexBarrelBlockEntity>> BARREL_TILE;
    public static final Supplier<BlockEntityType<FlexBarrelBlockEntity>> BARREL_SUPPLIER = () -> BlockEntityType.Builder
            .of(FlexBarrelBlockEntity::new)
            .build(null);

    public static RegistryObject<BlockEntityType<ControllerBlockEntity>> STORAGE_CONNECTOR_TILE;
    public static final Supplier<BlockEntityType<ControllerBlockEntity>> STORAGE_CONNECTOR_SUPPLIER = () -> BlockEntityType.Builder
            .of(ControllerBlockEntity::new)
            .build(null);
}
