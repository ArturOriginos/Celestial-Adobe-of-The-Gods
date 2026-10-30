package com.example.celestial.world;

import com.example.celestial.block.CelestialSaplingBlock;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Селестийские деревья трёх размеров: большое (ствол 3x3), среднее (ствол 2x2) и маленькое (ствол 1x1).
 * Вызывается из саженца при успешном применении костной муки (в мире само не появляется).
 */
public final class CelestialTreeGenerator {
    /** Параметры формы дерева. */
    private record Shape(int trunkMin, int trunkMax, int minHeight, int heightRange,
                         int canopyR, int canopyRy, int lowerR, int lowerRy, int lowerDrop,
                         int branches, int branchMin, int branchRange, int branchLeafR, int branchLeafRy,
                         int spire, boolean roots) {}

    private static final Shape LARGE = new Shape(-1, 1, 20, 6, 9, 6, 7, 4, 4, 8, 6, 3, 4, 3, 4, true);
    private static final Shape MEDIUM = new Shape(0, 1, 11, 5, 6, 4, 0, 0, 0, 4, 3, 3, 3, 2, 2, false);
    private static final Shape SMALL = new Shape(0, 0, 6, 3, 4, 3, 0, 0, 0, 0, 0, 1, 0, 0, 2, false);

    // сначала стороны света, потом диагонали
    private static final int[][] DIRS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
    private static final int MAX_LEAF_DISTANCE = 6; // дальше листва бы сразу засохла

    private CelestialTreeGenerator() {}

    public static boolean grow(ServerLevel level, BlockPos origin, RandomSource random) {
        int roll = random.nextInt(100);
        Shape[] order = roll < 25 ? new Shape[]{LARGE, MEDIUM, SMALL}
                : roll < 60 ? new Shape[]{MEDIUM, SMALL} : new Shape[]{SMALL};
        for (Shape shape : order) {
            if (tryGrow(level, origin, random, shape)) return true; // не поместилось - пробуем поменьше
        }
        return false;
    }

    private static boolean tryGrow(ServerLevel level, BlockPos origin, RandomSource random, Shape s) {
        int height = s.minHeight() + random.nextInt(s.heightRange());

        for (int y = 0; y < height; y++) {
            for (int dx = s.trunkMin(); dx <= s.trunkMax(); dx++) {
                for (int dz = s.trunkMin(); dz <= s.trunkMax(); dz++) {
                    if (!canReplace(level, origin.offset(dx, y, dz))) return false;
                }
            }
        }

        BlockState logY = ModBlocks.CELESTIAL_LOG.get().defaultBlockState();
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

        // расстояние листвы до ближайшего бревна (как в ванильной механике засыхания)
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
        BlockState leaves = ModBlocks.CELESTIAL_LEAVES.get().defaultBlockState();
        for (BlockPos p : candidates) {
            Integer d = dist.get(p);
            if (d == null || d > MAX_LEAF_DISTANCE) continue;
            level.setBlock(p, leaves.setValue(LeavesBlock.DISTANCE, d), 2);
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

    private static boolean canReplace(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS) || state.getBlock() instanceof CelestialSaplingBlock;
    }
}
