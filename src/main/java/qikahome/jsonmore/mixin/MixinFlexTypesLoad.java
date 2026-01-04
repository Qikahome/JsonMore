package qikahome.jsonmore.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.gigaherz.jsonthings.things.serializers.FlexBlockType;
import qikahome.jsonmore.JsonMore;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlexBlockType.class)
public class MixinFlexTypesLoad {
    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void registerFlexBlockTypes(CallbackInfo ci) {
        JsonMore.onFlexTypesLoad();
    }
}
