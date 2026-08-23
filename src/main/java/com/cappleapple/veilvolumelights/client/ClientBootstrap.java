package com.cappleapple.veilvolumelights.client;

public final class ClientBootstrap {
    public static void initialize() {
        // Loading this class keeps client-only bootstrap isolated from dedicated servers.
    }

    private ClientBootstrap() {
    }
}
