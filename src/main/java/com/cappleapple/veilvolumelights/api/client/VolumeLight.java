package com.cappleapple.veilvolumelights.api.client;

import org.joml.Vector3f;

/** Snapshot descriptions accepted by the client-side Veil volume renderer. */
public sealed interface VolumeLight permits VolumeLight.Point, VolumeLight.Spot, VolumeLight.Area {
    Vector3f position();

    Vector3f color();

    float intensity();

    float range();

    boolean shadows();

    float volumetricStrength();

    record Point(
            Vector3f position,
            Vector3f color,
            float intensity,
            float range,
            boolean shadows,
            float volumetricStrength
    ) implements VolumeLight {
        public Point {
            position = new Vector3f(position);
            color = new Vector3f(color);
        }
    }

    record Spot(
            Vector3f position,
            Vector3f forward,
            Vector3f up,
            Vector3f color,
            float intensity,
            float range,
            float outerConeAngleDegrees,
            float innerConeAngleDegrees,
            boolean shadows,
            float volumetricStrength
    ) implements VolumeLight {
        public Spot {
            position = new Vector3f(position);
            forward = new Vector3f(forward).normalize();
            up = orthogonalUp(forward, up);
            color = new Vector3f(color);
        }
    }

    record Area(
            Vector3f position,
            Vector3f forward,
            Vector3f up,
            Vector3f color,
            float intensity,
            float range,
            float width,
            float height,
            float spreadAngleDegrees,
            boolean shadows,
            float volumetricStrength
    ) implements VolumeLight {
        public Area {
            position = new Vector3f(position);
            forward = new Vector3f(forward).normalize();
            up = orthogonalUp(forward, up);
            color = new Vector3f(color);
        }
    }

    private static Vector3f orthogonalUp(Vector3f forward, Vector3f preferredUp) {
        Vector3f result = new Vector3f(preferredUp)
                .sub(new Vector3f(forward).mul(preferredUp.dot(forward)));
        if (result.lengthSquared() < 1.0E-6F) {
            result.set(Math.abs(forward.y) < 0.99F ? 0.0F : 1.0F,
                    Math.abs(forward.y) < 0.99F ? 1.0F : 0.0F, 0.0F);
            result.sub(new Vector3f(forward).mul(result.dot(forward)));
        }
        return result.normalize();
    }
}
