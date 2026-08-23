package com.cappleapple.veilvolumelights.api.client;

/** Public entry point for creating transparent-medium-aware Veil lights. */
public final class VeilVolumeLights {
    public static VolumeLightHandle create(VolumeLight light) {
        return new VolumeLightHandle(light);
    }

    private VeilVolumeLights() {
    }
}
