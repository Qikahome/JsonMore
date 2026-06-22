# Built-in Datapack

Provides the ability to bundle datapacks directly inside a mod JAR and have them appear in the "Datapacks" screen, allowing players to enable or disable them like any other datapack.

Path: `things/<namespace>/built_in_datapack/`

**(Object)** Root object.
- **default_enable** (boolean, optional) Whether the datapack is enabled by default. Defaults to `false`.
- **display_name** ([Text Component](https://minecraft.wiki/w/Raw_JSON_text_format), optional) The display name for the datapack. If not specified, it will use a translatable key `pack.<namespace>.<path>`.

The datapack files themselves should be placed in the `datapacks/<path>/` directory inside the mod JAR, where `<path>` corresponds to the path portion of the registry name.

## Example

```json
{
  "default_enable": true,
  "display_name": {
    "text": "Example Mod Tweaks"
  }
}
```

This registers a datapack with the path `datapacks/example_tweaks/` inside the mod JAR, enabled by default with the display name "Example Mod Tweaks".
