# Record

Path: `things/<namespace>/item/`

**(Object)** Root object.
- **type** ([Resource Location](https://minecraft.wiki/w/Identifier)) Item type ID, must be `"jsonmore:record"`
- **jukebox_song** ([Resource Location](https://minecraft.wiki/w/Identifier)) The song referenced by the record. Required. Points at `data/<namespace>/jukebox_song/<path>.json` in a data pack.
- Other properties supported by [Item Definition](https://github.com/gigaherz/JsonThings/blob/1.20.1/documentation/formats/Items.md)

## Song Definition in the Data Pack

The record item itself is only a reference; the actual playback information comes from the song definition in the data pack (path `data/<namespace>/jukebox_song/<path>.json`):

**(Object)** Root object.
- **sound_event** (sound event ID) The sound event played by the record. Required.
- **description** ([text component](https://minecraft.wiki/w/Text_component_format)) The name shown while the record plays in a jukebox. Required.
- **length_in_seconds** (float) Playback length in seconds. Required, must be positive.
- **comparator_output** (integer, `0`–`15`) Redstone comparator output while the record plays in a jukebox. Required.

> **Note**: since 1.21 records are fully data-driven; the 1.20.1 `sound` / `length` / `comparator_value` trio no longer exists. Records also no longer need the `minecraft:music_discs` item tag to be inserted into a jukebox — whether a disc can be inserted is decided entirely by the `jukebox_playable` component.

## Example

Item definition `things/testpack/item/test_record.json`:

```json
{
  "type": "jsonmore:record",
  "jukebox_song": "testpack:test_record",
  "max_stack_size": 1
}
```

The corresponding song definition `data/testpack/jukebox_song/test_record.json`:

```json
{
  "comparator_output": 15,
  "description": {
    "translate": "jukebox_song.testpack.test_record"
  },
  "length_in_seconds": 185.0,
  "sound_event": "minecraft:music_disc.cat"
}
```

This registers a record with the same playback length as vanilla "cat" (`185` seconds). The song referenced by `jukebox_song` must exist in the data pack, otherwise building an item stack and resolving the `jukebox_playable` component will fail; the sound event referenced by `sound_event` must already be registered. `description` is the display text component, conventionally a `jukebox_song.<namespace>.<path>` translation key provided in a language file.
