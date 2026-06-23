[English](README.md) | [简体中文](README-zh-cn.md)

# JsonMore - 更多 JSON 魔改内容

一个 [JsonThings](https://github.com/gigaherz/JsonThings) 附属模组，增加了更多可通过 JSON 配置的内容类型。

## 功能特性

### 容器系统
- **自定义容器** - 通过 JSON 创建任意大小的箱子
- **每面过滤器** - 为容器的每个面分别设置放入/取出过滤器
- **保留模式** - 支持 `never`（掉落）、`always`（类似潜影盒）和 `silk_touch`（仅精准采集保留）
- **容器扩展** - 支持水平（x）、垂直（y）、纵深（z）、背靠背（o）四种方向连接相邻容器，槽位翻倍
- **多种界面** - 原版箱子界面、带过滤器的箱子界面、CyclopsCore 滚动容器界面
- **朝向与阻挡** - 可配置放置方向，以及阻挡方向（对应方向有方块时禁止打开）

### ~~匠魂 3 集成~~（尚未移植 1.21.1）
- ~~**工匠箱** - 匠魂风格的箱子，可配置槽位模式（`scaling` 动态大小/`fixed` 固定大小）、最大槽位数、单槽堆叠上限和物品标签过滤器~~
- ~~**流体储罐** - 可配置容量的流体储罐方块~~
- ~~**铜罐** - 可配置容量的流体存储物品~~
- ~~**动态材料属性类型** - 自定义材料的属性字段（耐久、挖掘速度、攻击伤害、挖掘等级等），支持浮点数和挖掘等级类型~~

### 音符盒
- **自定义音符方块** - （需要 [Anvil MusBox](https://github.com/Qikahome/Anvil_MusBox)）通过 JSON 创建音符方块，可配置乐器（通过方块标签）、音效、音量和碰撞

### ~~地幔指南书~~（尚未移植 1.21.1）
- ~~**自定义指南书** - （需要 [Mantle](https://github.com/SlimeKnights/Mantle)）创建自定义指南书，支持 Mantle 和匠魂的书籍数据变换器~~

### 自定义游戏规则
- **布尔与整数游戏规则** - 通过 JSON 定义自定义游戏规则，可用作配方加载条件

### 自定义配方
- **有序/无序消耗配方** - 支持自定义消耗逻辑的合成配方
- **工具耐久消耗** - 消耗工具耐久而非物品本身
- **计数原料** - 一次消耗 N 个物品
- **NBT 复制** - 合成时保留或转移 NBT 数据（附魔、名称、描述等）
- **返还覆盖** - 强制指定合成返还物品
- **展示覆盖** - 修改 JEI 显示而不改变消耗逻辑
- **条件原料** - 运行时评估的条件，条件不满足时在 JEI 中显示屏障物品提示

### 物品交互配方
- 手持工具右键点击方块触发转换，支持容器保留、方块状态复制、潜行要求等选项

### 自定义原料
- **True** - 匹配所有非空物品
- **Not** - 原料取反
- **Keep Inventory Container** - 匹配保留物品栏的容器

## 依赖项

### 必需依赖
- [JsonThings](https://www.curseforge.com/minecraft/mc-mods/json-things) (0.9.9+)

### 可选依赖
- [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) - 配方显示
### ~~可选依赖（尚未移植 1.21.1）~~
- ~~[Mantle](https://www.curseforge.com/minecraft/mc-mods/mantle) - 指南书支持~~
- ~~[Tinkers' Construct](https://www.curseforge.com/minecraft/mc-mods/tinkers-construct) - 工匠箱、流体储罐、铜罐、材料属性~~
- [Anvil MusBox](https://modrinth.com/mod/UHRtsv4j) (1.1.0+) - 自定义音符方块
- [CyclopsCore](https://www.curseforge.com/minecraft/mc-mods/cyclops-core) (1.19.1+) - 滚动容器界面

## 使用方法

JsonMore 遵循 JsonThings 的 JSON 语法。查看[文档](docs/zh_cn/目录.md)获取完整说明。

## 模组文件使用与再分发

模组文件（`.jar`）可被自由使用、复制和再分发，无需事先许可，适用于任何整合包、服务器或其他项目。

对于通过 jarjar 嵌入 JsonMore 的模组开发者，请在 `META-INF/jarjar/metadata.json` 中使用以下配置：

```json
{
  "jars": [
    {
      "identifier": {
        "group": "qikahome.jsonmore",
        "artifact": "jsonmore"
      },
      "version": {
        "range": "[<version>,)",
        "artifactVersion": "<version>"
      },
      "path": "META-INF/jarjar/jsonmore-<version>.jar"
    }
  ]
}
```

## 许可证

本项目采用 MIT 许可证 - 详情请参阅 [LICENSE](LICENSE) 文件。

## 致谢

- 感谢 gigaherz 创建了 JsonThings
- 感谢所有为 JsonThings 和相关模组做出贡献的开发者
