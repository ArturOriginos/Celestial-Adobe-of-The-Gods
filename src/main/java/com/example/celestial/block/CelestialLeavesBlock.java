package com.example.celestial.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Селестийская листва: при разрушении игроком с шансом 20% выпадает 100 опыта. (2% саженца - в лут-таблице.) */
public class CelestialLeavesBlock extends LeavesBlock {
    public static final MapCodec<CelestialLeavesBlock> CODEC = simpleCodec(CelestialLeavesBlock::new);
    public static final float XP_CHANCE = 0.20F;
    public static final int XP_AMOUNT = 100;

    public CelestialLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack stack, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, stack, dropExperience);
        if (dropExperience && level.random.nextFloat() < XP_CHANCE) {
            this.popExperience(level, pos, XP_AMOUNT);
        }
    }
}
