# JsonMore 滚动容器实现总结

## 项目目标
为 Minecraft Forge 1.20.1 实现支持任意槽数的滚动容器系统，替代原有的 `FilteredChestMenu`。

## 背景
原项目使用 `FilteredChestMenu` 处理容器，但该实现不支持超过54槽的容器。用户要求：
1. 实现滚动容器支持任意槽数
2. 所有容器都使用新的滚动菜单
3. 物品显示内容物（类似潜影盒）

## 核心文件

### 1. ScrollingContainerMenu.java
服务端菜单类，支持动态滚动：
- `VISIBLE_ROWS = 6` - 可见行数
- `SLOTS_PER_ROW = 9` - 每行槽数  
- `VISIBLE_SLOTS = 54` - 可见槽位总数
- `ScrollableSlot` - 自定义槽位，重写 `getX()`/`getY()` 实现动态位置
- `create(FriendlyByteBuf)` - 客户端工厂，从 buffer 读取 `totalSlots`
- `createSimple(int, Inventory)` - 默认工厂方法
- `updateSlotPositions()` - 根据滚动偏移更新槽位显示位置和索引

关键实现：
```java
public static class ScrollableSlot extends Slot {
    private int displayX, displayY;
    private int actualSlotIndex;
    
    @Override public int getX() { return displayX; }
    @Override public int getY() { return displayY; }
    @Override public int getContainerSlot() { return actualSlotIndex; }
    
    @Override public ItemStack getItem() {
        return container.getItem(actualSlotIndex);
    }
    @Override public void set(ItemStack stack) {
        container.setItem(actualSlotIndex, stack);
    }
    @Override public ItemStack remove(int amount) {
        return container.removeItem(actualSlotIndex, amount);
    }
}
```

### 2. ScrollingContainerScreen.java
客户端界面类：
- 继承 `AbstractContainerScreen<ScrollingContainerMenu>`
- 渲染滚动条（`renderScrollbar()`）
- 处理鼠标滚轮（`mouseScrolled()`）和拖拽（`mouseClicked()`/`mouseDragged()`）
- 显示页码信息（`renderLabels()`）
- 背景纹理：`textures/gui/container/generic_54.png`

### 3. FlexBarrelBlock.java
容器方块，原有基础上修改：
- `createMenu()` - 创建 `ScrollingContainerMenu` 而非 `ChestMenu`
- `appendHoverText()` - 显示物品内容物（类似潜影盒）
- `playerWillDestroy()` - 处理保留物品栏模式

### 4. FlexBarrelBlockEntity.java
方块实体：
- 继承 `RandomizableContainerBlockEntity implements WorldlyContainer`
- `createMenu()` 委托给 `FlexBarrelBlock`

### 5. MinecraftPlugin.java
注册自定义类型：
```java
// 菜单类型注册
public static RegistryObject<MenuType<ScrollingContainerMenu>> SCROLLING_CONTAINER_MENU;

public static MenuType<ScrollingContainerMenu> createFactory() {
    return new MenuType<>(ScrollingContainerMenu::createSimple, null);
}

// 容器方块注册
FlexBlockType.register("jsonmore:container", data -> {
    int slots = GsonHelper.getAsInt(data, "slots", 27);
    // ... 解析其他参数
    return (props, builder) -> new FlexBarrelBlock(props, propertyDefaultValues, slots, ...);
});
```

### 6. JsonMore.java
主类，注册：
```java
public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister
    .create(ForgeRegistries.MENU_TYPES, MODID);

static {
    MinecraftPlugin.SCROLLING_CONTAINER_MENU = MENU_TYPES.register("scrolling_container",
        MinecraftPlugin::createFactory);
}

// 客户端注册
@SubscribeEvent
public static void onClientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> {
        net.minecraft.client.gui.screens.MenuScreens.register(
            MinecraftPlugin.SCROLLING_CONTAINER_MENU.get(),
            ScrollingContainerScreen::new
        );
    });
}
```

## 已修复的问题

### 编译错误
1. `ForgeRegistries.MENUS` → `ForgeRegistries.MENU_TYPES`
2. `ModList.get.isLoaded` → `ModList.get().isLoaded`
3. `MenuScreens` 包路径：`net.minecraftforge.client.event.MenuScreens` → `net.minecraft.client.gui.screens.MenuScreens`
4. `createFactory()` 改为 public
5. 添加 `ScrollingContainerScreen` 导入

### 客户端 NPE
错误：`Cannot invoke "Container.setItem" because "this.container" is null`

修复：
- `create()` 和 `createSimple()` 使用 `SimpleContainer(totalSlots)` 创建临时容器
- `ScrollableSlot` 方法添加 null 检查

### 物品内容物显示
在 `FlexBarrelBlock.appendHoverText()` 实现：
```java
@Override
public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
    super.appendHoverText(stack, level, tooltip, flag);
    CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
    if (blockEntityTag != null && blockEntityTag.contains("Items")) {
        ListTag itemsList = blockEntityTag.getList("Items", 10);
        if (!itemsList.isEmpty()) {
            tooltip.add(Component.translatable("container.shulkerBox.contains", itemsList.size(), containerSize));
            // 显示前5个物品...
        }
    }
}
```

删除了不必要的 `FlexBarrelItem`。

### mouseScrolled 方法签名
原错误：4参数 `(mouseX, mouseY, scrollX, scrollY)`
修复：3参数 `(mouseX, mouseY, amount)`

## 待解决问题

1. **显示行数问题**：始终只显示3行
   - 需要检查 `displayRows` 计算逻辑
   - 可能是 `totalSlots` 未正确传递

2. **非27槽容器问题**：
   - 客户端需要正确获取容器大小
   - `create()` 方法从 buffer 读取 `totalSlots`

3. **打开容器无声音**：
   - 需要在 `FlexBarrelBlockEntity` 中实现声音播放
   - 参考 `ContainerOpenersCounter` 的 `onOpen()`/`onClose()`

## 关键设计决策

1. **使用 ScrollableSlot**：通过重写 `getX()`/`getY()` 实现动态位置，避免直接修改 Slot 的 final 字段

2. **客户端使用 SimpleContainer**：避免 NPE，实际数据通过服务器同步

3. **统一使用 ScrollingContainerMenu**：即使容器小于54槽也使用滚动菜单，简化代码

4. **删除 FilteredChestMenu**：功能已完全被 ScrollingContainerMenu 替代

## 下一步
1. 调试 `displayRows` 计算和 `totalSlots` 传递
2. 实现容器打开/关闭声音
3. 测试不同槽数的容器
