package com.cappleapple.veilvolumelights.client.render;

/** Prevents the world medium buffer from being composited over first-person geometry twice. */
public final class TransparentMediumRenderState {
    private static boolean firstPersonComposite;

    public static boolean isFirstPersonComposite() {
        return firstPersonComposite;
    }

    public static void runFirstPersonComposite(Runnable action) {
        boolean previous = firstPersonComposite;
        firstPersonComposite = true;
        try {
            action.run();
        } finally {
            firstPersonComposite = previous;
        }
    }

    private TransparentMediumRenderState() {
    }
}
