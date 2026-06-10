# Ingredient Types

JsonMore registers the following custom ingredient types, usable in recipes and `place_filter` (e.g., insert/extract filters in containers).

---

## Self-Consuming Ingredients

Self-consuming ingredients are the base class for all custom consumable ingredients. When a recipe uses a self-consuming ingredient, it calls its `consume` method to determine the remainder after crafting, rather than the vanilla `getCraftingRemainingItem`.

### Base Class `SelfConsumingIngredient`

**Core Logic:**

```java
public static ItemStack consume(Ingredient ingredient, ItemStack stack) {
    if (stack.isEmpty())
        return stack;
    if (ingredient instanceof SelfConsumingIngredient selfConsumingIngredient)
        return selfConsumingIngredient.consume(stack);
    return vanillaConsume(stack); // Vanilla logic: returns getCraftingRemainingItem
}
```

### Tool Damaging Ingredient `jsonmore:tool_damaging`

Matches damageable tools and consumes durability instead of the item itself during crafting.

**Format:**

```json
{
    "type": "jsonmore:tool_damaging",
    "ingredient": {
        "tag": "minecraft:pickaxes"
    },
    "damage": 5
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | Resource Location | Yes | Must be `jsonmore:tool_damaging` |
| `ingredient` | Ingredient | Yes | Inner ingredient for matching items |
| `damage` | Integer | Yes | Durability to consume |

**Behavior:**

- Matching: The item must be a damageable tool with remaining durability >= `damage`
- Consumption: Deals `damage` points of durability damage to the tool
- Return: If the tool breaks, returns `getCraftingRemainingItem`

**Tinkers' Compatibility**: If Tinkers' Construct 3 is installed, reinforced durability and other modifiers are handled correctly.

### Counted Ingredient `jsonmore:counted`

Matches a specified quantity of items and consumes that quantity during crafting.

**Format:**

```json
{
    "type": "jsonmore:counted",
    "ingredient": {
        "item": "minecraft:diamond"
    },
    "count": 9
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | Resource Location | Yes | Must be `jsonmore:counted` |
| `ingredient` | Ingredient | Yes | Inner ingredient for matching items |
| `count` | Integer | Yes | Required item count |

**Behavior:**

- Matching: Item stack count >= `count`
- Consumption: Consumes `count` items
- Return: `getCraftingRemainingItem` x `count`

### NBT Copy Ingredient `jsonmore:nbt_copy`

Matches items and copies NBT data from the source item to the result during crafting. Supports multiple copy modes and path filtering.

**Basic Format:**

```json
{
    "type": "jsonmore:nbt_copy",
    "ingredient": {
        "item": "minecraft:book"
    },
    "mode": "MERGE_SOURCE_FIRST"
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | Resource Location | Yes | Must be `jsonmore:nbt_copy` |
| `ingredient` | Ingredient | Yes | Inner ingredient for matching items |
| `mode` | String | Yes | NBT copy mode (see table below) |
| `tags` | String array / String | No | NBT path filter, supports nesting and array indices |

**Copy Modes:**

| Mode | Description |
|------|-------------|
| `REPLACE` | Replace mode: clears the target item's original NBT and fully copies the source item's NBT |
| `MERGE_TARGET_FIRST` | Merge mode (target priority): merges source NBT into target, using target values on conflict |
| `MERGE_SOURCE_FIRST` | Merge mode (source priority): merges source NBT into target, using source values on conflict |

**Path Format:**

The `tags` field supports the following path formats:

- **Simple tag name**: `"Enchantments"`, `"Damage"`
- **Nested path**: `"display.Name"`, `"display.Lore"`
- **Array index**: `"display.Lore[0]"`, `"Enchantments[1]"`
- **Mixed usage**: `"Enchantments[0].id"`, `"Enchantments[0].lvl"`

**Behavior:**

- Matching: Matches items specified by `ingredient`
- Consumption: Normal item consumption, returns `getCraftingRemainingItem`
- NBT Copy: Copies source item NBT to the result according to `mode` and `tags`

> **Note**: NBT copy ingredients are typically used in scenarios such as enchanted books, named items, or any crafting that requires preserving or transferring NBT data.

---

### Remainder Override Ingredient `jsonmore:remainder_override`

Wraps a normal ingredient and forcibly overrides its crafting remainder. **The inner ingredient must NOT be a self-consuming ingredient** (e.g., `jsonmore:counted`, `jsonmore:tool_damaging`, etc.).

**Format:**

```json
{
    "type": "jsonmore:remainder_override",
    "ingredient": {
        "item": "minecraft:water_bucket"
    },
    "remainder_override": {
        "id": "minecraft:water_bucket",
        "Count": 1
    }
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | Resource Location | Yes | Must be `jsonmore:remainder_override` |
| `ingredient` | Ingredient | Yes | Normal ingredient, must not be self-consuming |
| `remainder_override` | SNBT | Yes | Override remainder item (full SNBT item stack format) |

**Behavior:**

- Executes vanilla consumption logic on the item stack (consumes 1, returns vanilla container item)
- Ignores the vanilla remainder result and always returns the `remainder_override` item

---

### Item Display Override Ingredient `jsonmore:item_display_override`

Wraps an ingredient and applies operations to the item display list returned by `getItems()`. **Does not change matching or consumption logic**; it only affects display in JEI/recipe book.

**Format:**

```json
{
    "type": "jsonmore:item_display_override",
    "ingredient": {
        "item": "minecraft:stick"
    },
    "ops": [
        { "op": "remove", "value": { "item": "minecraft:stick" } },
        { "op": "add", "value": { "id": "minecraft:diamond", "Count": 1 } },
        { "op": "add_all", "value": { "tag": "minecraft:planks" } },
        { "op": "modify_nbt", "value": { "Enchantments": [] }, "mode": "merge" },
        { "op": "modify_count", "operation": "multiply", "value": 2 }
    ]
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | Resource Location | Yes | Must be `jsonmore:item_display_override` |
| `ingredient` | Ingredient | Yes | Inner ingredient |
| `ops` | Array | Yes | List of operations, applied in order |

**Available Operations (op):**

| Operation | Description | `value` Type | Optional Fields |
|-----------|-------------|-------------|-----------------|
| `remove` | Removes matching item stacks from the display list | Ingredient | `filter` |
| `add_all` | Gets all item stacks from another ingredient and adds them to the display list | Ingredient | -- |
| `add` | Adds a specific item stack to the display list | ItemStack SNBT | -- |
| `modify_nbt` | Modifies the NBT of item stacks (`mode` can be `merge`/`replace`) | CompoundTag | `filter`, `mode` |
| `modify_count` | Modifies the count of item stacks (`operation` can be `set`/`add`/`multiply`) | Integer | `filter`, `operation` |

**`filter` field**: Optional, type is Ingredient. Only item stacks matching this filter are affected.

**Behavior:**

- Matching and consumption are fully delegated to the inner `ingredient`
- Operations only affect the `getItems()` return value for display (JEI recipe display)
- Operations are applied sequentially in the `ops` array order
- Each operation acts on the result list of the previous operation

---

### Condition Ingredient `jsonmore:condition`

Wraps an ingredient and only makes it available in crafting when a specified condition is met. **The condition is evaluated in real-time at each crafting attempt**, without requiring recipe reload.

**Format:**

```json
{
    "type": "jsonmore:condition",
    "condition": {
        "type": "forge:mod_loaded",
        "modid": "tconstruct"
    },
    "ingredient": {
        "item": "minecraft:stick"
    },
    "message": "recipe.jsonmore.disabled"
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | Resource Location | Yes | Must be `jsonmore:condition` |
| `condition` | Object | Yes | Forge `ICondition` condition object (supports any Forge condition type) |
| `ingredient` | Ingredient | Yes | Inner ingredient, used when condition is met |
| `message` | String | No | Translation key for the barrier item shown when condition is not met, default `recipe.jsonmore.disabled` |

**Behavior:**

- Condition met: Behaves exactly like the inner ingredient (matching, consumption, remainder all delegated)
- Condition not met:
  - `test()` always returns `false` (does not match any items)
  - `getItems()` returns a **barrier item** with the `message` translation key (shown to players indicating the condition is not satisfied)

**Network Sync:**

- Server evaluates the `condition` and syncs the result (boolean) to the client
- Client does not parse `condition`, only uses the server's evaluation result
- When the client receives `passes = false`, crafting itself fails, and JEI displays the barrier item

**Use Cases:**

- Replace `forge:condition` (evaluated at recipe load time) for **runtime dynamic conditions**
- Used with gamerule conditions (`jsonmore:gamerule_boolean`) to let players toggle recipes via commands during gameplay
- Any scenario where the condition needs to be evaluated at crafting time rather than recipe load time

**Example with gamerule condition:**

```json
{
    "type": "jsonmore:condition",
    "condition": {
        "type": "jsonmore:gamerule_boolean",
        "key": "doMobSpawning"
    },
    "ingredient": {
        "item": "minecraft:rotten_flesh"
    },
    "message": "recipe.jsonmore.disabled"
}
```

When `doMobSpawning` is `false`, this ingredient cannot be used in crafting, and JEI displays a "condition not met" barrier.

> **Note**: This ingredient inherits from `SelfConsumingIngredient` and can be used as a consumable ingredient, supporting wrappers like `remainder_override`.

---

## Filter Ingredients

The following ingredient types are used **only for matching items** and do not involve consumption logic.

### `jsonmore:true`

Always matches **all** non-empty items.
In JEI/recipe book, it displays as a stick named "Anything" (display only, does not affect matching logic).

```json
{
  "type": "jsonmore:true"
}
```

### `jsonmore:not`

Logical NOT filter, matches items **not** in the specified `ingredient`.
In JEI/recipe book, it displays the excluded items list and shows as "Anything except <original item name>".

```json
{
  "type": "jsonmore:not",
  "ingredient": {
    "tag": "minecraft:shulker_boxes"
  }
}
```

The above example matches all items that are **not** shulker boxes.

### `jsonmore:keep_inventory_container`

Matches **containers that can retain their inventory** (i.e., containers where `keep_inventory` is not `NEVER`). Supports two modes:

| Mode | Description |
|------|-------------|
| `may` | Matches all containers that **can** retain items (`keep_inventory` is `ALWAYS` or `SILK_TOUCH`) |
| `contains` | Matches keep-inventory containers that **currently contain items** (at least one non-empty slot) |

**Examples:**

```json
{
  "type": "jsonmore:keep_inventory_container",
  "mode": "may"
}
```

Matches all containers that can retain items (regardless of whether they contain items).

```json
{
  "type": "jsonmore:keep_inventory_container",
  "mode": "contains"
}
```

Matches all keep-inventory containers that **currently contain at least one item**.

> Note: `contains` mode checks the `BlockEntityTag.Items` list, so only placed containers with stored items will be matched.

---

## Combined Usage

You can combine the above custom ingredients with Forge's `forge:compound` (OR), `forge:intersection` (AND), etc., for complex logic.

### Example: Match items that are neither shulker boxes nor keep-inventory containers

```json
{
  "type": "forge:intersection",
  "children": [
    {
      "type": "jsonmore:not",
      "ingredient": {
        "tag": "minecraft:shulker_boxes"
      }
    },
    {
      "type": "jsonmore:not",
      "ingredient": {
        "type": "jsonmore:keep_inventory_container",
        "mode": "may"
      }
    }
  ]
}
```

### Example: Match keep-inventory containers **or** shulker boxes

```json
[
  {
    "type": "jsonmore:keep_inventory_container",
    "mode": "may"
  },
  {
    "tag": "minecraft:shulker_boxes"
  }
]
```

> **Note**: In `place_filter`, you can use a JSON array to represent OR combination without explicitly using `forge:compound`.

---

## Usage in `place_filter`

`place_filter` is an `ItemFilter` that can directly use any of the above `Ingredient` JSON objects. For example, to only allow non-keep-inventory-container items:

```json
{
  "place_filter": {
    "type": "jsonmore:not",
    "ingredient": {
      "type": "jsonmore:keep_inventory_container",
      "mode": "may"
    }
  }
}
```
