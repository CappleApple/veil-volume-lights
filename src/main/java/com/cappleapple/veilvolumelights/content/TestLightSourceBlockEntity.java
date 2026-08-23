package com.cappleapple.veilvolumelights.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TestLightSourceBlockEntity extends BlockEntity {
    public TestLightSourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.TEST_LIGHT_SOURCE_BLOCK_ENTITY.get(), pos, state);
    }
}
