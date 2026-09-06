# Recipe Types

JsonMore registers the following custom recipe types that support custom consumption logic.

## Shaped Consuming Recipe `jsonmore:shaped_consuming`

Similar to the vanilla shaped recipe, but uses the consumption logic of [self-consuming ingredients](ingredient_types.md#self-consuming-ingredients).

**Format:**

```json
{
    "type": "jsonmore:shaped_consuming",
    "pattern": [
        "T#T",
        " W "
    ],
    "key": {
        "T": {
            "type": "jsonmore:tool_damaging",
            "ingredient": {
                "tag": "minecraft:pickaxes"
            },
            "damage": 5
        },
        "#": {
            "type": "jsonmore:counted",
            "ingredient": {
                "item": "minecraft:iron_ingot"
            },
            "count": 3
        },
        "W": {
            "type": "jsonmore:counted",
            "ingredient": {
                "item": "minecraft:water_bucket"
            },
            "count": 0
        }
    },
    "result": {
        "item": "minecraft:diamond",
        "count": 1
    }
}
```

**Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `type` | Resource Location | Must be `jsonmore:shaped_consuming` |
| `pattern` | String array | Recipe pattern, one string per row |
| `key` | Object | Character-to-ingredient mapping |
| `result` | Object | Crafting result |

Ingredients in `key` can be normal ingredients or [self-consuming ingredients](ingredient_types.md).

---

## Shapeless Consuming Recipe `jsonmore:shapeless_consuming`

Similar to the vanilla shapeless recipe, but uses the consumption logic of [self-consuming ingredients](ingredient_types.md#self-consuming-ingredients).

**Format:**

```json
{
    "type": "jsonmore:shapeless_consuming",
    "ingredients": [
        {
            "type": "jsonmore:tool_damaging",
            "ingredient": {
                "tag": "minecraft:pickaxes"
            },
            "damage": 1
        },
        {
            "type": "jsonmore:counted",
            "ingredient": {
                "item": "minecraft:diamond"
            },
            "count": 9
        }
    ],
    "result": {
        "item": "minecraft:diamond_block",
        "count": 1
    }
}
```

**Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `type` | Resource Location | Must be `jsonmore:shapeless_consuming` |
| `ingredients` | Array | List of ingredients |
| `result` | Object | Crafting result |

Ingredients can be normal ingredients or [self-consuming ingredients](ingredient_types.md).

---

## Examples with NBT Copy Ingredient

### Example: Transfer a specific enchantment from an enchanted book

Transfer the first enchantment from an enchanted book to another book:

```json
{
    "type": "jsonmore:shapeless_consuming",
    "ingredients": [
        {
            "item": "minecraft:book"
        },
        {
            "type": "jsonmore:nbt_copy",
            "ingredient": {
                "item": "minecraft:enchanted_book"
            },
            "mode": "MERGE_SOURCE_FIRST",
            "tags": [
                "StoredEnchantments[0]"
            ]
        }
    ],
    "result": {
        "item": "minecraft:enchanted_book",
        "count": 1
    }
}
```

**Explanation:**
- First ingredient: normal book (target item)
- Second ingredient: enchanted book (source item), copying only the first enchantment
- Result: enchanted book with the first enchantment
- Remainder: the enchanted book becomes a normal book (if specified by `remainder_override`)

### Example: Preserve item name and Lore during crafting

Transfer a named item's custom name and first Lore line to a new item:

```json
{
    "type": "jsonmore:shaped_consuming",
    "pattern": [
        "A",
        "B"
    ],
    "key": {
        "A": {
            "item": "minecraft:diamond_sword"
        },
        "B": {
            "type": "jsonmore:nbt_copy",
            "ingredient": {
                "item": "minecraft:enchanted_book"
            },
            "mode": "MERGE_SOURCE_FIRST",
            "tags": [
                "display.Name",
                "display.Lore[0]"
            ]
        }
    },
    "result": {
        "item": "minecraft:diamond_sword",
        "count": 1
    }
}
```

**Explanation:**
- Copies the enchanted book's custom name and first Lore line to the diamond sword
- Uses `MERGE_SOURCE_FIRST` mode, using source values on conflict
- Suitable for custom-named equipment crafting

### Example: Full NBT copy (Replace mode)

Fully copy the source item's NBT to the target item:

```json
{
    "type": "jsonmore:shapeless_consuming",
    "ingredients": [
        {
            "item": "minecraft:diamond_sword"
        },
        {
            "type": "jsonmore:nbt_copy",
            "ingredient": {
                "item": "minecraft:golden_sword"
            },
            "mode": "REPLACE"
        }
    ],
    "result": {
        "item": "minecraft:diamond_sword",
        "count": 1
    }
}
```

**Explanation:**
- Uses `REPLACE` mode, completely clearing the diamond sword's original NBT
- Fully copies all NBT data (enchantments, name, durability, etc.) from the golden sword
- Suitable for "upgrade" crafting that preserves all attributes

---

## Item Application Recipe `jsonmore:item_application`

Triggered by right-clicking a block with a tool in hand. Replaces the block and consumes/damages the tool.

**Format:**

```json
{
    "type": "jsonmore:item_application",
    "block": {
        "item": "minecraft:stone"
    },
    "tool": {
        "type": "jsonmore:tool_damaging",
        "ingredient": {
            "tag": "minecraft:pickaxes"
        },
        "damage": 1
    },
    "result": {
        "item": "minecraft:cobblestone"
    },
    "sneaking": false
}
```

**Field Descriptions:**

| Field | Type | Required | Default | Description |
|-------|------|----------|---------|-------------|
| `type` | Resource Location | Yes | -- | Must be `jsonmore:item_application` |
| `block` | [Ingredient](ingredient_types.md) | Yes | -- | Target block matcher |
| `tool` | [Self-consuming ingredient](ingredient_types.md#self-consuming-ingredients) | Yes | -- | Held item matcher, supports consumption logic |
| `result` | Object | Yes | -- | Replacement item result |
| `drop_container` | Boolean | No | `true` | How the original block is handled: `true` = breaking semantics (container contents spill), `false` = silent replacement |
| `keep_block_state` | Boolean | No | `false` | Whether to preserve the original block's BlockState properties |
| `update_block` | Boolean | No | `true` | Whether to trigger block updates (redstone, observer detection) |
| `sneaking` | Boolean | No | *ignored* | Sneak requirement |
| `force_input` | Object | No | -- | Match the input by its actual block/state instead of item conversion (matches blocks without items) |
| `force_output` | Object | No | -- | Force placing a specific block/state; result need not be that block's item |

**Field Details:**

- **block**: An [ingredient](ingredient_types.md) matching the target block. Supports special ingredients like `nbt_copy` for handling block NBT during application. If the right-clicked block has **no item representation** (e.g. most tech blocks), the item-based match is skipped, and `force_input` must be provided to match it.
- **tool**: An [ingredient](ingredient_types.md) for the held item. Supports self-consuming ingredients like `tool_damaging`, `counted`, or `nbt_copy` for copying NBT from the tool to the result.
- **result**: The replacement item. If it is a block item and no `force_output` is given, that block (default state) replaces the original in place; if it is neither a block item nor backed by `force_output`, the original block is removed and this item is dropped.
- **drop_container**: How the original block is handled. `true` (default) uses "breaking" semantics — the block entity is not removed first, so container blocks spill their contents via vanilla behavior (the block itself is not loot-dropped). `false` silently replaces — the old block entity is removed first and data is **not** migrated automatically. To preserve contents/data, use `nbt_copy` on the `block` ingredient to write the block data into `result`; it is loaded automatically on placement.
- **keep_block_state**: Whether to copy the original block's BlockState properties to the new block. Compatible properties are matched automatically; directional properties (e.g., `facing`) support subset mapping (e.g., 6-direction to 4-direction), and container `open` is forced to `false`. Properties explicitly listed in `force_output` override copied values.
- **update_block**: Whether to trigger block updates. When `false`, only syncs the client-side display without triggering neighbor updates (redstone, observers won't react). Suitable for "silent replacement" scenarios; block entity data loading and `setPlacedBy` still run.
- **sneaking**: `true` requires the player to sneak, `false` requires the player to not sneak. When unset, sneaking state is ignored.
- **force_input / force_output**: Optional state spec objects, see below.

### force_input / force_output State Spec

Both fields are optional objects with the shape `{ "block": "...", "properties": { "property": "value" } }`. `block` and `properties` may each be omitted:

- **force_input**: Matches the right-clicked block by its **actual block and properties** (instead of converting it to an item first). Only the listed properties are compared; unlisted ones are not required, and `block` and properties must all match. If the block has an item representation, the `block` ingredient's item match must still pass (both apply); if it has no item, this is the only way to match.
- **force_output**: Specifies the block/state to place, bypassing the "result must be a placeable block item and only places its default state" limitation — so it can output **blocks without items** (e.g. a lit furnace) and non-default states. Property priority: `force_output` explicit properties > `keep_block_state` copied values > defaults.
- Property values are written as strings, e.g. `"facing": "east"`, `"lit": "true"`.
- After a successful placement, the vanilla "place with NBT" flow runs: block entity data carried by `result` is loaded, and the new block's `setPlacedBy` is invoked (JsonMore signs open their edit screen here). Both steps still run with `update_block=false`; only the block update broadcast is skipped.

> **Note**: Recipe triggering requires the held item to be in the `jsonmore:item_application_tool` item tag. Pack authors must add their applicable tool items to this tag.

### Example: Right-click stone with pickaxe to get cobblestone

```json
{
    "type": "jsonmore:item_application",
    "block": {
        "item": "minecraft:stone"
    },
    "tool": {
        "type": "jsonmore:tool_damaging",
        "ingredient": {
            "tag": "minecraft:pickaxes"
        },
        "damage": 1
    },
    "result": {
        "item": "minecraft:cobblestone"
    },
    "sneaking": false
}
```

**Explanation:**
- Right-click stone with any pickaxe
- Pickaxe consumes 1 durability
- Stone turns into cobblestone
- Player must not be sneaking

### Example: Upgrade barrel to container (preserve items)

```json
{
    "type": "jsonmore:item_application",
    "block": {
        "type": "jsonmore:nbt_copy",
        "ingredient": {
            "item": "minecraft:barrel"
        },
        "mode": "REPLACE"
    },
    "tool": {
        "type": "jsonmore:counted",
        "ingredient": {
            "item": "minecraft:paper"
        },
        "count": 0
    },
    "result": {
        "item": "testpack:test_container_plain"
    },
    "drop_container": false,
    "keep_block_state": true,
    "sneaking": true
}
```

**Explanation:**
- Sneak + right-click barrel with paper
- Barrel turns into a custom container, preserving all items inside
- Block state properties are preserved
- Paper is not consumed (count: 0)
- `nbt_copy` copies the block's NBT data to the result item

### Example: Placing a non-default block state (force_output)

```json
{
    "type": "jsonmore:item_application",
    "block": {
        "item": "minecraft:stone"
    },
    "tool": {
        "type": "jsonmore:counted",
        "ingredient": {
            "item": "minecraft:paper"
        },
        "count": 0
    },
    "result": {
        "item": "minecraft:furnace"
    },
    "force_output": {
        "block": "minecraft:furnace",
        "properties": {
            "facing": "east",
            "lit": "true"
        }
    }
}
```

**Explanation:**
- Right-click stone with paper
- Stone is replaced by a **lit furnace facing east** (a non-default state that normal placement cannot produce)
- `result` is still used for JEI display and as the NBT carrier; placement is decided by `force_output`

### Interaction Tag `jsonmore:item_application_tool`

An item tag that marks which items can trigger `item_application` recipes. Only items in this tag will check for recipes when right-clicking a block.

**Example** (datapack `data/jsonmore/tags/items/item_application_tool.json`):

```json
{
    "replace": false,
    "values": [
        "minecraft:iron_pickaxe",
        "minecraft:paper"
    ]
}
```

> **Note**: Pack authors must add their tool items to this tag, or the recipes will not be triggered.

### JEI Display

Recipes are displayed in JEI's "Item Application" category, showing:
- Left: Target block input slot
- Top-left: Tool input slot
- Right: Result output slot
- Center: Enlarged block preview
- Top-right: Sneak requirement hint (bold white, if set)
