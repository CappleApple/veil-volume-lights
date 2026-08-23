package com.cappleapple.veilvolumelights.mixin;

import foundry.veil.api.client.render.framebuffer.AdvancedFboTextureAttachment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL30C.*;

@Mixin(value = AdvancedFboTextureAttachment.class, remap = false)
abstract class AdvancedFboTextureAttachmentMixin {
    @Shadow private int internalFormat;

    @ModifyArg(method = "create",
            at = @At(value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL30C;glTexImage2D(IIIIIIIILjava/nio/ByteBuffer;)V"),
            index = 7)
    private int veilvolumelights$usePackedDepthStencilType(int originalType) {
        if (this.internalFormat == GL_DEPTH24_STENCIL8 && originalType == GL_UNSIGNED_BYTE) {
            return GL_UNSIGNED_INT_24_8;
        }
        if (this.internalFormat == GL_DEPTH32F_STENCIL8 && originalType == GL_UNSIGNED_BYTE) {
            return GL_FLOAT_32_UNSIGNED_INT_24_8_REV;
        }
        return originalType;
    }
}
