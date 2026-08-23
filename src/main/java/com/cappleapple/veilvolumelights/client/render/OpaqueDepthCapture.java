package com.cappleapple.veilvolumelights.client.render;

import com.cappleapple.veilvolumelights.VeilVolumeLightsMod;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferAttachmentDefinition;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import static org.lwjgl.opengl.GL11C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11C.GL_NEAREST;
import static org.lwjgl.opengl.GL11C.glGetInteger;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL21C.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL21C.GL_PIXEL_UNPACK_BUFFER_BINDING;

/** Captures opaque-only depth before translucent blocks render. */
public final class OpaqueDepthCapture {
    public static final ResourceLocation FRAMEBUFFER_ID = ResourceLocation.fromNamespaceAndPath(
            VeilVolumeLightsMod.MOD_ID, "opaque_depth");
    private static AdvancedFbo opaqueDepth;
    private static int sharedColorTexture;
    private static boolean stencil;
    private static boolean captured;

    public static void beginFrame() {
        captured = false;
    }

    public static void capture(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES
                || !VolumeLightRenderManager.needsOpaqueDepthCapture()) {
            return;
        }
        AdvancedFbo main = AdvancedFbo.getMainFramebuffer();
        ensureFramebuffer(main);
        main.resolveToAdvancedFbo(opaqueDepth, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        captured = true;
    }

    public static void copyToInscattering(AdvancedFbo target) {
        if (captured && opaqueDepth != null
                && opaqueDepth.getWidth() == target.getWidth()
                && opaqueDepth.getHeight() == target.getHeight()) {
            opaqueDepth.resolveToAdvancedFbo(target, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        }
    }

    public static void clear() {
        captured = false;
        AdvancedFbo old = VeilRenderSystem.renderer().getFramebufferManager().removeFramebuffer(FRAMEBUFFER_ID);
        if (old == null) {
            old = opaqueDepth;
        }
        opaqueDepth = null;
        sharedColorTexture = 0;
        if (old != null) {
            if (RenderSystem.isOnRenderThread()) {
                old.free();
            } else {
                AdvancedFbo finalOld = old;
                RenderSystem.recordRenderCall(finalOld::free);
            }
        }
    }

    private static void ensureFramebuffer(AdvancedFbo main) {
        int colorTexture = main.getColorTextureAttachment(0).getId();
        boolean hasStencil = main.hasStencilAttachment();
        if (opaqueDepth != null && opaqueDepth.getWidth() == main.getWidth()
                && opaqueDepth.getHeight() == main.getHeight()
                && sharedColorTexture == colorTexture && stencil == hasStencil) {
            return;
        }
        clear();
        sharedColorTexture = colorTexture;
        stencil = hasStencil;
        int pixelUnpackBuffer = glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING);
        if (pixelUnpackBuffer != 0) {
            glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        }
        try {
            opaqueDepth = AdvancedFbo.withSize(main.getWidth(), main.getHeight())
                    .addColorTextureWrapper(colorTexture)
                    .setFormat(hasStencil
                            ? FramebufferAttachmentDefinition.Format.DEPTH32F_STENCIL8
                            : FramebufferAttachmentDefinition.Format.DEPTH_COMPONENT)
                    .setDepthTextureBuffer()
                    .setDebugLabel("Veil Volume Lights Opaque Depth")
                    .build(true);
            VeilRenderSystem.renderer().getFramebufferManager().setFramebuffer(FRAMEBUFFER_ID, opaqueDepth);
        } finally {
            if (pixelUnpackBuffer != 0) {
                glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pixelUnpackBuffer);
            }
        }
    }

    private OpaqueDepthCapture() {
    }
}
