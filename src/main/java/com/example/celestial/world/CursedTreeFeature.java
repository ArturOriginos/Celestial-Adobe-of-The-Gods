package com.example.celestial.world;

import com.example.celestial.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** Проклятое дерево в мире: растёт ТОЛЬКО на незерском камне. */
public class CursedTreeFeature extends Feature<CursedTreeConfig> {
    public CursedTreeFeature(Codec<CursedTreeConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<CursedTreeConfig> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (!level.getBlockState(origin.below()).is(ModBlocks.CURSED_STONE.get())) return false;
        return CursedTreeGenerator.grow(level, origin, context.random(), context.config().size());
    }
}
