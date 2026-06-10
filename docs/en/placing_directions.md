# Placing Directions

Placing directions control how a block is oriented when placed.

| Direction Value | Description |
|----------------|-------------|
| `up` | Always facing up |
| `down` | Always facing down |
| `north` | Always facing north |
| `south` | Always facing south |
| `east` | Always facing east |
| `west` | Always facing west |
| `facing_horizontal` | Horizontal facing, determined by the player's looking direction |
| `facing_all` | Full direction, determined by the player's looking direction |
| `clicking_horizontal` | Horizontal click face, determined by the face clicked (horizontal only) |
| `clicking_all` | Full click face, determined by the face clicked |
| `vertical` | Vertical direction, determined by the clicked face or player's pitch |

## Details

### Fixed Directions
- `up`: Block always faces upward
- `down`: Block always faces downward
- `north`: Block always faces north
- `south`: Block always faces south
- `east`: Block always faces east
- `west`: Block always faces west

### `facing_horizontal`
Determines the block's facing based on the player's horizontal looking direction (N/S/E/W only), like chests.

### `facing_all`
Determines the block's facing based on the player's looking direction, supporting all six directions, like pistons.

### `clicking_horizontal`
Determines the block's facing based on the face of the block the player clicked, horizontal directions only. Falls back to `facing_horizontal` behavior when clicking top or bottom faces, like signs.

### `clicking_all`
Determines the block's facing based on the face of the block the player clicked, supporting all six directions.

### `vertical`
Determines the vertical direction based on the clicked face or player's pitch:
- Clicking top/bottom faces: faces the clicked face
- Clicking horizontal faces: faces up or down based on the player's pitch
