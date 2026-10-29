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
 * Большое Селестийское дерево: толстый ствол 3x3 с корнями, 8 ветвей и огромная крона.
 * Вызывается из саженца при успешном применении костной муки (в мире само не появляется).
 */
public final class CelestialTreeGenerator {
    private static final int[][] DIRS = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
    private static final int MAX_LEAF_DISTANCE = 6; // дальше листва бы сразу засохла

    private CelestialTreeGenerator() {}

    public static boolean grow(ServerLevel level, BlockPos origin, RandomSource random) {
        int height = 20 + random.nextInt(6);

        // Проверяем, что ствол 3x3 помещается
        for (int y = 0; y < height; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (!canReplace(level, origin.offset(dx, y, dz))) return false;
                }
            }
        }

        BlockState logY = ModBlocks.CELESTIAL_LOG.get().defaultBlockState();
        Map<BlockPos, BlockState> logs = new HashMap<>();

        // ствол 3x3 и шпиль 1x1 над ним
        for (int y = 0; y < height; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    logs.put(origin.offset(dx, y, dz), logY);
                }
            }
        }
        for (int y = height; y < height + 4; y++) logs.put(origin.above(y), logY);

        // корни у основания
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

        // 8 ветвей, на концах - кластеры листвы
        List<BlockPos> branchEnds = new ArrayList<>();
        for (int i = 0; i < DIRS.length; i++) {
            int dx = DIRS[i][0], dz = DIRS[i][1];
            int baseY = (int) (height * 0.5) + random.nextInt(4) + (i % 2) * 3;
            int length = 6 + random.nextInt(3);
            Direction.Axis axis = (dx != 0 && dz == 0) ? Direction.Axis.X
                    : (dz != 0 && dx == 0) ? Direction.Axis.Z : Direction.Axis.Y;
            BlockState branch = logY.setValue(RotatedPillarBlock.AXIS, axis);
            BlockPos last = origin.offset(dx, baseY, dz);
            for (int s = 2; s <= length + 1; s++) {
                BlockPos p = origin.offset(dx * s, baseY + (s - 2) / 2, dz * s);
                if (!canReplace(level, p)) break;
                logs.put(p, branch);
                last = p;
            }
            branchEnds.add(last);
        }

        // форма кроны
        Set<BlockPos> candidates = new HashSet<>();
        addEllipsoid(candidates, origin.above(height + 1), 9, 6, random);
        addEllipsoid(candidates, origin.above(height - 4), 7, 4, random);
        for (BlockPos end : branchEnds) addEllipsoid(candidates, end.above(), 4, 3, random);
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

        // размещаем (флаг 2: без каскада обновлений соседей)
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
