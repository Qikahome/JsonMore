# Container

Path: `things/<namespace>/block/`

**(Object)** Root object.
- **type** ([Resource Location](https://minecraft.wiki/w/Identifier)) Block type ID, must be `"jsonmore:container"`
- **slots** (integer) Number of container slots. Range depends on GUI screen type (see "GUI Screen Types" below). Default `27`
- **open_sound** ([Resource Location](https://minecraft.wiki/w/Identifier)) Sound event ID played when opening the container, default barrel sound
- **close_sound** ([Resource Location](https://minecraft.wiki/w/Identifier)) Sound event ID played when closing the container, default barrel sound
- **can_waterlogged** (boolean) Whether the block can be waterlogged, default `false`
- **directions** (string) Placing direction. See [Placing Directions](../placing_directions.md) for options, default `facing_horizontal`
- **keep_inventory** (string) Item retention mode. See table below, default `never`

| Mode Value | Description |
|------------|-------------|
| `never` | Items are not retained; they drop when broken |
| `always` | Items are always retained, like Shulker Boxes |
| `silk_touch` | Items are retained only when mined with Silk Touch |

- **anger_piglins** (boolean) Whether opening the container angers nearby piglins, default `false`
- **blocked** (string) Blocking direction. When a solid block exists in that direction, the container cannot be opened. See table below, default `never`

| Direction Value | Description |
|----------------|-------------|
| `never` | No blocking check |
| `up` | Blocked when there is a solid block above |
| `down` | Blocked when there is a solid block below |
| `east` | Blocked when there is a solid block to the east |
| `west` | Blocked when there is a solid block to the west |
| `south` | Blocked when there is a solid block to the south |
| `north` | Blocked when there is a solid block to the north |
| `facing` | Blocked when there is a solid block in the facing direction |

- **insert_filters** (object) Per-face filters for **inserting** items. Format below, default empty
- **extract_filters** (object) Per-face filters for **extracting** items. Same format as `insert_filters`, default empty
- **place_filter** (Ingredient or array) Global restriction on items that can be placed. Default linked to `keep_inventory` (see below)
- **screen** ([Resource Location](https://minecraft.wiki/w/Identifier) or object) GUI screen type. See table below, default `jsonmore:chest`. Supports range mapping format (see below)
- **connected_screen** ([Resource Location](https://minecraft.wiki/w/Identifier) or object) GUI screen type used when connected. Default same as `screen`. Supports range mapping format
- **expandable** (string array) Expandable modes. See table below, default empty (no connection support)

| Mode Value | Description | Connection Direction |
|------------|-------------|---------------------|
| `x` | Horizontal expansion | Left/right (relative to block facing) |
| `y` | Vertical expansion | Up/down |
| `z` | Depth expansion | Front/back (relative to block facing) |
| `o` | Back-to-back expansion | Back-to-back connection |

- **connectable** ([Resource Location](https://minecraft.wiki/w/Identifier)) Tag of connectable container blocks. Default `none:none` (only connect to identical containers)

- Other properties supported by [Block Definition](https://github.com/gigaherz/JsonThings/blob/1.20.1/documentation/formats/Blocks.md)

## GUI Screen Types

| Screen ID | Description | Slot Range | Notes |
|-----------|-------------|------------|-------|
| `minecraft:chest` | Vanilla chest UI | 9 ~ 54, must be multiple of 9 | No filter support |
| `jsonmore:chest` | Chest UI with filter support | 9 ~ 54, must be multiple of 9 | Recommended |
| `cyclopscore:scrolling` | CyclopsCore scrollable container UI | Recommended 45+, multiple of 9 | Requires CyclopsCore |

The `slots` parameter must be within the allowed range of the corresponding screen type, otherwise the UI may not display correctly.

### Range Mapping Format

The `screen` and `connected_screen` parameters support a range mapping format to automatically select the appropriate UI type based on container size.

**Format:**
```json
{
  "screen": {
    "[min,max]": "screen_type_id",
    "[min,max)": "screen_type_id",
    ...
  }
}
```

**Interval Notation:**
- `[a,b]` - Closed interval, includes a and b
- `[a,b)` - Left-closed right-open interval, includes a, excludes b
- `(a,b]` - Left-open right-closed interval, excludes a, includes b
- `(a,b)` - Open interval, excludes a and b
- `[,b]` - From minimum to b
- `[a,]` - From a to maximum

**Example:**
```json
{
  "type": "jsonmore:container",
  "slots": 27,
  "screen": {
    "[0,27]": "jsonmore:chest",
    "[28,54]": "jsonmore:chest",
    "[55,81]": "cyclopscore:scrolling"
  },
  "expandable": ["x", "y"],
  "item": {}
}
```

**Explanation:**
- Container size 0-27: uses `jsonmore:chest` UI
- Container size 28-54 (e.g., after connection): uses `jsonmore:chest` UI
- Container size 55-81: uses `cyclopscore:scrolling` UI

**Priority:** Matches in definition order; the first matching interval takes effect.

## Item Retention Mechanism

| Mode | Description | Loot Table Requirement |
|------|-------------|----------------------|
| `never` | Items are not retained; they drop when broken | Normal configuration works |
| `always` | Items are always retained, like Shulker Boxes | No loot table needed |
| `silk_touch` | Items are retained only when mined with Silk Touch | Special handling needed (see below) |

### SILK_TOUCH Mode Loot Table

When using `silk_touch` mode, the loot table needs a condition: **do not drop the item when mined with Silk Touch**.

```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "namespace:container_id"
        }
      ],
      "conditions": [
        {
          "condition": "minecraft:survives_explosion"
        },
        {
          "condition": "minecraft:inverted",
          "term": {
            "condition": "minecraft:match_tool",
            "predicate": {
              "enchantments": [
                {
                  "enchantment": "minecraft:silk_touch",
                  "levels": { "min": 1 }
                }
              ]
            }
          }
        }
      ]
    }
  ]
}
```

## Item Insert/Extract Restrictions

### Global Restriction (`place_filter`)

The `place_filter` sets a global condition for insertable items, using the standard Ingredient format. Supports custom ingredient types. See [Ingredient Types](../recipes/ingredient_types.md) for details.

Default behavior: When `keep_inventory` is not `never` and `place_filter` is not set, items in the `jsonmore:cannot_place_in_keep_inventory` tag and any keep-inventory containers (`keep_inventory_container`) are prohibited by default.

### Per-Face Restrictions (`insert_filters` / `extract_filters`)

Filters can be set for different directions. Keys are face filters (`any`, `all`, `same`, `opposite`, `up`, `down`, `north`, `south`, `east`, `west`). Values are Ingredients.

Example:
```json
{
  "insert_filters": {
    "up": { "tag": "minecraft:planks" },
    "opposite": { "type": "jsonmore:not", "ingredient": { "item": "minecraft:diamond" } }
  }
}
```

## Piglin Anger Mechanism

- **Anger on open**: Set `anger_piglins: true`
- **Anger on break**: Add the block to the `minecraft:guarded_by_piglins` tag

## Container Connection Mechanism

Containers support connection with adjacent identical containers, merging into a larger container (up to 2). When connected, the slot count doubles and the `connected_screen` UI is used.

### Block States

Connected containers have an additional block state `part`, indicating the container's position in the connection:

| State Value | Description |
|-------------|-------------|
| `none` | Not connected |
| `left` | Left part (X mode) |
| `right` | Right part (X mode) |
| `top` | Top part (Y mode) |
| `bottom` | Bottom part (Y mode) |
| `front` | Front part (Z mode) |
| `back` | Back part (Z mode) |

### Placement Behavior

- **Default placement**: Automatically attempts to connect with adjacent identical containers
- **Shift + Place**: Does not connect
- **Shift + Click adjacent container**: Connects to the specified adjacent container

### Connection Order

In a connected container (`CompoundContainer`), the container order is fixed as follows:
- **X mode**: LEFT container first, RIGHT container second
- **Y mode**: TOP container first, BOTTOM container second
- **Z mode**: FRONT container first, BACK container second
- **O mode**: The container in the positive world coordinate direction first

### Blockstate Requirements

If a container supports connection, blockstate definitions are needed for all `facing` and `part` combinations. Recommended:
- `part=none`: Use a closed model (e.g., `minecraft:block/barrel`)
- `part!=none`: Use an open model (e.g., `minecraft:block/barrel_open`)

### Connectable Containers

The `connectable` parameter specifies which container types can be connected. By default, containers can only connect to identical containers. If `connectable` is set, it specifies which types can be connected.

Note: Currently, connection logic only supports connecting to other `jsonmore:container` type containers. The `connectable` parameter is mainly used to specify the list of container IDs for identification and filtering.

Example:
```json
{
  "type": "jsonmore:container",
  "slots": 27,
  "expandable": ["x"],
  "connectable": ["modid:custom_chest", "modid:wooden_box"],
  "item": {}
}
```

This allows the container to connect to `modid:custom_chest` and `modid:wooden_box` (provided those containers are also `jsonmore:container` type).

### Examples

#### Multi-direction expansion container
```json
{
  "type": "jsonmore:container",
  "slots": 27,
  "expandable": ["x", "y", "z"],
  "screen": "jsonmore:chest",
  "connected_screen": "cyclopscore:scrolling",
  "item": {}
}
```

#### Back-to-back only container
```json
{
  "type": "jsonmore:container",
  "slots": 54,
  "expandable": ["o"],
  "screen": "jsonmore:chest",
  "item": {}
}
```

## Examples

### Basic Container
```json
{
  "type": "jsonmore:container",
  "item": {}
}
```

### Shulker Box-like Container
```json
{
  "type": "jsonmore:container",
  "slots": 27,
  "keep_inventory": "always",
  "item": {}
}
```

### CyclopsCore Scrolling UI (81 slots)
```json
{
  "type": "jsonmore:container",
  "slots": 81,
  "screen": "cyclopscore:scrolling",
  "item": {}
}
```
