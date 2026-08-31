# Json More Changelog

## 1.2.10

### 新增 / Added
- 新增 `jsonmore:standing_and_wall` 物品类型与 `jsonmore:standing_sign` / `jsonmore:wall_sign` 方块
  - New `jsonmore:standing_and_wall` item type and `jsonmore:standing_sign` / `jsonmore:wall_sign` blocks
- 存储连接器新增 `max_connectors` / `max_capacity` 限制（默认 -1 无限制），在 BFS 扫描阶段检查并阻断超限容器的进一步扩展
  - Storage connector gains `max_connectors` / `max_capacity` limits (default -1 unlimited), enforced during BFS scanning, blocking expansion past the limit

## 1.2.9

### 新增 / Added
- `item_display_override` 继承 `SelfConsumingIngredient`，消耗/输出修改逻辑透传，支持包裹自消耗原料
  - `item_display_override` now extends `SelfConsumingIngredient`, delegating consume/output-modify logic and supporting wrapping self-consuming ingredients
- 网络传输优化：直接发送应用 ops 后的展示物品列表（自定义 streamCodec）
  - Network serialization now sends the modified display item list directly (custom streamCodec)
- op 的 `filter` 字段改为可选
  - The `filter` field of ops is now optional
- 更新配方原料类型文档
  - Updated recipe ingredient documentation
