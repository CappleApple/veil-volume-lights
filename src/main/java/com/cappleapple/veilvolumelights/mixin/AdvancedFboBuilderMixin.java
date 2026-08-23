package com.cappleapple.veilvolumelights.mixin;

import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.AdvancedFboTextureAttachment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static org.lwjgl.opengl.GL30C.GL_DEPTH_STENCIL;

@Mixin(value = AdvancedFbo.Builder.class, remap = false)
abstract class AdvancedFboBuilderMixin {
    @Shadow private int format;

    @ModifyArg(method = "setDepthTextureBuffer(II)Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo$Builder;",
            at = @At(value = "INVOKE",
                    target = "Lfoundry/veil/api/client/render/framebuffer/AdvancedFboTextureAttachment;<init>(IIIIIILfoundry/veil/api/client/render/texture/TextureFilter;Ljava/lang/String;)V"),
            index = 2)
    private int veilvolumelights$useDepthStencilTextureFormat(int originalFormat) {
        return this.format == GL_DEPTH_STENCIL ? GL_DEPTH_STENCIL : originalFormat;
    }
}
