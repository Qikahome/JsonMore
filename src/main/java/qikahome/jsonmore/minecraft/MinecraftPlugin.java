package qikahome.jsonmore.minecraft;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.mutable.MutableObject;

import dev.gigaherz.jsonthings.things.parsers.ThingParseException;
import dev.gigaherz.jsonthings.things.serializers.FlexBlockType;
import dev.gigaherz.jsonthings.things.serializers.FlexItemType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import qikahome.jsonmore.Utils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
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
            var woodTypeName = Utils.getOrInfo(()->data.get("wood_type").getAsString(), "oak");
            blockSetType.setValue(new ResourceLocation(woodTypeName));
            return (props, builder) -> {
                List<Property<?>> _properties = builder.getProperties();
                Map<Property<?>, Comparable<?>> propertyDefaultValues = builder.getPropertyDefaultValues();
                var woodType = WoodType.values().filter(w -> Objects.equals(w.name(), woodTypeName)).findFirst()
                        .orElseThrow(() -> new ThingParseException("Error parsing block " + builder.getRegistryName().toString() + ": wood type not found " + woodTypeName));
                return new FlexWallSignBlock(props, woodType, propertyDefaultValues) {
                    @Override
                    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder1) {
                        super.createBlockStateDefinition(builder1);
                        _properties.forEach(builder1::add);
                    }
                };
            };
        }, "cutout", false, false, false);
        FlexBlockType.register("jsonmore:standing_sign", data -> {
            var blockSetType = new MutableObject<ResourceLocation>();
            var woodTypeName = Utils.getOrInfo(()->data.get("wood_type").getAsString(), "oak");
            blockSetType.setValue(new ResourceLocation(woodTypeName));
            return (props, builder) -> {
                List<Property<?>> _properties = builder.getProperties();
                Map<Property<?>, Comparable<?>> propertyDefaultValues = builder.getPropertyDefaultValues();
                var woodType = WoodType.values().filter(w -> Objects.equals(w.name(), woodTypeName)).findFirst()
                        .orElseThrow(() -> new ThingParseException("Error parsing block " + builder.getRegistryName().toString() + ": wood type not found " + woodTypeName));
                return new FlexStandingSignBlock(props, woodType, propertyDefaultValues) {
                    @Override
                    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder1) {
                        super.createBlockStateDefinition(builder1);
                        _properties.forEach(builder1::add);
                    }
                };
            };
        }, "cutout", false, false, false);
    }
}
