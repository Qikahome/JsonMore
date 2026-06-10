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

**(Object)** Root object.
- **rule** (string) Game rule name in dot format (same as `/gamerule` command)
- **value** (optional) Expected value:
  - Not present -> treated as a **boolean rule**, checks if `true`
  - Integer (e.g., `5`) -> treated as an **integer rule**, exact match
  - Range string (e.g., `[1,3]`, `[2,)`) -> treated as an **integer rule**, range match

> For boolean rule negation, use `forge:not` instead of explicitly specifying `false` in `jsonmore:gamerule`.

```jsonc
// Boolean rule: recipe loads when rule is true
{
  "type": "jsonmore:gamerule",
  "rule": "jsonmore.some_flag"
}

// Boolean rule negation: via forge:not
{
  "type": "forge:not",
  "value": {
    "type": "jsonmore:gamerule",
    "rule": "jsonmore.some_flag"
  }
}

// Integer rule: exact match
{
  "type": "jsonmore:gamerule",
  "rule": "jsonmore.some_count",
  "value": 5
}

// Integer rule: range match (closed interval)
{
  "type": "jsonmore:gamerule",
  "rule": "jsonmore.some_count",
  "value": "[3,10)"
}

// Integer rule: range match (lower bound only)
{
  "type": "jsonmore:gamerule",
  "rule": "jsonmore.some_count",
  "value": "[100,]"
}
```

### Complete Example

Only load the advanced crafting recipe when `jsonmore.enable_advanced_crafting` is `true`:

```json
{
  "type": "minecraft:crafting_shaped",
  "conditions": [
    {
      "type": "jsonmore:gamerule",
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
