package qikahome.jsonmore.minecraft.jsonscript;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.gigaherz.jsonthings.things.builders.BaseBuilder;
import dev.gigaherz.jsonthings.things.parsers.ThingParser;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link BaseBuilder} 实现，从 JSON 节点列表构建 {@link ScriptFlexEventHandler}。
 * <p>
 * 由 {@link JsonScriptParser} 在加载 thing 数据包时自动创建。
 */
public class JsonScriptBuilder extends BaseBuilder<ScriptFlexEventHandler, JsonScriptBuilder> {

    private List<ScriptNode> nodes = List.of();

    protected JsonScriptBuilder(ThingParser<JsonScriptBuilder> ownerParser, ResourceLocation registryName) {
        super(ownerParser, registryName);
    }

    public void setNodes(List<ScriptNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    protected String getThingTypeDisplayName() {
        return "JSON Script";
    }

    /**
     * 从 JSON 对象的 {@code nodes} 字段解析脚本节点列表。
     */
    public void parseNodes(JsonObject data) {
        if (data.has("nodes")) {
            JsonArray arr = data.getAsJsonArray("nodes");
            List<ScriptNode> list = new ArrayList<>();
            for (JsonElement element : arr) {
                list.add(ScriptNode.parse(element));
            }
            this.nodes = list;
        }
    }

    @Override
    protected ScriptFlexEventHandler buildInternal() {
        return new ScriptFlexEventHandler(getRegistryName(), nodes);
    }
}
