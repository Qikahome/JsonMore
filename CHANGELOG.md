# Json More Changelog

## 1.2.10

### 新增 / Added
- 新增 `jsonmore:standing_and_wall` 通用物品类型（替代 `jsonmore:sign`），告示牌文本编辑逻辑下沉到方块 `setPlacedBy`
  - New `jsonmore:standing_and_wall` generic item type (replaces `jsonmore:sign`); sign text editing moved to block `setPlacedBy`
- 新增 `IProtectedBlock` + `MixinLevel`：在 `Level.setBlock` 源头拦截已连接容器/连接器的破坏，替代 onRemove 放回逻辑
  - New `IProtectedBlock` + `MixinLevel`: block breaking of linked containers/connectors is intercepted at `Level.setBlock`, replacing the onRemove restore hack
- 容器物品提示（tooltip）改为原版潜影盒格式
  - Container item tooltip now uses vanilla shulker box format
- 存储连接器新增 `max_connectors` / `max_capacity` 限制（默认 -1 无限制），在 BFS 扫描阶段检查并阻断超限容器的进一步扩展
  - Storage connector gains `max_connectors` / `max_capacity` limits (default -1 unlimited), enforced during BFS scanning, blocking expansion past the limit

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
