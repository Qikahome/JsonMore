package qikahome.jsonmore.minecraft.jsonscript.node;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.world.item.crafting.Ingredient;
import qikahome.jsonmore.minecraft.jsonscript.ScriptNode;
import qikahome.jsonmore.minecraft.jsonscript.ScriptState;
import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.BooleanVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.ItemStackVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.NullVariable;

/**
 * 物品谓词节点。判断物品是否匹配指定的 ingredient。
 * <p>
 * JSON 示例：
 * <pre>
 * {"type":"item_predicate","item":"$held_item","predicate":{"item":"minecraft:diamond"}}
 * {"type":"item_predicate","item":"$held_item","predicate":{"tag":"forge:gems/diamond"}}
 * {"type":"item_predicate","item":"$held_item","predicate":[
 *     {"item":"minecraft:diamond"},
 *     {"item":"minecraft:emerald"}
 * ]}
 * </pre>
 * <p>
 * ingredient 在运行时才从 JSON 解析，避免数据包加载时注册表尚未就绪导致解析失败。
 */
public class ItemPredicateNode implements ScriptNode {

    private final ScriptNode itemNode;
    private final JsonElement predicateJson;
    private Ingredient ingredient;

    public ItemPredicateNode(ScriptNode itemNode, JsonElement predicateJson) {
        this.itemNode = itemNode;
        this.predicateJson = predicateJson;
    }

    private Ingredient getIngredient() {
        if (ingredient == null) {
            ingredient = Ingredient.fromJson(predicateJson, false);
        }
        return ingredient;
    }

    @Override
    public ScriptVariable<?> process(ScriptState state) {
        ScriptVariable<?> itemVar = itemNode.process(state);
        if (itemVar instanceof NullVariable || !(itemVar instanceof ItemStackVariable isv)) {
            return new BooleanVariable(false);
        }
        var stack = isv.value();
        if (stack == null || stack.isEmpty()) {
            return new BooleanVariable(false);
        }
        return new BooleanVariable(getIngredient().test(stack));
    }

    static {
        ScriptNode.register("item_predicate", json -> {
            ScriptNode itemNode = ScriptNode.parse(json.get("item"));
            JsonElement predicate = json.get("predicate");
            if (predicate == null) {
                throw new IllegalArgumentException("item_predicate 节点缺少 'predicate' 字段");
            }
            return new ItemPredicateNode(itemNode, predicate);
        });
    }
}
