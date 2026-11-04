package com.example.celestial.world;

import com.example.celestial.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Растительность островов Селестии: трава, цветы, грибы, кристаллы и деревья трёх размеров.
 * Запускается один раз на чанк; растёт только на селестийской земле (на облаках и песке - ничего).
 */
public class CelestiaVegetationFeature extends Feature<NoneFeatureConfiguration> {
    /** Радиус вокруг центра мира, где ничего не растёт (там стоит портал обратно). */
    private static final int SPAWN_CLEAR = 14;

    public CelestiaVegetationFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static boolean isEarth(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.CELESTIAL_EARTH.get());
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockPos.MutableBlockPos ground = new BlockPos.MutableBlockPos();
        boolean placed = false;

        // --- трава, цветы, грибы, кристаллы
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = origin.getX() + lx;
                int z = origin.getZ() + lz;
                if (Math.abs(x) <= SPAWN_CLEAR && Math.abs(z) <= SPAWN_CLEAR) continue;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                ground.set(x, y - 1, z);
                if (!isEarth(level, ground)) continue;
                BlockPos above = ground.above();
                if (!level.getBlockState(above).isAir()) continue;
                BlockState plant = pickPlant(random);
                if (plant != null) {
                    level.setBlock(above, plant, 2);
                    placed = true;
                }
            }
        }

        // --- деревья: несколько попыток на чанк, размер случайный (5% большое, 20% среднее, 75% маленькое)
        for (int i = 0; i < 4; i++) {
            if (random.nextInt(5) != 0) continue;
            int x = origin.getX() + random.nextInt(16);
            int z = origin.getZ() + random.nextInt(16);
            if (Math.abs(x) <= SPAWN_CLEAR + 4 && Math.abs(z) <= SPAWN_CLEAR + 4) continue;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            BlockPos base = new BlockPos(x, y - 1, z);
            if (!isEarth(level, base)) continue;
            int roll = random.nextInt(100);
            int size = roll < 5 ? CelestialTreeGenerator.LARGE : roll < 25 ? CelestialTreeGenerator.MEDIUM
                    : CelestialTreeGenerator.SMALL;
            if (size >= CelestialTreeGenerator.MEDIUM && !solidBase(level, base, size)) size = CelestialTreeGenerator.SMALL;
            if (CelestialTreeGenerator.grow(level, base.above(), random, size)) placed = true;
        }
        return placed;
    }

    /** Для толстых стволов под деревом должна быть земля, чтобы дерево не висело над краем острова. */
    private static boolean solidBase(WorldGenLevel level, BlockPos base, int size) {
        int from = size >= CelestialTreeGenerator.LARGE ? -1 : 0;
        for (int dx = from; dx <= 1; dx++) {
            for (int dz = from; dz <= 1; dz++) {
                if (!isEarth(level, base.offset(dx, 0, dz))) return false;
            }
        }
        return true;
    }

    private static BlockState pickPlant(RandomSource random) {
        int roll = random.nextInt(1000);
        if (roll < 180) return ModBlocks.CELESTIAL_GRASS.get().defaultBlockState();
        if (roll < 210) return ModBlocks.CELESTIAL_FLOWER.get().defaultBlockState();
        if (roll < 222) return ModBlocks.CELESTIAL_MUSHROOM.get().defaultBlockState();
        if (roll < 230) return ModBlocks.CELESTIAL_CRYSTAL.get().defaultBlockState();
        return null;
    }
}
