# JsonMore - More JSON-customizable content

[English](README.md) | [简体中文](README-zh-cn.md)

A [JsonThings](https://github.com/gigaherz/JsonThings) addon that adds more JSON-configurable content types.

## Features

### Container System
- **Custom Containers** - Create chests of any size via JSON
- **Per-face Filters** - Set insert/extract filters for each face of the container
- **Retention Modes** - Supports `never`, `always` (Shulker-like), and `silk_touch` modes
- **Container Expansion** - Connect adjacent containers horizontally (x), vertically (y), depth-wise (z), or back-to-back (o), doubling the slot count
- **Multiple GUI Types** - Vanilla chest UI, filtered chest UI, or CyclopsCore scrollable container UI
- **Facing & Blocking** - Configurable facing direction and blocking direction (prevents opening when blocked)

### Tinkers' Construct Integration
- **Tinker Chest** - A Tinkers' Construct-style chest with configurable slot mode (`scaling` or `fixed`), max slots, stack limit, and item tag filters
- **Fluid Tank** - A fluid tank block with configurable capacity
- **Copper Can** - A fluid-holding item with configurable capacity
- **Dynamic Material Stat Types** - Define custom material attributes (durability, mining speed, attack damage, harvest tier, etc.) using float and tier fields

### Note Blocks
- **Custom Note Blocks** - (requires [Anvil MusBox](https://github.com/Qikahome/Anvil_MusBox)) Create note blocks with configurable instruments (via block tags), sounds, volume, and collision

### Signs
- **Standing & Wall Signs** - Configurable wood type, with separate block and item registration

### Mantle Books
- **Guide Books** - (requires [Mantle](https://github.com/SlimeKnights/Mantle)) Create custom guide books with Mantle and Tinkers' Construct book transformers

### Custom Game Rules
- **Boolean & Integer Game Rules** - Define custom gamerules via JSON, usable as recipe loading conditions

### Custom Recipes
- **Shaped/Shapeless Consuming** - Recipes that support custom consumption logic
- **Tool Damaging** - Consume tool durability instead of the item
- **Counted Ingredients** - Consume N items at once
- **NBT Copy** - Preserve or transfer NBT data (enchantments, names, lore) during crafting
- **Remainder Override** - Force custom remainder items
- **Item Display Override** - Modify JEI display without changing consumption logic
- **Condition Ingredients** - Runtime-evaluated conditions that can show barrier items in JEI

### Item Application Recipes
- Right-click a block with a tool to transform it, with options for container preservation, blockstate copying, and sneaking requirements

### Custom Ingredients
- **True** - Match all non-empty items
- **Not** - Invert ingredient matching
- **Keep Inventory Container** - Match containers that retain inventory

## Dependencies

### Required
- [JsonThings](https://www.curseforge.com/minecraft/mc-mods/json-things) (0.9.9+)

### Optional
- [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) (15.20.0+) - Recipe display
- [Mantle](https://www.curseforge.com/minecraft/mc-mods/mantle) (1.11.95+) - Guide book support
- [Tinkers' Construct](https://www.curseforge.com/minecraft/mc-mods/tinkers-construct) (3.11.0.148+) - Tinker chest, fluid tank, copper can, material stats
- [Anvil MusBox](https://modrinth.com/mod/UHRtsv4j) (1.1.0+) - Custom note blocks
- [CyclopsCore](https://www.curseforge.com/minecraft/mc-mods/cyclops-core) (1.19.1+) - Scrollable container UI

## Usage

JsonMore follows JsonThings' JSON syntax. See the [documentation](docs/en/index.md) for complete details.

## Mod File Use & Redistribution

The mod file (`.jar`) may be freely used, copied, and redistributed in any modpack, server, or project without prior permission.

For mod developers embedding JsonMore via jarjar, use the following in `META-INF/jarjar/metadata.json`:

```json
{
  "jars": [
    {
      "identifier": {
        "group": "qikahome.jsonmore",
        "artifact": "jsonmore"
      },
      "version": {
        "range": "[<version>,)",
        "artifactVersion": "<version>"
      },
      "path": "META-INF/jarjar/jsonmore-<version>.jar"
    }
  ]
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgements

- Thanks to gigaherz for creating JsonThings
- Thanks to all contributors to JsonThings and related mods
