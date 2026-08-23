package com.cappleapple.veilvolumelights.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

public abstract class TestLightSourceBlock extends HorizontalDirectionalBlock implements EntityBlock {
    private final TestLightShape shape;

    protected TestLightSourceBlock(BlockBehaviour.Properties properties, TestLightShape shape) {
        super(properties);
        this.shape = shape;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public TestLightShape shape() {
        return shape;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestLightSourceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != ModContent.TEST_LIGHT_SOURCE_BLOCK_ENTITY.get()) {
            return null;
        }
        return (tickLevel, tickPos, tickState, ignored) -> {
            if (tickLevel.isClientSide()) {
                TestLightSourceTracker.see(tickLevel, tickPos,
                        tickState.getValue(FACING), this.shape);
            }
        };
    }
}
