package com.cappleapple.veilvolumelights.api.client;

import com.cappleapple.veilvolumelights.client.render.ManagedVolumeLight;

/** A persistent native Veil light plus its transparent-medium volume state. */
public final class VolumeLightHandle implements AutoCloseable {
    private ManagedVolumeLight delegate;

    public VolumeLightHandle(VolumeLight initialLight) {
        this.delegate = ManagedVolumeLight.create(initialLight);
    }

    public void update(VolumeLight light) {
        if (delegate == null) {
            delegate = ManagedVolumeLight.create(light);
        } else {
            delegate.update(light);
        }
    }

    public boolean isValid() {
        return delegate != null && delegate.isValid();
    }

    public VolumeLight light() {
        return delegate == null ? null : delegate.definition();
    }

    public void free() {
        if (delegate != null) {
            delegate.free();
            delegate = null;
        }
    }

    @Override
    public void close() {
        free();
    }
}
