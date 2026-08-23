package com.cappleapple.veilvolumelights.client.render;

import com.cappleapple.veilvolumelights.api.client.VolumeLight;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.AreaLightData;
import foundry.veil.api.client.render.light.data.LightData;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.data.SpotLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

public final class ManagedVolumeLight {
    private static int nextRenderId = 1;

    private final int renderId = nextRenderId++;
    private VolumeLight definition;
    private LightData veilLight;
    private LightRenderHandle<?> veilHandle;
    private List<TransparentMediumBuffer.Medium> detectedMedia = List.of();
    private Collection<TransparentMediumBuffer.Volume> transmissionMedia = List.of();
    private final Vector3f lastScanPosition = new Vector3f();
    private final Vector3f lastScanDirection = new Vector3f();
    private int scanCountdown;
    private boolean scanned;

    public static ManagedVolumeLight create(VolumeLight definition) {
        ManagedVolumeLight light = new ManagedVolumeLight();
        light.definition = definition;
        light.recreateVeilLight();
        light.update(definition);
        VolumeLightRenderManager.register(light);
        return light;
    }

    public void update(VolumeLight updated) {
        if (definition != null && definition.getClass() != updated.getClass()) {
            definition = updated;
            recreateVeilLight();
            scanned = false;
        } else {
            definition = updated;
            ensureHandle();
        }

        if (updated instanceof VolumeLight.Point point) {
            updatePoint(point);
        } else if (updated instanceof VolumeLight.Spot spot) {
            updateSpot(spot);
        } else if (updated instanceof VolumeLight.Area area) {
            updateArea(area);
        }
        updateTransmissionMedia();
    }

    public boolean isValid() {
        return veilHandle != null && veilHandle.isValid();
    }

    public VolumeLight definition() {
        return definition;
    }

    public boolean hasVolumetricBeam() {
        return definition != null && definition.volumetricStrength() > 0.0F;
    }

    public boolean hasTransmissionMedia() {
        return !transmissionMedia.isEmpty();
    }

    Collection<TransparentMediumBuffer.Volume> transmissionMedia() {
        return transmissionMedia;
    }

    public void free() {
        VolumeLightRenderManager.unregister(this);
        freeVeilHandle();
        detectedMedia = List.of();
        transmissionMedia = List.of();
    }

    private void updatePoint(VolumeLight.Point point) {
        PointLightData data = (PointLightData) veilLight;
        data.setPosition(point.position().x, point.position().y, point.position().z)
                .setRadius(point.range())
                .setBrightness(point.intensity())
                .setColor(point.color())
                .setOcclusionEnabled(point.shadows())
                .setInscatteringStrength(point.volumetricStrength());
    }

    private void updateSpot(VolumeLight.Spot spot) {
        SpotLightData data = (SpotLightData) veilLight;
        float outerHalfAngle = (float) Math.toRadians(spot.outerConeAngleDegrees() * 0.5F);
        float innerHalfAngle = (float) Math.toRadians(spot.innerConeAngleDegrees() * 0.5F);
        float falloffWidth = Math.max(0.0001F, outerHalfAngle - innerHalfAngle);
        data.getPositionMutable().set(spot.position().x, spot.position().y, spot.position().z);
        orient(data.getOrientationMutable(), spot.forward(), spot.up());
        data.setSize(outerHalfAngle)
                .setAngle(falloffWidth)
                .setDistance(spot.range())
                .setBrightness(spot.intensity())
                .setColor(spot.color())
                .setOcclusionEnabled(spot.shadows())
                .setInscatteringStrength(spot.volumetricStrength());
    }

    private void updateArea(VolumeLight.Area area) {
        AreaLightData data = (AreaLightData) veilLight;
        data.getPositionMutable().set(area.position().x, area.position().y, area.position().z);
        orient(data.getOrientationMutable(), area.forward(), area.up());
        data.setSize(area.width() * 0.5F, area.height() * 0.5F)
                .setAngle((float) Math.toRadians(area.spreadAngleDegrees() * 0.5F))
                .setDistance(area.range())
                .setBrightness(area.intensity())
                .setColor(area.color())
                .setOcclusionEnabled(area.shadows())
                .setInscatteringStrength(area.volumetricStrength());
    }

    private void updateTransmissionMedia() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || definition == null) {
            detectedMedia = List.of();
            transmissionMedia = List.of();
            scanned = false;
            return;
        }

        Vector3f direction = direction(definition);
        boolean moved = !scanned || definition.position().distanceSquared(lastScanPosition) > 0.0625F;
        boolean turned = !scanned || direction.dot(lastScanDirection) < 0.9986F;
        if (--scanCountdown <= 0 || moved || turned) {
            detectedMedia = TransparentMediumDetector.find(level, definition);
            lastScanPosition.set(definition.position());
            lastScanDirection.set(direction);
            scanCountdown = 5;
            scanned = true;
        }
        transmissionMedia = detectedMedia.stream()
                .map(medium -> medium.forLight(definition, renderId))
                .toList();
    }

    private void recreateVeilLight() {
        freeVeilHandle();
        veilLight = switch (definition) {
            case VolumeLight.Point ignored -> new PointLightData();
            case VolumeLight.Spot ignored -> new SpotLightData();
            case VolumeLight.Area ignored -> new AreaLightData();
        };
        veilHandle = VeilRenderSystem.renderer().getLightRenderer().addLight(veilLight);
    }

    private void ensureHandle() {
        if (!isValid()) {
            veilHandle = VeilRenderSystem.renderer().getLightRenderer().addLight(veilLight);
        }
    }

    private void freeVeilHandle() {
        if (veilHandle != null) {
            veilHandle.free();
            veilHandle = null;
        }
    }

    private static Vector3f direction(VolumeLight light) {
        if (light instanceof VolumeLight.Spot spot) {
            return new Vector3f(spot.forward());
        }
        if (light instanceof VolumeLight.Area area) {
            return new Vector3f(area.forward());
        }
        return new Vector3f(0.0F, 1.0F, 0.0F);
    }

    private static void orient(Quaternionf orientation, Vector3f forward, Vector3f up) {
        // Veil's local emitter direction is +Z; JOML lookAlong aligns local -Z.
        orientation.identity().lookAlong(new Vector3f(forward).negate(), up);
    }

    private ManagedVolumeLight() {
    }
}
