package com.cappleapple.veilvolumelights.client;

import com.cappleapple.veilvolumelights.VeilVolumeLightsMod;
import com.cappleapple.veilvolumelights.client.render.OpaqueDepthCapture;
import com.cappleapple.veilvolumelights.client.render.VolumeLightRenderManager;
import com.cappleapple.veilvolumelights.content.TestLightSourceTracker;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = VeilVolumeLightsMod.MOD_ID)
public final class ClientEvents {
    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            TestLightManager.sync(java.util.List.of());
        } else {
            TestLightManager.sync(TestLightSourceTracker.active(minecraft.level));
        }
    }

    @SubscribeEvent
    public static void frameStart(RenderFrameEvent.Pre event) {
        OpaqueDepthCapture.beginFrame();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void frameEnd(RenderFrameEvent.Post event) {
        // Integration mods normally submit moving lights from this same event.
        // Flush after their default-priority listeners so the native Veil
        // light and its medium-owner position always describe one frame.
        VolumeLightRenderManager.uploadTransparentMedia();
    }

    @SubscribeEvent
    public static void renderLevelStage(RenderLevelStageEvent event) {
        OpaqueDepthCapture.capture(event);
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            VolumeLightRenderManager.applyTransmittedSurface();
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            clear();
        }
    }

    private static void clear() {
        TestLightManager.clear();
        TestLightSourceTracker.clear();
        VolumeLightRenderManager.clear();
        OpaqueDepthCapture.clear();
    }

    private ClientEvents() {
    }
}
