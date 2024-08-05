package com.example.celestial.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Селестийское стекло: как обычное стекло (ломается рукой, без шёлкового касания ничего не выпадает). */
public class CelestialGlassBlock extends Block {
    public CelestialGlassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Соседние блоки селестийского стекла не рисуют внутренние грани. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, direction);
    }
}
