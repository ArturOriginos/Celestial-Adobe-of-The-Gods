package com.example.celestial.world;

import com.example.celestial.block.CursedSaplingBlock;
import com.example.celestial.registry.ModBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Проклятые деревья трёх размеров: большое (ствол 3x3), среднее (ствол 2x2) и маленькое (ствол 1x1).
 * Растут на незерском камне: из ростка (костная мука) и при генерации биома «Проклятый Нифельхельм».
 */
public final class CursedTreeGenerator {
    public static final int SMALL = 0, MEDIUM = 1, LARGE = 2;

    private record Shape(int trunkMin, int trunkMax, int minHeight, int heightRange,
                         int canopyR, int canopyRy, int lowerR, int lowerRy, int lowerDrop,
                         int branches, int branchMin, int branchRange, int branchLeafR, int branchLeafRy,
                         int spire, boolean roots) {}

    private static final Shape LARGE_SHAPE = new Shape(-1, 1, 20, 6, 9, 6, 7, 4, 4, 8, 6, 3, 4, 3, 4, true);
    private static final Shape MEDIUM_SHAPE = new Shape(0, 1, 11, 5, 6, 4, 0, 0, 0, 4, 3, 3, 3, 2, 2, false);
    private static final Shape SMALL_SHAPE = new Shape(0, 0, 6, 3, 4, 3, 0, 0, 0, 0, 0, 1, 0, 0, 2, false);

    private static final int[][] DIRS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
    private static final int MAX_LEAF_DISTANCE = 6;

    private CursedTreeGenerator() {}

    /** Из ростка: случайный размер (25% большое, 35% среднее, 40% маленькое). */
    public static boolean growRandom(LevelAccessor level, BlockPos origin, RandomSource random) {
        int roll = random.nextInt(100);
        return grow(level, origin, random, roll < 25 ? LARGE : roll < 60 ? MEDIUM : SMALL);
    }

    /** Пытается вырастить дерево нужного размера, не поместилось - пробует поменьше. */
    public static boolean grow(LevelAccessor level, BlockPos origin, RandomSource random, int size) {
        if (size >= LARGE && tryGrow(level, origin, random, LARGE_SHAPE)) return true;
        if (size >= MEDIUM && tryGrow(level, origin, random, MEDIUM_SHAPE)) return true;
        return tryGrow(level, origin, random, SMALL_SHAPE);
    }

    private static boolean tryGrow(LevelAccessor level, BlockPos origin, RandomSource random, Shape s) {
        int height = s.minHeight() + random.nextInt(s.heightRange());

        for (int y = 0; y < height; y++) {
            for (int dx = s.trunkMin(); dx <= s.trunkMax(); dx++) {
                for (int dz = s.trunkMin(); dz <= s.trunkMax(); dz++) {
                    if (!canReplace(level, origin.offset(dx, y, dz))) return false;
                }
            }
        }

        BlockState logY = ModBlocks.CURSED_LOG.get().defaultBlockState();
        Map<BlockPos, BlockState> logs = new HashMap<>();

        for (int y = 0; y < height; y++) {
            for (int dx = s.trunkMin(); dx <= s.trunkMax(); dx++) {
                for (int dz = s.trunkMin(); dz <= s.trunkMax(); dz++) {
                    logs.put(origin.offset(dx, y, dz), logY);
                }
            }
        }
        for (int y = height; y < height + s.spire(); y++) logs.put(origin.above(y), logY);

        if (s.roots()) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    int ax = Math.abs(dx), az = Math.abs(dz);
                    int ring = Math.max(ax, az);
                    if (ring < 2 || ax + az > 4 || random.nextInt(4) == 0) continue;
                    int rootHeight = (ring == 2 ? 2 : 1) + random.nextInt(2);
                    for (int y = 0; y < rootHeight; y++) {
                        BlockPos p = origin.offset(dx, y, dz);
                        if (canReplace(level, p)) logs.put(p, logY);
                    }
                }
            }
        }

        List<BlockPos> branchEnds = new ArrayList<>();
        for (int i = 0; i < s.branches(); i++) {
            int dx = DIRS[i][0], dz = DIRS[i][1];
            int baseY = (int) (height * 0.5) + random.nextInt(3) + (i % 2) * 2;
            int length = s.branchMin() + random.nextInt(s.branchRange());
            Direction.Axis axis = (dx != 0 && dz == 0) ? Direction.Axis.X
                    : (dz != 0 && dx == 0) ? Direction.Axis.Z : Direction.Axis.Y;
            BlockState branch = logY.setValue(RotatedPillarBlock.AXIS, axis);
            BlockPos last = origin.offset(dx, baseY, dz);
            for (int step = 2; step <= length + 1; step++) {
                BlockPos p = origin.offset(dx * step, baseY + (step - 2) / 2, dz * step);
                if (!canReplace(level, p)) break;
                logs.put(p, branch);
                last = p;
            }
            branchEnds.add(last);
        }

        Set<BlockPos> candidates = new HashSet<>();
        addEllipsoid(candidates, origin.above(height + 1), s.canopyR(), s.canopyRy(), random);
        if (s.lowerR() > 0) addEllipsoid(candidates, origin.above(height - s.lowerDrop()), s.lowerR(), s.lowerRy(), random);
        if (s.branchLeafR() > 0) {
            for (BlockPos end : branchEnds) addEllipsoid(candidates, end.above(), s.branchLeafR(), s.branchLeafRy(), random);
        }
        candidates.removeIf(p -> logs.containsKey(p) || !canReplace(level, p));

        Map<BlockPos, Integer> dist = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos lp : logs.keySet()) {
            dist.put(lp, 0);
            queue.add(lp);
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int d = dist.get(p);
            if (d >= MAX_LEAF_DISTANCE) continue;
            for (Direction dir : Direction.values()) {
                BlockPos n = p.relative(dir);
                if (candidates.contains(n) && !dist.containsKey(n)) {
                    dist.put(n, d + 1);
                    queue.add(n);
                }
            }
        }

        for (Map.Entry<BlockPos, BlockState> e : logs.entrySet()) {
            level.setBlock(e.getKey(), e.getValue(), 2);
        }
        BlockState leaves = ModBlocks.CURSED_LEAVES.get().defaultBlockState();
        List<BlockPos> placedLeaves = new ArrayList<>();
        for (BlockPos p : candidates) {
            Integer d = dist.get(p);
            if (d == null || d > MAX_LEAF_DISTANCE) continue;
            level.setBlock(p, leaves.setValue(LeavesBlock.DISTANCE, d), 2);
            placedLeaves.add(p);
        }

        // свисающие листья под кроной
        BlockState hanging = ModBlocks.CURSED_HANGING_LEAVES.get().defaultBlockState();
        for (BlockPos p : placedLeaves) {
            if (random.nextInt(6) != 0 || !level.getBlockState(p.below()).isAir()) continue;
            int length = 1 + random.nextInt(4);
            BlockPos last = null;
            for (int i = 1; i <= length; i++) {
                BlockPos hp = p.below(i);
                if (!level.getBlockState(hp).isAir()) break;
                level.setBlock(hp, hanging, 2);
                last = hp;
            }
            if (last != null) { // нижний кончик лозы рисуется отдельной моделью
                level.setBlock(last, hanging.setValue(com.example.celestial.block.CursedHangingLeavesBlock.END, true), 2);
            }
        }
        return true;
    }

    private static void addEllipsoid(Set<BlockPos> out, BlockPos center, int radiusXZ, int radiusY, RandomSource random) {
        for (int dx = -radiusXZ; dx <= radiusXZ; dx++) {
            for (int dy = -radiusY; dy <= radiusY; dy++) {
                for (int dz = -radiusXZ; dz <= radiusXZ; dz++) {
                    double d = (double) (dx * dx) / (radiusXZ * radiusXZ)
                            + (double) (dy * dy) / (radiusY * radiusY)
                            + (double) (dz * dz) / (radiusXZ * radiusXZ);
                    if (d <= 1.0 - random.nextFloat() * 0.3F) out.add(center.offset(dx, dy, dz));
                }
            }
        }
    }

    private static boolean canReplace(LevelAccessor level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS) || state.getBlock() instanceof CursedSaplingBlock;
    }
}
