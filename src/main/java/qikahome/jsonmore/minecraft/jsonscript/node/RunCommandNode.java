package qikahome.jsonmore.minecraft.jsonscript.node;

import com.google.gson.JsonObject;
import qikahome.jsonmore.minecraft.jsonscript.*;
import qikahome.jsonmore.minecraft.jsonscript.variable.NullVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.NumberVariable;
import qikahome.jsonmore.minecraft.jsonscript.variable.ServerVariable;

import javax.annotation.Nullable;

import static qikahome.jsonmore.JsonMore.LOGGER;

/**
 * 执行一条 Minecraft 命令。命令字符串中支持 $(var) 宏替换。
 * <p>
 * JSON 示例：
 * <pre>
 * {"type":"run_command","command":"say hello"}
 * {"type":"run_command","command":"setblock $(block_pos.x) $(block_pos.y) $(block_pos.z) minecraft:stone"}
 * {"type":"run_command","command":"give @s minecraft:diamond 1"}
 * </pre>
 */
public class RunCommandNode implements ScriptNode {

    private final String commandTemplate;
    private final boolean hasPlaceholders;

    /** 构造时判断是否为静态命令（无 $() 占位符）。 */
    public RunCommandNode(String commandTemplate) {
        this.commandTemplate = commandTemplate;
        this.hasPlaceholders = commandTemplate.contains("$(");
    }

    @Override
    public ScriptVariable<?> process(ScriptState state) {
        // 1. 获取服务端
        var srvVar = state.getVar("server");
        if (!(srvVar instanceof ServerVariable sv) || sv.value() == null) {
            LOGGER.warn("服务端不可用，跳过命令执行");
            return NullVariable.INSTANCE;
        }
        var server = sv.value();

        // 2. 解析命令字符串
        String command;
        if (hasPlaceholders) {
            command = ScriptNode.resolveParenthesized(commandTemplate, state);
            if (command == null) {
                state.fail();
                return NullVariable.INSTANCE;
            }
        } else {
            command = commandTemplate;
        }

        // 3. 执行并返回 int 结果（抑制框架反馈，命令自身的提示不受影响）
        int result = server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withSuppressedOutput(), command);
        return new NumberVariable(result);
    }

    static {
        ScriptNode.register("run_command", json -> {
            String cmd = json.get("command").getAsString();
            return new RunCommandNode(cmd);
        });
    }
}
