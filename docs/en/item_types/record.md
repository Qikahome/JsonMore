# Record

Path: `things/<namespace>/item/`

**(Object)** Root object.
- **type** ([Resource Location](https://minecraft.wiki/w/Identifier)) Item type ID, must be `"jsonmore:record"`
- **jukebox_song** ([Resource Location](https://minecraft.wiki/w/Identifier)) The song played by the record. Required. Points at `data/<namespace>/jukebox_song/<path>.json` in a data pack.
- Other properties supported by [Item Definition](https://github.com/gigaherz/JsonThings/blob/1.20.1/documentation/formats/Items.md)

Since 1.21 records are data-driven: the item itself no longer stores sound, length and comparator output — all of that lives in the song entry referenced by `jukebox_song`. The registered item automatically receives the `jukebox_playable` component (referencing the song by key, so the song does not have to be loaded before the item is registered), and its stack size is forced to `1`.

The item does **not** need to be added to the `minecraft:music_discs` item tag: since 1.21 jukeboxes look at the `jukebox_playable` component directly, and vanilla no longer uses that tag.

## Song entry

Path: `data/<namespace>/jukebox_song/<path>.json`

**(Object)** Root object.
- **sound_event** (sound event ID) The sound played by the record. Required.
- **description** (text component) The song description, shown in the jukebox and item tooltip. Required.
- **length_in_seconds** (float, seconds) Playback length. Required.
- **comparator_output** (integer) Redstone comparator output while the record plays in a jukebox. Required.

## Example

Item:

```json
{
  "type": "jsonmore:record",
  "jukebox_song": "testpack:test_record"
}
```

Song:

```json
{
  "sound_event": "minecraft:music_disc.cat",
  "description": {
    "translate": "jukebox_song.testpack.test_record"
  },
  "length_in_seconds": 185,
  "comparator_output": 15
}
```

This registers a record with the same sound as the vanilla "cat" music disc and a length of `185` seconds. The sound event referenced by `sound_event` must already be registered, otherwise the item fails to parse; the translation key used by `description` must be provided in a language file, otherwise the raw key is shown. The song entry referenced by `jukebox_song` must exist in a data pack, otherwise playback cannot resolve it.
