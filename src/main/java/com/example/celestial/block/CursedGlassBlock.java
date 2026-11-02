package com.example.celestial.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Проклятое стекло: как обычное стекло (ломается рукой, без шёлкового касания ничего не выпадает). */
public class CursedGlassBlock extends Block {
    public CursedGlassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Соседние блоки проклятого стекла не рисуют внутренние грани. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, direction);
    }
}
