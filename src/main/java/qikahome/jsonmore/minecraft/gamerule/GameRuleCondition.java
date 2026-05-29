package qikahome.jsonmore.minecraft.gamerule;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

import qikahome.jsonmore.Utils;

public class GameRuleCondition implements ICondition {
    public static final ResourceLocation ID = new ResourceLocation("jsonmore:gamerule");

    private final String ruleName;
    private final String valueRange;

    public GameRuleCondition(String ruleName, String valueRange) {
        this.ruleName = ruleName;
        this.valueRange = valueRange;
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return false;

        GameRules.Key<?> foundKey = findKey(ruleName);
        if (foundKey == null)
            return false;

        GameRules.Value<?> value = server.getGameRules().getRule(foundKey);
        if (value == null)
            return false;

        if (valueRange == null) {
            if (value instanceof GameRules.BooleanValue bv)
                return bv.get();
            return false;
        }

        if (value instanceof GameRules.IntegerValue iv) {
            try {
                int exact = Integer.parseInt(valueRange);
                return iv.get() == exact;
            } catch (NumberFormatException e) {
                Utils.IntRange range = Utils.IntRange.parse(valueRange);
                return range.contains(iv.get());
            }
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

    public static class Serializer implements IConditionSerializer<GameRuleCondition> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, GameRuleCondition condition) {
            json.addProperty("rule", condition.ruleName);
            if (condition.valueRange != null) {
                try {
                    int exact = Integer.parseInt(condition.valueRange);
                    json.addProperty("value", exact);
                } catch (NumberFormatException e) {
                    json.addProperty("value", condition.valueRange);
                }
            }
        }

        @Override
        public GameRuleCondition read(JsonObject json) {
            String rule = GsonHelper.getAsString(json, "rule");
            String valueRange = null;
            if (json.has("value")) {
                var valueEl = json.get("value");
                if (valueEl.isJsonPrimitive() && valueEl.getAsJsonPrimitive().isNumber()) {
                    int exact = valueEl.getAsInt();
                    valueRange = "[" + exact + "," + exact + "]";
                } else {
                    valueRange = GsonHelper.getAsString(json, "value");
                }
            }
            return new GameRuleCondition(rule, valueRange);
        }

        @Override
        public ResourceLocation getID() {
            return GameRuleCondition.ID;
        }
    }
}
