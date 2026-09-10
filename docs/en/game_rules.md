# Game Rules

Path: `things/<namespace>/gamerule/`

**(Object)** Root object.
- **type** ([Resource Location](https://minecraft.wiki/w/Identifier)) GameRule type, must be one of:
  - `jsonmore:boolean` - Boolean type (true/false)
  - `jsonmore:integer` - Integer type
- **default_value** (boolean or integer) Default value of the GameRule, must match the `type`
- **category** (string) GameRule category, determines its grouping in the "Create World" screen. Optional values below, default `misc`

| Category Value | Description |
|----------------|-------------|
| `chat` | Chat-related settings |
| `cheats` | Cheat-related settings |
| `drops` | Drop-related settings |
| `mobs` | Mob-related settings |
| `misc` | Miscellaneous settings |
| `player` | Player-related settings |
| `spawning` | Spawning-related settings |
| `updates` | Update-related settings |

## Usage

Use the `/gamerule` command to view or modify GameRules:

```bash
# View current value
/gamerule <namespace>.<rule_name>

# Set to a specific value
/gamerule testpack.test_boolean_rule true
/gamerule testpack.test_integer_rule 50
```

## Examples

### Boolean GameRule
```json
{
  "type": "jsonmore:boolean",
  "default_value": true,
  "category": "misc"
}
```

### Integer GameRule
```json
{
  "type": "jsonmore:integer",
  "default_value": 10,
  "category": "misc"
}
```

## Recipe Condition

`jsonmore:gamerule` can be used as a recipe loading condition, determining whether a recipe is loaded based on the current value of a game rule.

On Fabric this goes through Fabric API's resource conditions (`fabric-resource-conditions-api-v1`): the recipe JSON uses the top-level `fabric:load_conditions` key (either a single condition or a list of conditions), and the type key of a condition object is `condition` — not the Neo/Forge `conditions` + `type`.

**(Object)** Root object.
- **rule** (string) Game rule name in dot format (same as `/gamerule` command)
- **value** (optional) Expected value:
  - Not present -> treated as a **boolean rule**, checks if `true`
  - Integer (e.g., `5`) -> treated as an **integer rule**, exact match
  - Range string (e.g., `[1,3]`, `[2,)`) -> treated as an **integer rule**, range match

> For boolean rule negation, use `fabric:not` (the negated condition goes in its `value` field) instead of explicitly specifying `false` in `jsonmore:gamerule`.

```jsonc
// Boolean rule: recipe loads when rule is true
{
  "condition": "jsonmore:gamerule",
  "rule": "jsonmore.some_flag"
}

// Boolean rule negation: via fabric:not
{
  "condition": "fabric:not",
  "value": {
    "condition": "jsonmore:gamerule",
    "rule": "jsonmore.some_flag"
  }
}

// Integer rule: exact match
{
  "condition": "jsonmore:gamerule",
  "rule": "jsonmore.some_count",
  "value": 5
}

// Integer rule: range match (closed interval)
{
  "condition": "jsonmore:gamerule",
  "rule": "jsonmore.some_count",
  "value": "[3,10)"
}

// Integer rule: range match (lower bound only)
{
  "condition": "jsonmore:gamerule",
  "rule": "jsonmore.some_count",
  "value": "[100,]"
}
```

### Complete Example

Only load the advanced crafting recipe when `jsonmore.enable_advanced_crafting` is `true`:

```json
{
  "type": "minecraft:crafting_shaped",
  "fabric:load_conditions": [
    {
      "condition": "jsonmore:gamerule",
      "rule": "jsonmore.enable_advanced_crafting"
    }
  ],
  "pattern": ["###", "#X#", "###"],
  "key": {
    "#": { "item": "minecraft:diamond" },
    "X": { "item": "minecraft:nether_star" }
  },
  "result": { "item": "minecraft:beacon" }
}
```

> The condition is evaluated **when recipes are loaded** (world load / `/reload`), not on each crafting attempt. On the first world join `Utils#getCurrentServer()` is not set yet, so the condition always evaluates to `false`; it takes effect after a `/reload`. For runtime dynamic checks use the [`jsonmore:condition`](recipes/ingredient_types.md) ingredient instead.
