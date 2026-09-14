# Record

Path: `things/<namespace>/item/`

**(Object)** Root object.
- **type** ([Resource Location](https://minecraft.wiki/w/Identifier)) Item type ID, must be `"jsonmore:record"`
- **jukebox_song** ([Resource Location](https://minecraft.wiki/w/Identifier)) The jukebox song to reference, in the form `<namespace>:<path>`, i.e. `data/<namespace>/jukebox_song/<path>.json`. Required.
- Other properties supported by [Item Definition](https://github.com/gigaherz/JsonThings/blob/1.20.1/documentation/formats/Items.md)

**Records are data-driven since 1.21**: the 1.20.1 `sound` / `length` / `comparator_value` trio is gone — that information all lives in the jukebox song definition referenced by `jukebox_song`. The item itself is always single-stack (`max_stack_size: 1`) and automatically carries the `jukebox_playable` data component, so adding it to the `minecraft:music_discs` item tag is **no longer required** to insert it into a jukebox.

## Song Definition

You must provide the song file yourself in a data pack at `data/<namespace>/jukebox_song/<path>.json`, with the following fields:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `sound_event` | Sound event ID | Yes | The sound event played |
| `description` | Text component | Yes | The name shown in the item tooltip |
| `length_in_seconds` | Positive float | Yes | Playback length in seconds |
| `comparator_output` | Integer | Yes | Redstone comparator output while playing, `0`-`15` |

## Example

```json
{
  "type": "jsonmore:record",
  "jukebox_song": "testpack:test_record"
}
```

The matching song definition `data/testpack/jukebox_song/test_record.json`:

```json
{
  "sound_event": "minecraft:music_disc.cat",
  "description": { "translate": "jukebox_song.testpack.test_record" },
  "length_in_seconds": 185.0,
  "comparator_output": 2
}
```

This registers a record with the same sound and 185-second length as the vanilla "cat" disc.
