package qikahome.jsonmore.lib.ingredient;

public enum NBTCopyMode {
    /**
     * 替换模式：清空目标物品的原有 NBT，完全复制源物品的 NBT
     */
    REPLACE,
    
    /**
     * 合并模式：目标优先
     * 将源物品的 NBT 合并到目标物品，冲突时使用目标物品的值
     */
    MERGE_TARGET_FIRST,
    
    /**
     * 合并模式：源优先
     * 将源物品的 NBT 合并到目标物品，冲突时使用源物品的值
     */
    MERGE_SOURCE_FIRST
}
