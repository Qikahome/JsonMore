# Json More Changelog

## 1.2.11

### 新增 / Added
- `jsonmore:item_application` 新增 `force_input` / `force_output`：按 block + properties 精确匹配输入方块 / 指定输出方块状态，支持无对应物品的方块与非默认状态
  - `jsonmore:item_application` gains `force_input` / `force_output` to match input blocks and force output block states by block + properties (supports blocks without items and non-default states)

### 变更 / Changed
- `drop_container: false` 不再自动把旧方块实体数据整体迁移到新方块，需要保留内容/NBT 时请配合 `jsonmore:nbt_copy` 原料显式完成
  - `drop_container: false` no longer auto-migrates the old block entity data to the new block; carry content/NBT over explicitly with `jsonmore:nbt_copy`
- 方块型输出改为模拟原版放置：放置后装载产物 NBT 并调用方块 `setPlacedBy`；`drop_container: true` 时旧容器按原版破坏语义处理（onRemove 洒出内容物）
  - Block-type outputs now mimic vanilla placement (result NBT is loaded and `setPlacedBy` is called); with `drop_container: true` old containers are removed with vanilla break semantics
- 替换/移除失败时自动回滚旧方块实体，不残留损坏状态
  - Failed replacement/removal rolls back the old block entity, leaving no corrupted states
- `item_application` 客户端改用 NeoForge 官方配方同步（`OnDatapackSyncEvent#sendRecipes` / 客户端 `RecipesReceivedEvent`）获取全量配方精确匹配后再取消右键事件，与 1.20.1/1.21.1 行为一致（MC≥1.21.2 原版已不再把配方同步给客户端）
  - Client-side `item_application` now uses NeoForge's built-in recipe sync (`OnDatapackSyncEvent#sendRecipes` / client-side `RecipesReceivedEvent`) to get the full recipe list and cancel right-clicks only on an exact match, matching 1.20.1/1.21.1 behavior (clients on MC≥1.21.2 no longer receive recipes from vanilla)

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
