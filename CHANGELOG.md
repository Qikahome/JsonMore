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
- 依赖更新：Tinkers' Construct 3.12.0.220 / Mantle 1.11.117（开发环境 JEI 15.56.0.205，满足 TCon 3.12 的 JEI ≥15.56.0.204 要求）
  - Dependency update: Tinkers' Construct 3.12.0.220 / Mantle 1.11.117 (JEI 15.56.0.205 in dev, satisfying TCon 3.12's JEI ≥15.56.0.204 requirement)

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
