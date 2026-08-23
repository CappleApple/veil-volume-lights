package com.cappleapple.veilvolumelights.mixin;

import com.cappleapple.veilvolumelights.client.render.OpaqueDepthCapture;
import com.cappleapple.veilvolumelights.client.render.TransparentMediumBuffer;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LightRenderer.class, remap = false)
abstract class LightRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void veilvolumelights$bindTransmissionMedia(
            CullFrustum frustum, AdvancedFbo lightFbo, AdvancedFbo lightInscatteringFbo,
            boolean renderInscattering, CallbackInfoReturnable<Boolean> cir) {
        TransparentMediumBuffer.bind();
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lfoundry/veil/api/client/render/light/renderer/InscatteringLightRenderer;renderLightInscattering(Lfoundry/veil/api/client/render/light/renderer/LightRenderer;)V"))
    private void veilvolumelights$useOpaqueDepthForInscattering(
            CullFrustum frustum, AdvancedFbo lightFbo, AdvancedFbo lightInscatteringFbo,
            boolean renderInscattering, CallbackInfoReturnable<Boolean> cir) {
        TransparentMediumBuffer.bind();
        OpaqueDepthCapture.copyToInscattering(lightInscatteringFbo);
    }
}
