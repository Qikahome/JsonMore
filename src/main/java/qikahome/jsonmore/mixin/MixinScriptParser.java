package qikahome.jsonmore.mixin;

import dev.gigaherz.jsonthings.things.scripting.ScriptParser;
import dev.gigaherz.jsonthings.things.scripting.ThingScript;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qikahome.jsonmore.minecraft.jsonscript.JsonScriptParser;
import qikahome.jsonmore.minecraft.jsonscript.ScriptFlexEventHandler;

import java.util.Map;

/**
 * 混入 {@link ScriptParser}，让没有 Rhino 时也能启用脚本事件系统，
 * 并将 JSON 脚本桥接到 JsonThings 的事件系统。
 */
@Mixin(ScriptParser.class)
public abstract class MixinScriptParser {

    /**
     * 始终返回 {@code true}，使 {@code constructEventHandlers} 能正常处理
     * 事物中的事件引用（如 {@code "use": "testpack:plank_test"}）。
     */
    @Inject(method = "isEnabled", at = @At("RETURN"), cancellable = true, remap = false)
    private static void onIsEnabled(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * 在 {@code getEvent} 开头检查 JSON 脚本表。
     * <p>
     * 如果 JSON 脚本表中存在该 ID，直接返回对应的 {@link ScriptFlexEventHandler}；
     * 否则放行让原方法继续（抛出 KeyNotFoundException）。
     */
    @Inject(method = "getEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetEvent(ResourceLocation id, CallbackInfoReturnable<ThingScript> cir) {
        JsonScriptParser parser = JsonScriptParser.INSTANCE;
        if (parser == null) return;
        Map<ResourceLocation, ScriptFlexEventHandler> scripts = parser.getScripts();
        ScriptFlexEventHandler handler = scripts.get(id);
        if (handler != null) {
            cir.setReturnValue(handler);
        }
    }
}
