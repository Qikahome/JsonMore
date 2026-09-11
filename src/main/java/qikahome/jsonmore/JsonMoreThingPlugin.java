package qikahome.jsonmore;

import dev.gigaherz.jsonthings.api.ThingPlugin;
import dev.gigaherz.jsonthings.things.parsers.ThingResourceManager;
import qikahome.jsonmore.minecraft.BuiltInDatapackParser;
import qikahome.jsonmore.minecraft.gamerule.GameRuleParser;

/**
 * JsonMore 对 JsonThings 的扩展（Fabric 侧），对应 Forge 版在 {@code @Mod} 构造器里做的那部分注册。
 *
 * <p>Forge 上"mod 构造"早于"thingpack 加载"是加载器保证的顺序，所以 JsonMore 直接在构造器中
 * 调用 {@code ThingResourceManager#registerParser} 与 {@link JsonMore#onFlexTypesLoad()}。
 * Fabric 各 mod 的 {@code main} 入口点是并行执行的，没有这个保证，故改由 JsonThings 经
 * {@code "jsonthings"} 入口点在解析前、解析后各回调一次（见 {@link ThingPlugin}）。
 */
public class JsonMoreThingPlugin implements ThingPlugin {
    private static GameRuleParser gameRuleParser;
    private static BuiltInDatapackParser builtInDatapackParser;

    @Override
    public void registerThingTypes(ThingResourceManager manager) {
        gameRuleParser = manager.registerParser(new GameRuleParser());
        builtInDatapackParser = manager.registerParser(new BuiltInDatapackParser());

        // 自建注册表条目与自定义方块/物品类型。
        JsonMore.onFlexTypesLoad();
    }

    @Override
    public void afterThingLoading(ThingResourceManager manager) {
        // 两者的 registerValues 依赖解析结果（自定义游戏规则 / 内置数据包）。
        gameRuleParser.registerValues();
        builtInDatapackParser.registerValues();
    }
}
