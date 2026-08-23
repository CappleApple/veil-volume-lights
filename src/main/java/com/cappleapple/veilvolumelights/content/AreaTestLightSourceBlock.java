package com.cappleapple.veilvolumelights.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class AreaTestLightSourceBlock extends TestLightSourceBlock {
    public static final MapCodec<AreaTestLightSourceBlock> CODEC =
            simpleCodec(AreaTestLightSourceBlock::new);

    public AreaTestLightSourceBlock(BlockBehaviour.Properties properties) {
        super(properties, TestLightShape.AREA);
    }

    @Override
    protected MapCodec<? extends AreaTestLightSourceBlock> codec() {
        return CODEC;
    }
}
