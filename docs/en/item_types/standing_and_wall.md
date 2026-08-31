# Standing and Wall (Item)

Path: `things/<namespace>/item/`

**(Object)** Root object.
- **type** ([Resource Location](https://minecraft.wiki/w/Identifier)) Item type ID, must be `"jsonmore:standing_and_wall"`
- **block** (block ID) The standing block, placed on the ground or ceiling. Required.
- **wall_block** (block ID) The wall block, placed on walls. Required.
- **use_block_name** (boolean, default `true`) Use the block's name as the item display name.
- **direction** (string, default `"down"`) The attachment direction of the standing block. Valid values: `down`, `up`, `north`, `south`, `west`, `east`.
- Other properties supported by [Item Definition](https://github.com/gigaherz/JsonThings/blob/1.20.1/documentation/formats/Items.md)
