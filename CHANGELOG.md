# Json More Changelog

## 1.1.3
- 添加 NBTCopyIngredient 原料类型，支持 NBT 复制和合并
- 支持路径过滤（display.Lore、display.Lore[0] 等格式）
- 支持 "!" 前缀反选和 "*" 通配符
- 添加 remainder_override 字段统一解析方法
- 优化错误处理，格式错误时抛出 JsonSyntaxException
- 修复路径解析越界问题
- 修复 SelfConsumingIngredient outputModify 未重写问题

## 1.0.4
- 添加 `connectable` 参数，支持跨容器连接
- 添加 `screen` 参数区间映射格式，支持根据容器大小自动选择界面
- 修复容器连接方向逻辑
- 优化 CompoundContainer 顺序
- 支持不同连接前后界面
- 添加容器连接系统文档
- 修复 O 模式连接问题

## 1.0.3
- 初始版本
- 基础容器功能
- 支持过滤器
- 支持保留物品
- 支持水淹没
- 支持猪灵愤怒机制