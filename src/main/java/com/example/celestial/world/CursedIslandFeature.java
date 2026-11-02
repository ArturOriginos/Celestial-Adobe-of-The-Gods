package com.example.celestial.world;

import com.example.celestial.registry.ModBlocks;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Плавающий остров из незерского камня с кристаллами, растениями, деревом и рунической башней. */
public class CursedIslandFeature extends Feature<NoneFeatureConfiguration> {
    public CursedIslandFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos o = context.origin();
        if (!level.getBlockState(o).isAir()) return false;

        int radius = 5 + random.nextInt(5);   // 5..9
        int depth = Math.max(5, (int) (radius * (0.9 + random.nextFloat() * 0.9)));
        double p1 = random.nextDouble() * Math.PI * 2;
        double p2 = random.nextDouble() * Math.PI * 2;
        double p3 = random.nextDouble() * Math.PI * 2;

        BlockState stone = ModBlocks.CURSED_STONE.get().defaultBlockState();
        BlockState basalt = ModBlocks.CURSED_BASALT.get().defaultBlockState();
        BlockState bricks = ModBlocks.CURSED_BRICKS.get().defaultBlockState();
        BlockState rune = ModBlocks.CURSED_RUNE_STONE.get().defaultBlockState();
        BlockState glow = ModBlocks.CURSED_GLOWSTONE.get().defaultBlockState();
        BlockState foliage = ModBlocks.CURSED_FOLIAGE.get().defaultBlockState();

        List<BlockPos> tops = new ArrayList<>();
        int range = 13;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                double th = Math.atan2(dz, dx);
                double wob = 1.0 + 0.22 * Math.sin(3 * th + p1) + 0.12 * Math.sin(5 * th + p2) + 0.08 * Math.sin(2 * th + p3);
                double edge = Math.min(radius * wob, 12.5);
                if (dist > edge) continue;
                double ratio = dist / edge;
                int colDepth = (int) Math.round(depth * Math.pow(1.0 - ratio, 0.75)) + 1 + (ratio < 0.5 ? random.nextInt(2) : 0);

                for (int d = 0; d < colDepth; d++) {
                    BlockPos p = o.offset(dx, -d, dz);
                    int roll = random.nextInt(100);
                    BlockState state;
                    if (d == 0) {
                        state = roll < 80 ? stone : roll < 90 ? basalt : roll < 96 ? foliage : bricks;
                    } else {
                        state = roll < 80 ? stone : roll < 93 ? basalt : (roll < 95 && d >= 2) ? rune : roll < 97 ? glow : stone;
                    }
                    level.setBlock(p, state, 2);
                }
                if (ratio < 0.75 && random.nextInt(14) == 0) {
                    int spike = 2 + random.nextInt(5);
                    for (int k = 0; k < spike; k++) {
                        level.setBlock(o.offset(dx, -colDepth - k, dz), basalt, 2);
                    }
                }
                tops.add(o.offset(dx, 0, dz));
            }
        }

        // руническая башня в центре
        boolean tower = radius >= 7 && random.nextInt(2) == 0;
        if (tower) {
            int h = 5 + random.nextInt(3);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean centerCol = dx == 0 && dz == 0;
                    boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                    int colH = centerCol ? h : corner ? h - 2 : 2 + random.nextInt(2);
                    for (int y = 1; y <= colH; y++) {
                        level.setBlock(o.offset(dx, y, dz), centerCol ? rune : corner ? basalt : bricks, 2);
                    }
                    if (corner) level.setBlock(o.offset(dx, colH + 1, dz), ModBlocks.CURSED_CRYSTAL.get().defaultBlockState(), 2);
                }
            }
            level.setBlock(o.offset(0, h + 1, 0), glow, 2);
            level.setBlock(o.offset(0, h + 2, 0), ModBlocks.CURSED_CRYSTAL.get().defaultBlockState(), 2);
        }

        // растения и кристаллы на поверхности
        for (BlockPos top : tops) {
            if (tower && Math.abs(top.getX() - o.getX()) <= 1 && Math.abs(top.getZ() - o.getZ()) <= 1) continue;
            BlockState plant = pickPlant(random);
            if (plant != null) level.setBlock(top.above(), plant, 2);
        }

        // одно маленькое дерево на острове
        if (radius >= 6 && random.nextInt(3) == 0) {
            int tx = (random.nextBoolean() ? 1 : -1) * (2 + random.nextInt(radius / 2));
            int tz = (random.nextBoolean() ? 1 : -1) * (2 + random.nextInt(radius / 2));
            BlockPos ground = o.offset(tx, 0, tz);
            if (level.getBlockState(ground).is(ModBlocks.CURSED_STONE.get())) {
                CursedTreeGenerator.grow(level, ground.above(), random, CursedTreeGenerator.SMALL);
            }
        }
        return true;
    }

    private static BlockState pickPlant(RandomSource random) {
        int roll = random.nextInt(1000);
        if (roll < 220) return ModBlocks.CURSED_GRASS.get().defaultBlockState();
        if (roll < 250) return ModBlocks.CURSED_FLOWER.get().defaultBlockState();
        if (roll < 280) return ModBlocks.CURSED_MUSHROOM.get().defaultBlockState();
        if (roll < 315) return ModBlocks.HELL_BUSH.get().defaultBlockState();
        if (roll < 330) return ModBlocks.BLAZING_FLOWER.get().defaultBlockState();
        if (roll < 365) return ModBlocks.CURSED_CRYSTAL.get().defaultBlockState();
        if (roll < 400) return ModBlocks.CRYSTAL_SPROUTS.get().defaultBlockState();
        return null;
    }
}
