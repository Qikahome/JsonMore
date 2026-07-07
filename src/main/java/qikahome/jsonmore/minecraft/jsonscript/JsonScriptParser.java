package qikahome.jsonmore.minecraft.jsonscript;

import com.google.gson.JsonObject;
import dev.gigaherz.jsonthings.things.parsers.ThingParser;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 加载 {@code things/jsonscript/} 目录下的 JSON 脚本文件，解析为 {@link ScriptFlexEventHandler}。
 * <p>
 * JSON 格式示例（放在 {@code data/&lt;namespace&gt;/things/jsonscript/&lt;path&gt;.json}）：
 * <pre>
 * {
 *   "nodes": [
 *     { "type": "set_var", "name": "msg", "value": "Hello" },
 *     { "type": "run_command", "command": "say $(msg)" }
 *   ]
 * }
 * </pre>
 * 脚本的 ID 为 {@code &lt;namespace&gt;:&lt;path&gt;}，可被 JsonThings 的事件系统
 * 通过 {@code ScriptParser#getEvent(ResourceLocation)} 引用（由 Mixin 桥接）。
 */
public class JsonScriptParser extends ThingParser<JsonScriptBuilder> {

    public static final Logger LOGGER = LogManager.getLogger();

    /** 全局实例，供 Mixin 从外部访问已加载的脚本表。 */
    public static JsonScriptParser INSTANCE;

    /** 已加载的 JSON 脚本表，key = 脚本 ID，value = 构建好的处理器。 */
    private final Map<ResourceLocation, ScriptFlexEventHandler> scripts = new HashMap<>();

    public JsonScriptParser() {
        super(GSON, "jsonscript");
        ScriptNode.loadBuiltinNodes();
        INSTANCE = this;
    }

    /**
     * 返回不可变的已加载脚本表快照。供 Mixin 查找使用。
     */
    public Map<ResourceLocation, ScriptFlexEventHandler> getScripts() {
        return Collections.unmodifiableMap(scripts);
    }

    @Override
    public JsonScriptBuilder processThing(ResourceLocation key, JsonObject data,
                                          Consumer<JsonScriptBuilder> builderModification) {
        final JsonScriptBuilder builder = new JsonScriptBuilder(this, key);
        builder.parseNodes(data);
        builderModification.accept(builder);
        return builder;
    }

    @Override
    protected void finishLoadingInternal() {
        scripts.clear();
        for (var builder : getBuilders()) {
            ScriptFlexEventHandler handler = builder.build();
            scripts.put(builder.getRegistryName(), handler);
        }
        LOGGER.info("Loaded {} JSON script(s): {}", scripts.size(), scripts.keySet());
    }
}
