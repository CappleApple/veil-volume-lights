package com.cappleapple.veilvolumelights.client.render;

import com.cappleapple.veilvolumelights.api.client.VolumeLight;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.List;

/** GPU data shared by Veil's native light shaders and the analytical medium pass. */
public final class TransparentMediumBuffer {
    public static final int MAX_MEDIA = 96;
    public static final int VEC4S_PER_VOLUME = 8;
    private static final int BINDING = 7;
    private static final int FLOATS_PER_VOLUME = VEC4S_PER_VOLUME * 4;
    private static int bufferId;

    public static void upload(Collection<Volume> volumes) {
        List<Volume> snapshot = volumes.stream().limit(MAX_MEDIA).toList();
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> upload(snapshot));
            return;
        }

        ensureBuffer();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer data = stack.mallocFloat(4 + snapshot.size() * FLOATS_PER_VOLUME);
            data.put((float) snapshot.size()).put(0.0F).put(0.0F).put(0.0F);
            for (Volume volume : snapshot) {
                data.put(volume.source.x).put(volume.source.y).put(volume.source.z).put(volume.tint.x);
                data.put(volume.minimum.x).put(volume.minimum.y).put(volume.minimum.z).put(volume.tint.y);
                data.put(volume.maximum.x).put(volume.maximum.y).put(volume.maximum.z).put(volume.tint.z);
                data.put(volume.throughput).put(volume.densityBoost)
                        .put(volume.shape.shaderId).put(volume.volumetricStrength);
                data.put(volume.forward.x).put(volume.forward.y).put(volume.forward.z).put(volume.range);
                data.put(volume.up.x).put(volume.up.y).put(volume.up.z).put(volume.outerHalfAngle);
                data.put(volume.lightColor.x).put(volume.lightColor.y).put(volume.lightColor.z)
                        .put(volume.innerHalfAngle);
                data.put(volume.areaHalfWidth).put(volume.areaHalfHeight)
                        .put((float) volume.ownerId)
                        .put(volume.requiresSurfacePass ? 1.0F : 0.0F);
            }
            data.flip();

            GL43C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, bufferId);
            GL43C.glBufferData(GL43C.GL_SHADER_STORAGE_BUFFER, data, GL43C.GL_DYNAMIC_DRAW);
            GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING, bufferId);
            GL43C.glBindBuffer(GL43C.GL_SHADER_STORAGE_BUFFER, 0);
        }
    }

    public static void bind() {
        if (bufferId != 0) {
            GL43C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, BINDING, bufferId);
        }
    }

    public static void clear() {
        upload(List.of());
    }

    private static void ensureBuffer() {
        if (bufferId == 0) {
            bufferId = GL43C.glGenBuffers();
        }
    }

    public record Medium(
            Vector3f minimum,
            Vector3f maximum,
            Vector3f tint,
            float throughput,
            float densityBoost,
            boolean requiresSurfacePass
    ) {
        Volume forLight(VolumeLight light, int ownerId) {
            Vector3f forward = new Vector3f(0.0F, 1.0F, 0.0F);
            Vector3f up = new Vector3f(0.0F, 0.0F, 1.0F);
            Shape shape = Shape.POINT;
            float outerHalfAngle = (float) Math.PI;
            float innerHalfAngle = (float) Math.PI;
            float areaHalfWidth = 0.0F;
            float areaHalfHeight = 0.0F;
            if (light instanceof VolumeLight.Spot spot) {
                shape = Shape.SPOT;
                forward.set(spot.forward());
                up.set(spot.up());
                outerHalfAngle = (float) Math.toRadians(spot.outerConeAngleDegrees() * 0.5F);
                innerHalfAngle = (float) Math.toRadians(spot.innerConeAngleDegrees() * 0.5F);
            } else if (light instanceof VolumeLight.Area area) {
                shape = Shape.AREA;
                forward.set(area.forward());
                up.set(area.up());
                outerHalfAngle = (float) Math.toRadians(area.spreadAngleDegrees() * 0.5F);
                innerHalfAngle = outerHalfAngle * 0.72F;
                areaHalfWidth = area.width() * 0.5F;
                areaHalfHeight = area.height() * 0.5F;
            }
            return new Volume(
                    new Vector3f(light.position()),
                    new Vector3f(minimum),
                    new Vector3f(maximum),
                    new Vector3f(tint),
                    throughput,
                    densityBoost,
                    shape,
                    new Vector3f(forward).normalize(),
                    new Vector3f(up).normalize(),
                    light.range(),
                    outerHalfAngle,
                    innerHalfAngle,
                    areaHalfWidth,
                    areaHalfHeight,
                    new Vector3f(light.color()).mul(light.intensity()),
                    light.volumetricStrength(),
                    ownerId,
                    requiresSurfacePass
            );
        }
    }

    enum Shape {
        POINT(0.0F),
        SPOT(1.0F),
        AREA(2.0F);

        private final float shaderId;

        Shape(float shaderId) {
            this.shaderId = shaderId;
        }
    }

    public record Volume(
            Vector3f source,
            Vector3f minimum,
            Vector3f maximum,
            Vector3f tint,
            float throughput,
            float densityBoost,
            Shape shape,
            Vector3f forward,
            Vector3f up,
            float range,
            float outerHalfAngle,
            float innerHalfAngle,
            float areaHalfWidth,
            float areaHalfHeight,
            Vector3f lightColor,
            float volumetricStrength,
            int ownerId,
            boolean requiresSurfacePass
    ) {
    }

    private TransparentMediumBuffer() {
    }
}
