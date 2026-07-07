package qikahome.jsonmore.minecraft.jsonscript.variable;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import qikahome.jsonmore.minecraft.jsonscript.ScriptVariable;

/**
 * 服务端变量，封装 {@link MinecraftServer} 供节点访问。
 * <p>
 * 通过此变量可以执行命令。
 */
public class ServerVariable extends ScriptVariable<MinecraftServer> {

    public ServerVariable(@Nullable MinecraftServer value) {
        super(value);
    }

    @Override
    public String toScriptString() {
        throw new UnsupportedOperationException("ServerVariable 不能用于命令字符串");
    }

    @Override
    public String type() {
        return "Server";
    }
}
