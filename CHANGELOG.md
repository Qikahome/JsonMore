# Json More Changelog

## 1.3.0

### 变更 / Changed
- 原料、配方、条件、方块保护（`IProtectedBlock`）、JEI 分类与模型 override 注入改为依赖共享库 **QikaLib 0.1.0**（jarJar 内嵌），JsonMore 不再自带这些实现；`jsonmore:*` 的原料/配方/条件 id 由 QikaLib 以旧 id 重新注册，数据包与旧存档无需改动
  - Ingredients, recipes, conditions, `IProtectedBlock`, JEI categories and model override injection now come from the shared **QikaLib 0.1.0** library (jarJar'd). JsonMore no longer ships its own copies; QikaLib re-registers the legacy `jsonmore:*` ids, so datapacks and old saves keep working
- 存储连接器幽灵槽修复同步到所有加载器：拆解时重建物品列表、读档时按实际最大槽位扩容、激活时归一化 `items.size() == totalSlots`，避免读档后容器凭空多出槽位、拆解时爆出物品或丢物品
  - Ported the storage-connector ghost-slot fix to every loader: rebuild the item list on disassembly, expand to the highest saved slot on load, and normalize `items.size() == totalSlots` on assembly, preventing phantom slots after a reload and item loss/spill on disassembly
## 1.2.15

### 修复 / Fixed
- 修复存储连接器读档后拆解产生“幽灵槽”、导致集装容器凭空多出槽位并在拆解时爆出物品/丢物品：`NonNullList#clear()` 在默认值非 null 时只是把每格填回默认值，并不会缩短列表。控制器 `load()` 用 `NonNullList.withSize(totalSlots, ItemStack.EMPTY)` 建的列表正属此类，`disassemble()` 里的 `items.clear()` 于是留下等长的空槽，之后 `assemble()` 追加新槽位，槽位越积越多（例如 54 幽灵槽 + 27 真实槽 = 81 格），而下一次拆解只按 `Connectors` 回填、多余物品全部溢出，再读档时超出 `TotalSlots` 的物品还会被静默丢弃。现在拆解改为重建列表，读档改用可变列表并把容器扩到实际最大槽位（旧存档里越界的物品不再被丢弃），`assemble()` 也增加了 `items.size() == totalSlots` 的归一化防御
  - Fixed ghost slots after a world reload: `NonNullList#clear()` only refills the list with the default value when that default is non-null, so it never shrinks. The controller's `items` created by `NonNullList.withSize(totalSlots, ItemStack.EMPTY)` in `load()` is exactly such a list, so `disassemble()` left an equally-long list of empty slots; the next `assemble()` appended new slots on top (e.g. 54 ghost + 27 real = 81 slots), the next disassembly could only restore `Connectors`-sized slots and spilled the rest, and any item beyond `TotalSlots` was silently dropped on the following load. Disassembly now rebuilds the list, loading uses a growable list and expands to the highest saved slot (so out-of-range items in old saves are no longer dropped), and `assemble()` now normalizes `items.size() == totalSlots`
- 修正连接器拆解日志：原来把木桶坐标当成“控制器坐标”打印，且恢复后仍打印 `connected=true` 的旧状态
  - Fixed the disassembly log lines, which printed the barrel position as the controller position and the pre-restore `connected=true` state

## 1.2.14

### 修复 / Fixed
- 修复已互相配对的两格容器在再次放置或被就地替换时连接被强制拆开：`FlexBarrelBlock.retryConnection` 原先只在邻居 `part` 为 `none` 时才尝试连接，遇到已经连着自己的邻居会返回失败，`setPlacedBy` 随即把本方块的 `part` 清成 `none`。现在邻居的 `part` 已经指向本方块时视为连接有效并保留（判据与 `updateShape` 中互相配对的检查一致）
  - Fixed forced disconnection when an already paired two-block container is placed again or replaced in place: `FlexBarrelBlock.retryConnection` only tried to connect while the neighbor's `part` was `none`, so a neighbor already linked to this block made it fail and `setPlacedBy` reset this block's `part` to `none`. A neighbor whose `part` already points back at this block is now treated as connected and kept, using the same mutual-pairing check as `updateShape`

## 1.2.13

### 修复 / Fixed
- 修复容器槽位超过 255 时存档丢失物品：原版 `ContainerHelper` 把槽位写成 TAG_Byte，256 以上会被截断并与低槽位撞号（256 → 0），读档时相互覆盖。现改用 `SlotContainerHelper`（`jsonmore:container` 的 `slots`、存储连接器拼接的大容器都可能超过 255），槽位以非负 TAG_Long 存储；读取时兼容旧存档，`128~255` 的负 byte 按无符号还原
  - Fixed item loss when a container holds more than 255 slots: vanilla `ContainerHelper` wrote the slot as a TAG_Byte, so slots ≥ 256 were truncated and collided with lower slots (256 → 0), overwriting each other on load. Slots are now written as a non-negative TAG_Long by `SlotContainerHelper`, and old saves still load correctly (negative bytes for slots 128–255 are decoded as unsigned)

## 1.2.12

### 新增 / Added
- 新增 `jsonmore:regex` 原料类型：按物品注册名（`命名空间:路径`）做正则匹配，`pattern` / `namespace` / `path` 三字段均为整串匹配且可组合，`expand_items` 可展开显示全部匹配物品
  - New `jsonmore:regex` ingredient type: regex matching against item registry names; `pattern` / `namespace` / `path` are anchored and combinable, and `expand_items` lists every matched item
- 新增 `jsonmore:record` 物品类型：`sound`（声音事件）、`length`（时长，单位游戏刻）、`comparator_value`（默认 15）；物品需加入 `minecraft:music_discs` 标签才能放入唱片机
  - New `jsonmore:record` item type: `sound`, `length` (in game ticks) and `comparator_value` (default 15); the item must be added to the `minecraft:music_discs` tag to be playable

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
