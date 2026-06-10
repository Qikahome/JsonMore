# Note Block

Path: `things/<namespace>/block/`

*Requires [Anvil MusBox](https://github.com/Qikahome/Anvil_MusBox)*

**(Object)** Root object.
- **type** ([Resource Location](https://minecraft.wiki/w/Identifier)) Block type ID, must be `"jsonmore:noteblock"`
- **instrument_block_tag** (string) Tag for blocks that trigger this instrument when placed below the note block
- **instrument_name** (string) Translation key for the instrument name, e.g., `"block.minecraft.amethyst_block"`
- **sound** ([Resource Location](https://minecraft.wiki/w/Identifier)) Sound event ID to play, same format as `/playsound`
- **volume** (float) Volume, default `1.0`
- **not_solid** (boolean) Whether the block is non-solid, default `false`
- **has_collision** (boolean) Whether the block has collision, default `true`
- Other properties supported by [Block Definition](https://github.com/gigaherz/JsonThings/blob/1.20.1/documentation/formats/Blocks.md)

## Example

```json
{
  "type": "jsonmore:noteblock",
  "instrument_block_tag": "crystal_sound_blocks",
  "instrument_name": "block.minecraft.amethyst_block",
  "sound": "block.amethyst_block.chime",
  "volume": 100,
  "not_solid": true,
  "has_collision": false
}
```

This example creates an amethyst-style note block: when placed above blocks in the `crystal_sound_blocks` tag, it acts as an instrument playing amethyst chime sound at volume 100, with no collision.
