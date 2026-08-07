# Json More Changelog

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
