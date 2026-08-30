package qikahome.jsonmore.minecraft;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import dev.gigaherz.jsonthings.things.parsers.ThingParseException;
import dev.gigaherz.jsonthings.things.serializers.FlexBlockType;
import dev.gigaherz.jsonthings.things.serializers.FlexBlockType.DefaultTypeProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.registries.DeferredHolder;
import qikahome.jsonmore.lib.BlockedDirection;
import qikahome.jsonmore.lib.ContainerPart;
import qikahome.jsonmore.lib.ContainerScreenType;
import qikahome.jsonmore.lib.ExpandableMode;
import qikahome.jsonmore.lib.FaceFilter;
import qikahome.jsonmore.lib.ItemFilter;
import qikahome.jsonmore.lib.KeepInventoryMode;
import qikahome.jsonmore.lib.PlacingDirections;
import qikahome.jsonmore.minecraft.FlexBarrelBlock.FlexBarrelBlockEntity;
import qikahome.jsonmore.minecraft.StorageConnectorBlock.ControllerBlockEntity;

public class MinecraftPlugin {
    public static void load() {
        FlexBlockType.register("jsonmore:container", data -> {
            int slots = GsonHelper.getAsInt(data, "slots", 27);
            Identifier openSound = Identifier.parse(
                    GsonHelper.getAsString(data, "open_sound", "none:none"));
            Identifier closeSound = Identifier.parse(
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
            Identifier connectableContainers = Identifier.parse(
                    GsonHelper.getAsString(data, "connectable", "none:none"));
            return (props, builder) -> {
                List<Property<?>> _properties = builder.getProperties();
                Map<Property<?>, Comparable<?>> propertyDefaultValues = builder.getPropertyDefaultValues();
                SoundEvent openSoundEvent = BuiltInRegistries.SOUND_EVENT.getOptional(openSound).orElse(null);
                SoundEvent closeSoundEvent = BuiltInRegistries.SOUND_EVENT.getOptional(closeSound).orElse(null);
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
                        for (var prop : _properties) {
                            if (prop != BlockStateProperties.OPEN && prop != BlockStateProperties.FACING
                                    && prop != ContainerPart.PART && prop != BlockStateProperties.WATERLOGGED)
                                builder1.add(prop);
                        }
                        if (waterlogged) {
                            builder1.add(BlockStateProperties.WATERLOGGED);
                        }
                    }
                };
            };
        }, DefaultTypeProperties.builder().defaultSeeThrough(false).defaultReplaceable(false)
                .stockProperties(BlockStateProperties.OPEN, BlockStateProperties.FACING, ContainerPart.PART,
                        BlockStateProperties.WATERLOGGED));

        // ========== Storage Connector ==========
        FlexBlockType.register("jsonmore:storage_connector", data -> {
            int radius = GsonHelper.getAsInt(data, "radius", 4);
            if (!data.has("connectable")) {
                throw new ThingParseException("storage_connector requires 'connectable' field");
            }
            String connectableStr = GsonHelper.getAsString(data, "connectable");
            if (connectableStr.startsWith("#")) connectableStr = connectableStr.substring(1);
            Identifier connectable = Identifier.parse(connectableStr);
            ContainerScreenType screenType = ContainerScreenType.parse(data.get("screen"), "autosizedgui:auto");

            Identifier assembleSoundId = Identifier.parse(
                    GsonHelper.getAsString(data, "assemble_sound", "minecraft:block.beacon.activate"));
            Identifier disassembleSoundId = Identifier.parse(
                    GsonHelper.getAsString(data, "disassemble_sound", "minecraft:block.beacon.deactivate"));
            Identifier openSoundId = Identifier.parse(
                    GsonHelper.getAsString(data, "open_sound", "minecraft:block.barrel.open"));
            Identifier closeSoundId = Identifier.parse(
                    GsonHelper.getAsString(data, "close_sound", "minecraft:block.barrel.close"));

            return (props, builder) -> {
                List<Property<?>> _properties = builder.getProperties();
                Map<Property<?>, Comparable<?>> propertyDefaultValues = builder.getPropertyDefaultValues();
                SoundEvent assembleSound = BuiltInRegistries.SOUND_EVENT.getOptional(assembleSoundId).orElse(null);
                SoundEvent disassembleSound = BuiltInRegistries.SOUND_EVENT.getOptional(disassembleSoundId).orElse(null);
                SoundEvent openSound = BuiltInRegistries.SOUND_EVENT.getOptional(openSoundId).orElse(null);
                SoundEvent closeSound = BuiltInRegistries.SOUND_EVENT.getOptional(closeSoundId).orElse(null);
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
        }, DefaultTypeProperties.builder().defaultSeeThrough(false).defaultReplaceable(false));
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

    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<FlexBarrelBlockEntity>> BARREL_TILE;
    public static final Supplier<BlockEntityType<FlexBarrelBlockEntity>> BARREL_SUPPLIER = () -> new BlockEntityType<>(FlexBarrelBlockEntity::new,Set.of());

    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<ControllerBlockEntity>> STORAGE_CONNECTOR_TILE;
    public static final Supplier<BlockEntityType<ControllerBlockEntity>> STORAGE_CONNECTOR_SUPPLIER = () -> new BlockEntityType<>(ControllerBlockEntity::new,Set.of());
}
