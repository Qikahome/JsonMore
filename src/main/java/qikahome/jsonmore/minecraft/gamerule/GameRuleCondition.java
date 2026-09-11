package qikahome.jsonmore.minecraft.gamerule;

import java.util.function.Predicate;

import com.google.gson.JsonObject;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.GameRules;

import qikahome.jsonmore.Utils;

/**
 * 资源条件 {@code jsonmore:gamerule}：按游戏规则名（可带取值范围）决定资源是否加载。
 * <p>
 * 对应上游 Forge 的 {@code ICondition}，Fabric 1.20.1 侧的资源条件是 JSON 版，
 * 等价物是 {@link ResourceConditions} 注册的 {@link Predicate}{@code <JsonObject>}。
 * <p>
 * JSON 结构：{@code {"condition": "jsonmore:gamerule", "rule": "...", "value": 可选}}，
 * 其中 {@code value} 为整数时精确匹配，为字符串时按区间（如 {@code "[1,3]"}、{@code "[2,)"}）匹配。
 */
public class GameRuleCondition implements Predicate<JsonObject> {
    public static final ResourceLocation ID = new ResourceLocation("jsonmore:gamerule");

    public static final GameRuleCondition INSTANCE = new GameRuleCondition();

    /**
     * 注册到 Fabric 资源条件系统，由入口在合适时机调用。
     */
    public static void register() {
        ResourceConditions.register(ID, INSTANCE);
    }

    @Override
    public boolean test(JsonObject json) {
        MinecraftServer server = Utils.getCurrentServer();

        if (server == null)
            return false;

        String ruleName = GsonHelper.getAsString(json, "rule");

        GameRules.Key<?> foundKey = findKey(ruleName);
        if (foundKey == null)
            return false;

        GameRules.Value<?> value = server.getGameRules().getRule(foundKey);
        if (value == null)
            return false;

        // 未提供 value：布尔规则直接取布尔值
        if (!json.has("value")) {
            if (value instanceof GameRules.BooleanValue bv)
                return bv.get();
            return false;
        }

        if (value instanceof GameRules.IntegerValue iv) {
            var valueEl = json.get("value");
            // 整数：精确匹配
            if (valueEl.isJsonPrimitive() && valueEl.getAsJsonPrimitive().isNumber())
                return iv.get() == valueEl.getAsInt();
            // 字符串：区间，如 "[1,3]"、"[2,)"
            Utils.IntRange range = Utils.IntRange.parse(valueEl.getAsString());
            return range.contains(iv.get());
        }
        return false;
    }

    private static GameRules.Key<?> findKey(String name) {
        for (var entry : GameRules.GAME_RULE_TYPES.entrySet()) {
            if (entry.getKey().toString().equals(name))
                return entry.getKey();
        }
        return null;
    }
}
