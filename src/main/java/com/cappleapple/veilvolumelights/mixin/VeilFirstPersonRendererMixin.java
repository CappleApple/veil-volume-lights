package com.cappleapple.veilvolumelights.mixin;

import com.cappleapple.veilvolumelights.client.render.TransparentMediumRenderState;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.impl.client.render.pipeline.VeilFirstPersonRenderer;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = VeilFirstPersonRenderer.class, remap = false)
abstract class VeilFirstPersonRendererMixin {
    @Redirect(method = "unbind", at = @At(value = "INVOKE",
            target = "Lfoundry/veil/api/client/render/VeilRenderSystem;compositeLights(Lnet/minecraft/util/profiling/ProfilerFiller;)V"))
    private static void veilvolumelights$markFirstPersonComposite(ProfilerFiller profiler) {
        TransparentMediumRenderState.runFirstPersonComposite(
                () -> VeilRenderSystem.compositeLights(profiler));
    }
}
