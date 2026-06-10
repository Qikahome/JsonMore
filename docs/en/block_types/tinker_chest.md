# Tinker Chest

**Requires [Tinkers' Construct 3](https://github.com/SlimeKnights/TinkersConstruct)**

Path: `things/<namespace>/block/`

**(Object)** Root object.
- **type** ([Resource Location](https://minecraft.wiki/w/Identifier)) Block type ID, must be `"jsonmore:tinker_chest"`
- **drop_items** (boolean) Optional, default `true`. Whether the chest drops its contents when mined. *(untested)*
- **translation_key** (string) Optional, default is the block's translation key. The title displayed in the GUI's top-left corner. *(untested)*
- **slot_mode** (string) Optional, default `"scaling"`. Available values:
  - `"scaling"`: Dynamic size (like the Part Builder)
  - `"fixed"`: Fixed size (like the Tinker Chest). *(untested)*
- **max_slots** (integer) Optional, default `27`. The capacity/maximum slots of the chest. *(untested)*
- **slot_stack_limit** (integer) Optional, default `64`. Max stack size per slot; values above 64 are undefined. *(untested)*
- **allow_duplicate_item** (boolean) Optional (only when `slot_mode` is `"scaling"`), default `true`. Whether the chest accepts duplicate items. *(untested)*
- **filters** (list of [Resource Locations](https://minecraft.wiki/w/Identifier)) Optional (only when `slot_mode` is `"scaling"`). Only items with all these tags can be placed. *(untested)*
  - (Resource Location) An item tag
- Other properties supported by [Block Definition](https://github.com/gigaherz/JsonThings/blob/1.20.1/documentation/formats/Blocks.md)
