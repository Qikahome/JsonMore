# Record

Path: `things/<namespace>/item/`

**(Object)** Root object.
- **type** ([Resource Location](https://minecraft.wiki/w/Identifier)) Item type ID, must be `"jsonmore:record"`
- **sound** (sound event ID) The sound event played by the record. Required.
- **length** (integer, in game ticks) Playback length of the record. Required. Note this is not seconds (vanilla records are defined in seconds; multiply by 20 yourself).
- **comparator_value** (integer, default `15`) Redstone comparator output while the record plays in a jukebox.
- Other properties supported by [Item Definition](https://github.com/gigaherz/JsonThings/blob/1.20.1/documentation/formats/Items.md)

The item must also be added to the `minecraft:music_discs` item tag to be insertable into a jukebox (append it through a data pack tag).

## Example

```json
{
  "type": "jsonmore:record",
  "sound": "minecraft:music_disc.cat",
  "length": 3700,
  "comparator_value": 15,
  "max_stack_size": 1
}
```

This registers a record with the same playback length as vanilla "cat" (`185` seconds × `20` ticks). The sound event referenced by `sound` must already be registered, otherwise the item fails to parse.
