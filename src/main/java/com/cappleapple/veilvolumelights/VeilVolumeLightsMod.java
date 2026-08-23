package com.cappleapple.veilvolumelights;

import com.cappleapple.veilvolumelights.content.ModContent;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(VeilVolumeLightsMod.MOD_ID)
public final class VeilVolumeLightsMod {
    public static final String MOD_ID = "veilvolumelights";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VeilVolumeLightsMod(IEventBus modBus) {
        ModContent.register(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.cappleapple.veilvolumelights.client.ClientBootstrap.initialize();
        }
    }
}
