package com.cappleapple.veilvolumelights.client.render;

import com.cappleapple.veilvolumelights.VeilVolumeLightsMod;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class VolumeLightRenderManager {
    private static final ResourceLocation TRANSMITTED_SURFACE_APPLY =
            ResourceLocation.fromNamespaceAndPath(
                    VeilVolumeLightsMod.MOD_ID, "transmitted_surface_apply");
    private static final Set<ManagedVolumeLight> LIGHTS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    static void register(ManagedVolumeLight light) {
        LIGHTS.add(light);
    }

    static void unregister(ManagedVolumeLight light) {
        LIGHTS.remove(light);
    }

    public static void uploadTransparentMedia() {
        var volumes = new ArrayList<TransparentMediumBuffer.Volume>();
        LIGHTS.forEach(light -> volumes.addAll(light.transmissionMedia()));
        TransparentMediumBuffer.upload(volumes);
    }

    public static boolean needsOpaqueDepthCapture() {
        return LIGHTS.stream().anyMatch(light ->
                light.hasVolumetricBeam() || light.hasTransmissionMedia());
    }

    public static void applyTransmittedSurface() {
        if (LIGHTS.stream().noneMatch(ManagedVolumeLight::hasTransmissionMedia)) {
            return;
        }
        PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
        PostPipeline pipeline = manager.getPipeline(TRANSMITTED_SURFACE_APPLY);
        if (pipeline != null) {
            TransparentMediumBuffer.bind();
            manager.runPipeline(pipeline, false);
        }
    }

    public static void clear() {
        for (ManagedVolumeLight light : new ArrayList<>(LIGHTS)) {
            light.free();
        }
        LIGHTS.clear();
        TransparentMediumBuffer.clear();
    }

    private VolumeLightRenderManager() {
    }
}
