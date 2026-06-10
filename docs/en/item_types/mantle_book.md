# Mantle Book

**Requires [Mantle](https://github.com/SlimeKnights/Mantle)**

Path: `things/<namespace>/item/`

**(Object)** Root object.
- **type** ([Resource Location](https://minecraft.wiki/w/Identifier)) Item type ID, must be `"jsonmore:book"`
- **book_id** ([Resource Location](https://minecraft.wiki/w/Identifier)) Unique identifier for the book, format `<namespace>:<book_name>`
- **book_data** (JSON array) List of book data transformers. Each element can be:
  - **String**: Transformer ID, e.g., `"mantle:index"`
  - **Object**: Transformer with parameters, must include a `"type"` field, e.g., `{"type": "mantle:repository", "id": "..."}`
- Other properties supported by [Item Definition](https://github.com/gigaherz/JsonThings/blob/1.20.1/documentation/formats/Items.md)

## Available Transformers

### Mantle Built-in Transformers

- **mantle:index** - Adds an index page to the book
- **mantle:padding** - Adds blank padding pages at the end to ensure an even total page count, preventing double-page layout misalignment
- **mantle:content_table** - Adds a table of contents page
- **mantle:repository** - Adds a book content repository. Requires parameter:
  - **id** ([Resource Location](https://minecraft.wiki/w/Identifier)) Path to book content, format `<namespace>:book/<book_name>`, corresponding to resource directory `assets/<namespace>/book/<book_name>/`
- **mantle:set_unicode** - Sets the Unicode font renderer, supporting non-ASCII characters (Chinese, Japanese, etc.)

### Tinkers' Construct Transformers

- **tconstruct:tool_tag_injector** - Adds tool tag injector (auto-generates tool entries from tags)
- **tconstruct:modifier_tag_injector** - Adds modifier tag injector (auto-generates modifier entries from tags)
- **tconstruct:tool_section** - Adds a tool section. Optional parameters:
  - **tool_type** (string) Tool type, e.g., `"melee"`, `"ranged"`, `"armor"`, etc., default `"tools"`
  - **large_title** (boolean) Whether to use a large title, default `false`
  - **center_title** (boolean) Whether to center the title, default `false`
- **tconstruct:modifier_section** - Adds a modifier section. Optional parameters:
  - **modifier_type** (string) Modifier type, e.g., `"upgrades"`, `"defense"`, `"slotless"`, etc., default `"modifiers"`
  - **large_title** (boolean) Whether to use a large title, default `false`
  - **center_title** (boolean) Whether to center the title, default `false`
- **tconstruct:tier_range_material_section** - Adds a material section grouped by mining tier
- **tconstruct:fluid_effect_injector** - Adds a fluid effect injector (adds effect descriptions to fluid entries)

## Examples

### Example 1: Basic Guide Book (Mantle only)

```json
{
    "type": "jsonmore:book",
    "book_id": "mymod:guide_book",
    "book_data": [
        "mantle:index",
        "mantle:padding",
        "mantle:content_table",
        {
            "type": "mantle:repository",
            "id": "mymod:book/guide_book"
        }
    ]
}
```

### Example 2: Tinkers' Tool Guide Book

```json
{
    "type": "jsonmore:book",
    "book_id": "mymod:tinkers_guide",
    "book_data": [
        "mantle:index",
        "mantle:padding",
        "mantle:content_table",
        {
            "type": "mantle:repository",
            "id": "mymod:book/tinkers_guide"
        },
        "tconstruct:tool_tag_injector",
        "tconstruct:modifier_tag_injector",
        {
            "type": "tconstruct:tool_section",
            "tool_type": "melee",
            "large_title": true
        },
        {
            "type": "tconstruct:modifier_section",
            "modifier_type": "armor"
        },
        "tconstruct:tier_range_material_section",
        "tconstruct:fluid_effect_injector",
        "mantle:set_unicode"
    ],
    "creative_tab": "mymod:main",
    "rarity": "uncommon"
}
```

## Notes

1. **`book_id` vs `repository.id`**
   - `book_id` is the book's item ID (e.g., `mymod:guide_book`)
   - `repository.id` is the path to book content (e.g., `mymod:book/guide_book`), corresponding to resource directory `assets/mymod/book/guide_book/`

2. **Unicode Support**
   To display non-ASCII characters (Chinese, Japanese, etc.), the `mantle:set_unicode` transformer is required.

3. **Book Content**
   Book content must be provided in a resource pack. See Mantle documentation for details.

4. **Transformer Order**
   Dynamic content generators (e.g., `tconstruct:tool_tag_injector`) should be placed *before* `mantle:index` to ensure the index page includes their generated pages. `mantle:padding` is typically placed last.
