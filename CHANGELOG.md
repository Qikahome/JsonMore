# Json More Changelog

## 1.2.9

### 新增 / Added
- 新增 `jsonmore:fluid` 原料类型：匹配/消耗容器内的流体（返还空容器），展示用笛卡尔积
  - New `jsonmore:fluid` ingredient type: matches/consumes fluid in containers (returns the empty container), display uses cartesian product
- `item_display_override` 继承 `SelfConsumingIngredient`，消耗/输出修改逻辑透传，支持包裹自消耗原料
  - `item_display_override` now extends `SelfConsumingIngredient`, delegating consume/output-modify logic and supporting wrapping self-consuming ingredients
- 网络传输优化：直接发送应用 ops 后的展示物品列表
  - Network serialization now sends the modified display item list directly
- 更新配方原料类型文档
  - Updated recipe ingredient documentation
