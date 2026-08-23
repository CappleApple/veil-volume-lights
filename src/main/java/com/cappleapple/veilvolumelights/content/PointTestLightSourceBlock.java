package com.cappleapple.veilvolumelights.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PointTestLightSourceBlock extends TestLightSourceBlock {
    private static final VoxelShape SHAPE = box(6.0, 0.0, 6.0, 10.0, 10.0, 10.0);
    public static final MapCodec<PointTestLightSourceBlock> CODEC =
            simpleCodec(PointTestLightSourceBlock::new);

    public PointTestLightSourceBlock(BlockBehaviour.Properties properties) {
        super(properties, TestLightShape.POINT);
    }

    @Override
    protected MapCodec<? extends PointTestLightSourceBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
