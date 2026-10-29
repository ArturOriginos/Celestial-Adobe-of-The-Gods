package com.example.celestial.block;

import com.example.celestial.world.CelestialTreeGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Селестийский росток: растёт ТОЛЬКО от костной муки, шанс успеха 10%. */
public class CelestialSaplingBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<CelestialSaplingBlock> CODEC = simpleCodec(CelestialSaplingBlock::new);
    public static final float BONEMEAL_CHANCE = 0.10F;
    private static final VoxelShape SHAPE = net.minecraft.world.level.block.Block.box(2.0, 0.0, 2.0, 14.0, 12.0, 14.0);

    public CelestialSaplingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < BONEMEAL_CHANCE;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        CelestialTreeGenerator.grow(level, pos, random);
    }
}
