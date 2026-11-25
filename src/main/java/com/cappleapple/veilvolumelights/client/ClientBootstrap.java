package com.cappleapple.veilvolumelights.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientBootstrap {
    public static void initialize(IEventBus modBus) {
        modBus.addListener(ClientBootstrap::setup);
    }

    private static void setup(FMLClientSetupEvent event) {
        // OptiFine owns its render targets and TaCZ initializes those separately.
        try {
            Class.forName("net.optifine.Config", false, ClientBootstrap.class.getClassLoader());
            return;
        } catch (ClassNotFoundException ignored) {
            // Our light targets use NeoForge's packed DEPTH32F_STENCIL8 format.
            // Allocate matching main depth before Veil caches it for a blit.
            event.enqueueWork(() -> Minecraft.getInstance().getMainRenderTarget().enableStencil());
        }
    }

    private ClientBootstrap() {
    }
}
