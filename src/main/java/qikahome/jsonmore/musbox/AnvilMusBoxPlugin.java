package qikahome.jsonmore.musbox;

import static qikahome.jsonmore.JsonMore.*;

import java.util.List;
import java.util.Map;

import dev.gigaherz.jsonthings.things.parsers.ThingParseException;
import dev.gigaherz.jsonthings.things.serializers.FlexBlockType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import qikahome.anvil_musbox.AnvilMusBoxMod;

public class AnvilMusBoxPlugin {
    public static void load() {
        LOGGER.info("Loading JsonMore AnvilMusBoxPlugin");
        FlexBlockType.register("jsonmore:noteblock", data -> {
            String instrumentBlockTag = GsonHelper.getAsString(data, "instrument_block_tag");
            String instrumentName = GsonHelper.getAsString(data, "instrument_name");
            ResourceLocation sound = new ResourceLocation(GsonHelper.getAsString(data, "sound"));
            float volume = GsonHelper.getAsFloat(data, "volume", 1.0F);
            return (props, builder) -> {
                List<Property<?>> _properties = builder.getProperties();
                Map<Property<?>, Comparable<?>> propertyDefaultValues = builder.getPropertyDefaultValues();
                TagKey<Block> tag = TagKey.create(Registries.BLOCK, new ResourceLocation(instrumentBlockTag));
                SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(sound);
                if (soundEvent == null)
                    throw new ThingParseException("Sound event " + sound + " not found");
                FlexNoteBlock noteBlock = new FlexNoteBlock(props, propertyDefaultValues, tag, instrumentName,
                        soundEvent, volume) {
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
                AnvilMusBoxMod.INSTRUMENTS.add(noteBlock);
                return noteBlock;
            };
        }, "solid", false, false, false);
    }
}
