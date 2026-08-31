# Storage Connector

Path: `things/<namespace>/block/`

**(Object)** Root object.
- **type** ([Resource Location](https://minecraft.wiki/w/Resource_location)) Block type ID, must be `"jsonmore:storage_connector"`
- **radius** (integer) Scan radius, cube range centered on the connector, default `4`
- **max_connectors** (integer) Maximum number of containers that can be linked; when reached, further absorption is skipped and scanning stops, default `-1` (unlimited)
- **max_capacity** (integer) Maximum total capacity (slot count); when reached, further absorption is skipped and scanning stops, default `-1` (unlimited)
- **connectable** (string) Block tag ID for connectable blocks, without `#` prefix. Required.
- **screen** ([Resource Location](https://minecraft.wiki/w/Resource_location) or object) GUI screen type, supports interval mapping format (see [Container doc](container.md#interval-mapping-format)), default `autosizedgui:auto`
- **assemble_sound** ([Resource Location](https://minecraft.wiki/w/Resource_location)) Sound played when assembling, default `minecraft:block.beacon.activate`
- **disassemble_sound** ([Resource Location](https://minecraft.wiki/w/Resource_location)) Sound played when disassembling, default `minecraft:block.beacon.deactivate`
- **open_sound** ([Resource Location](https://minecraft.wiki/w/Resource_location)) Sound played when opening GUI, default `minecraft:block.barrel.open`
- **close_sound** ([Resource Location](https://minecraft.wiki/w/Resource_location)) Sound played when closing GUI, default `minecraft:block.barrel.close`

- Other properties supported in [Block Definition](https://github.com/gigaherz/JsonThings/blob/1.20.1/documentation/formats/Blocks.md)

## Overview

The Storage Connector is a controller block that scans and links nearby containers (within a cube range), managing all linked containers centrally. It is designed for scenarios requiring centralized item management across multiple containers.

### How It Works

1. **Assemble**: Sneak + right-click with empty hand on the connector. BFS search for adjacent `connectable` block paths to find all linkable containers (only `jsonmore:container` type) and absorb their items.
2. **Item Absorption**: During assembly, all items from connected containers are absorbed into the connector's internal inventory, viewable via its GUI.
3. **Incremental Addition**: Repeating sneak + right-click on an already-assembled connector scans for new containers incrementally without re-adding existing ones.
4. **GUI**: Right-click to open the connector's GUI, showing all absorbed items.
5. **Disassemble**: Breaking the connector block triggers disassembly, returning all items to their original containers.
6. **Block State**: When assembled, the connector's block state shows `connected=true`.

### Container Behavior

- Linked containers are **not removed**, their appearance stays the same but block state shows `connected=true`
- Once linked, containers cannot be opened or interacted with independently; all operations go through the connector GUI
- Breaking a linked container/connector is **blocked**: the block does not disappear; instead the whole connector group disassembles first (if the controller can be found), then the block remains
- Pipes and automation can interact directly with the connector's capabilities

## Examples

### Basic Storage Connector
```json
{
  "type": "jsonmore:storage_connector",
  "radius": 4,
  "connectable": "minecraft:wool",
  "screen": "autosizedgui:auto",
  "item": {}
}
```

### Wide Range Storage Connector
```json
{
  "type": "jsonmore:storage_connector",
  "radius": 8,
  "connectable": "extended_storage:barrels",
  "screen": "cyclopscore:scrolling",
  "item": {}
}
```
