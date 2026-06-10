# Dynamic Material Stat Type

**Requires [Tinkers' Construct 3](https://github.com/SlimeKnights/TinkersConstruct)**

Path: `things/<namespace>/material_stat_type/`

## Basic Format

```json
{
    "type": "jsonmore:plain",
    "durability_field": "durability",
    "stats": [
        {
            "type": "tconstruct:float",
            "name": "durability",
            "stat": "durability",
            "default_value": 100.0,
            "operation": "update",
            "description": "stat.jsonmore.durability.desc",
            "tooltip": "stat.jsonmore.durability.tooltip"
        },
        {
            "type": "tconstruct:tier",
            "name": "harvest_tier",
            "stat": "harvest_tier",
            "default_value": "diamond",
            "description": "stat.jsonmore.harvest_tier.desc"
        }
    ]
}
```

## Field Descriptions

### Top-level Fields

- **`type`** (Resource Location, required)
  - The type ID of the material stat type
  - Must be `"jsonmore:plain"`

- **`durability_field`** (string, optional)
  - Specifies the durability field name used for repair
  - If set, tool parts using this stat type can be repaired
  - Defaults to empty string (non-repairable)

- **`stats`** (array, required)
  - List of dynamic stat fields
  - Can be an empty array `[]`

### Per-Stat Field Format

**Common Fields:**

- **`type`** (Resource Location, required)
  - Stat field type
  - Options:
    - `"tconstruct:float"` - Floating point numeric stat
    - `"tconstruct:tier"` - Mining tier stat

- **`name`** (string, required)
  - Name of the stat field
  - Used as the key for JSON serialization/deserialization

- **`stat`** (Resource Location, required)
  - ToolStat ID
  - If no namespace is specified, `tconstruct:` is automatically prefixed
  - Examples: `"durability"`, `"tconstruct:attack_damage"`, `"harvest_tier"`

- **`description`** (string, optional)
  - Translation key for the stat description
  - Defaults to empty string (uses ToolStat's default description)

- **`tooltip`** (string, optional)
  - Translation key for the tooltip
  - Defaults to empty string (uses default format)

**Float-specific Fields:**

- **`default_value`** (float, required)
  - Default value for the stat
  - Example: `100.0`, `1.5`, `-0.5`

- **`operation`** (string, required)
  - Operation applied when used on a tool
  - Options:
    - `"update"` - Direct value update (default)
    - `"add"` - Addition
    - `"percent"` - Percentage bonus
    - `"multiply"` - Multiplication
    - `"multiply_all"` - Global multiplication

**Tier-specific Fields:**

- **`default_value`** (string, required)
  - Default mining tier value
  - Options: `"wood"`, `"stone"`, `"iron"`, `"diamond"`, `"gold"`, `"netherite"`
  - Can be specified without namespace (auto-prefixed with `minecraft:`)

## Examples

### Example 1: Standard Repairable Stat Type

```json
{
    "durability_field": "durability",
    "stats": [
        {
            "type": "tconstruct:float",
            "name": "durability",
            "stat": "durability",
            "default_value": 100.0,
            "operation": "update"
        },
        {
            "type": "tconstruct:float",
            "name": "attack_damage",
            "stat": "attack_damage",
            "default_value": 1.0,
            "operation": "add",
            "description": "stat.mymod.attack_damage.desc",
            "tooltip": "stat.mymod.attack_damage.tooltip"
        },
        {
            "type": "tconstruct:float",
            "name": "mining_speed",
            "stat": "mining_speed",
            "default_value": 2.0,
            "operation": "multiply"
        },
        {
            "type": "tconstruct:tier",
            "name": "harvest_tier",
            "stat": "harvest_tier",
            "default_value": "iron",
            "description": "stat.mymod.harvest_tier.desc"
        }
    ]
}
```

### Example 2: Non-repairable Custom Stat Type

```json
{
    "stats": [
        {
            "type": "tconstruct:float",
            "name": "special_power",
            "stat": "mymod:special_power",
            "default_value": 50.0,
            "operation": "add",
            "description": "stat.mymod.special_power.desc"
        }
    ]
}
```

### Example 3: Empty Stat Type

```json
{
    "stats": []
}
```

### Example 4: Using Percentage and Multiplication

```json
{
    "durability_field": "durability",
    "stats": [
        {
            "type": "tconstruct:float",
            "name": "durability",
            "stat": "durability",
            "default_value": 200.0,
            "operation": "update"
        },
        {
            "type": "tconstruct:float",
            "name": "attack_damage",
            "stat": "attack_damage",
            "default_value": 0.2,
            "operation": "percent",
            "tooltip": "stat.mymod.attack_damage.percent"
        },
        {
            "type": "tconstruct:float",
            "name": "mining_speed",
            "stat": "mining_speed",
            "default_value": 1.5,
            "operation": "multiply"
        }
    ]
}
```

## Notes

1. **`id` field not needed** - The system generates it automatically from the file name
2. **`stat` field auto-namespaces** - If not specified, `tconstruct:` is prefixed by default
3. **`default_value` format** - Tier type can use a simple tier name (e.g., `"diamond"`) without `minecraft:` prefix
4. **`durability_field` must match a stat's `name`** - This is required for correct repair value retrieval
5. **`operation` is Float-only** - Tier type always uses the `update` operation
6. **Empty `stats` array is valid** - But not recommended as it has no practical effect

## Translation Key Example

In `lang/en_us.json`:

```json
{
    "stat.mymod.attack_damage.desc": "Attack Damage",
    "stat.mymod.attack_damage.tooltip": "Attack Damage",
    "stat.mymod.harvest_tier.desc": "Harvest Tier",
    "stat.mymod.special_power.desc": "Special Power"
}
```
