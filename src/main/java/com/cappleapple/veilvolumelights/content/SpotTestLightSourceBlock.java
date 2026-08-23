package com.cappleapple.veilvolumelights.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class SpotTestLightSourceBlock extends TestLightSourceBlock {
    public static final MapCodec<SpotTestLightSourceBlock> CODEC =
            simpleCodec(SpotTestLightSourceBlock::new);

    public SpotTestLightSourceBlock(BlockBehaviour.Properties properties) {
        super(properties, TestLightShape.SPOT);
    }

    @Override
    protected MapCodec<? extends SpotTestLightSourceBlock> codec() {
        return CODEC;
    }
}
