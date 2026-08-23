package com.cappleapple.veilvolumelights.client;

import com.cappleapple.veilvolumelights.api.client.VeilVolumeLights;
import com.cappleapple.veilvolumelights.api.client.VolumeLight;
import com.cappleapple.veilvolumelights.api.client.VolumeLightHandle;
import com.cappleapple.veilvolumelights.content.TestLightSourceTracker;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class TestLightManager {
    private static final Map<Long, VolumeLightHandle> LIGHTS = new HashMap<>();
    private static final Vector3f WHITE = new Vector3f(1.0F, 0.98F, 0.92F);

    static void sync(Collection<TestLightSourceTracker.Source> sources) {
        for (TestLightSourceTracker.Source source : sources) {
            long key = source.pos().asLong();
            VolumeLight definition = definition(source);
            VolumeLightHandle handle = LIGHTS.get(key);
            if (handle == null) {
                LIGHTS.put(key, VeilVolumeLights.create(definition));
            } else {
                handle.update(definition);
            }
        }
        Set<Long> active = sources.stream().map(source -> source.pos().asLong()).collect(Collectors.toSet());
        LIGHTS.entrySet().removeIf(entry -> {
            if (active.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().free();
            return true;
        });
    }

    static void clear() {
        LIGHTS.values().forEach(VolumeLightHandle::free);
        LIGHTS.clear();
    }

    private static VolumeLight definition(TestLightSourceTracker.Source source) {
        Vector3f forward = new Vector3f(source.facing().getStepX(),
                source.facing().getStepY(), source.facing().getStepZ());
        Vector3f center = new Vector3f(source.pos().getX() + 0.5F,
                source.pos().getY() + 0.5F, source.pos().getZ() + 0.5F);
        return switch (source.shape()) {
            case POINT -> new VolumeLight.Point(
                    new Vector3f(center).add(0.0F, 0.32F, 0.0F),
                    WHITE, 1.25F, 14.0F, true, 0.75F);
            case SPOT -> new VolumeLight.Spot(
                    center.fma(0.52F, forward), forward, new Vector3f(0.0F, 1.0F, 0.0F),
                    WHITE, 1.15F, 40.0F, 20.0F, 9.0F, true, 0.85F);
            case AREA -> new VolumeLight.Area(
                    center.fma(0.52F, forward), forward, new Vector3f(0.0F, 1.0F, 0.0F),
                    WHITE, 1.10F, 28.0F, 4.0F, 2.0F, 72.0F, true, 0.80F);
        };
    }

    private TestLightManager() {
    }
}
