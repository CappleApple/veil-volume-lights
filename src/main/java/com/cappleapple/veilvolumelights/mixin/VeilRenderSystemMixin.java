package com.cappleapple.veilvolumelights.mixin;

import com.cappleapple.veilvolumelights.VeilVolumeLightsMod;
import com.cappleapple.veilvolumelights.client.render.TransparentMediumBuffer;
import com.cappleapple.veilvolumelights.client.render.TransparentMediumRenderState;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.lwjgl.opengl.GL11C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL30C.GL_COLOR_ATTACHMENT0;

@Mixin(value = VeilRenderSystem.class, remap = false)
abstract class VeilRenderSystemMixin {
    @Unique private static final ResourceLocation VEILVOLUMELIGHTS$TRANSPARENT_MEDIUM =
            ResourceLocation.fromNamespaceAndPath(VeilVolumeLightsMod.MOD_ID, "transparent_medium");
    @Unique private static final ResourceLocation VEILVOLUMELIGHTS$TRANSMITTED_SURFACE =
            ResourceLocation.fromNamespaceAndPath(VeilVolumeLightsMod.MOD_ID, "transmitted_surface");

    @Inject(method = "clearTextureSupported", at = @At("HEAD"), cancellable = true)
    private static void veilvolumelights$useFramebufferClear(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "compositeLights", at = @At("HEAD"))
    private static void veilvolumelights$renderTransparentMedium(
            ProfilerFiller profiler, CallbackInfo ci) {
        if (TransparentMediumRenderState.isFirstPersonComposite()) {
            veilvolumelights$clearFramebuffer(VEILVOLUMELIGHTS$TRANSPARENT_MEDIUM);
            return;
        }
        PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
        TransparentMediumBuffer.bind();
        veilvolumelights$runPipeline(manager, VEILVOLUMELIGHTS$TRANSPARENT_MEDIUM);
        veilvolumelights$runPipeline(manager, VEILVOLUMELIGHTS$TRANSMITTED_SURFACE);
    }

    @Unique
    private static void veilvolumelights$runPipeline(
            PostProcessingManager manager, ResourceLocation pipelineId) {
        PostPipeline pipeline = manager.getPipeline(pipelineId);
        if (pipeline != null) {
            TransparentMediumBuffer.bind();
            manager.runPipeline(pipeline, false);
        }
    }

    @Unique
    private static void veilvolumelights$clearFramebuffer(ResourceLocation framebufferId) {
        AdvancedFbo framebuffer = VeilRenderSystem.renderer().getFramebufferManager()
                .getFramebuffer(framebufferId);
        if (framebuffer != null) {
            framebuffer.clear(GL_COLOR_BUFFER_BIT, GL_COLOR_ATTACHMENT0);
        }
    }
}
