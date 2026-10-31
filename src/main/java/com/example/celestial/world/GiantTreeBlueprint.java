package com.example.celestial.world;

import com.example.celestial.registry.ModBlocks;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Чертёж гигантского Селестийского дерева: ствол, корни, ветви, крона, комната внутри, сундуки.
 * Строится только из позиции, высоты и зерна, поэтому для каждого чанка структуры получается одно и то же дерево.
 */
public final class GiantTreeBlueprint {
    private static final int MAX_LEAF_DISTANCE = 6;
    private static final int[][] DIR4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public final Map<Long, BlockState> logs = new HashMap<>();
    public final Set<Long> air = new HashSet<>();
    public final Set<Long> floor = new HashSet<>();
    public final List<BlockPos> lights = new ArrayList<>();
    public final Map<BlockPos, Direction> chests = new LinkedHashMap<>();
    public final Map<Long, Integer> leaves = new HashMap<>();
    public BlockPos guardianPos;

    private GiantTreeBlueprint() {}

    public static GiantTreeBlueprint build(BlockPos base, int height, long seed) {
        RandomSource random = RandomSource.create(seed);
        GiantTreeBlueprint bp = new GiantTreeBlueprint();
        BlockState log = ModBlocks.CELESTIAL_LOG.get().defaultBlockState();
        Map<Long, BlockState> logs = bp.logs;

        // ствол с сужением кверху и фундамент
        for (int y = -6; y < height; y++) {
            double r = y < 0 ? 5.0 : (y < height * 0.6 ? 6.4 : 6.4 - 3.4 * (y - height * 0.6) / (height * 0.4));
            int rr = (int) Math.ceil(r);
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dz = -rr; dz <= rr; dz++) {
                    if (dx * dx + dz * dz <= r * r) logs.put(base.offset(dx, y, dz).asLong(), log);
                }
            }
        }
        for (int y = height; y < height + 14; y++) {
            logs.put(base.offset(0, y, 0).asLong(), log);
            if (y < height + 4) {
                logs.put(base.offset(1, y, 0).asLong(), log);
                logs.put(base.offset(-1, y, 0).asLong(), log);
                logs.put(base.offset(0, y, 1).asLong(), log);
                logs.put(base.offset(0, y, -1).asLong(), log);
            }
        }
        // корни
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2 * i / 12 + random.nextDouble() * 0.3;
            for (int step = 6; step <= 11; step++) {
                int rootH = Math.max(0, 3 - (step - 6) / 2) + random.nextInt(2) - 1;
                int px = (int) Math.round(Math.cos(angle) * step), pz = (int) Math.round(Math.sin(angle) * step);
                for (int y = -1; y < rootH; y++) logs.putIfAbsent(base.offset(px, y, pz).asLong(), log);
            }
        }
        // ветви: 8 крупных и 6 коронных
        List<BlockPos> anchors = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8 + random.nextDouble() * 0.4;
            addBranch(logs, log, base, angle, (int) (height * 0.55) + random.nextInt(8), 6, 10 + random.nextInt(4), 0.35, anchors);
        }
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6 + random.nextDouble() * 0.5;
            addBranch(logs, log, base, angle, height - 6 + random.nextInt(4), 3, 8 + random.nextInt(3), 0.55, anchors);
        }

        // комната внутри ствола и вход
        int[] entrance = DIR4[random.nextInt(4)];
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                bp.floor.add(base.offset(dx, -1, dz).asLong());
                for (int y = 0; y <= 7; y++) bp.air.add(base.offset(dx, y, dz).asLong());
            }
        }
        for (int t = 4; t <= 8; t++) {
            for (int lat = -1; lat <= 1; lat++) {
                int px = entrance[0] * t + (entrance[0] == 0 ? lat : 0);
                int pz = entrance[1] * t + (entrance[1] == 0 ? lat : 0);
                for (int y = 0; y <= 3; y++) bp.air.add(base.offset(px, y, pz).asLong());
                bp.floor.add(base.offset(px, -1, pz).asLong());
            }
        }
        for (long l : bp.air) logs.remove(l);
        for (int[] o : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}, {0, 3}, {0, -3}, {3, 0}, {-3, 0}, {0, 0}}) {
            bp.lights.add(base.offset(o[0], 7, o[1]));
        }

        // сундуки: 4 угла и стена напротив входа
        List<int[]> chestSpots = new ArrayList<>(List.of(new int[]{3, 3}, new int[]{-3, 3}, new int[]{3, -3}, new int[]{-3, -3}));
        chestSpots.add(new int[]{-entrance[0] * 3, -entrance[1] * 3});
        for (int[] c : chestSpots) {
            Direction facing = Math.abs(c[0]) >= Math.abs(c[1])
                    ? (c[0] > 0 ? Direction.WEST : Direction.EAST) : (c[1] > 0 ? Direction.NORTH : Direction.SOUTH);
            bp.chests.put(base.offset(c[0], 0, c[1]), facing);
        }
        bp.guardianPos = base;

        // крона
        Set<Long> candidates = new HashSet<>();
        anchors.add(base.above(height + 9));
        for (BlockPos anchor : anchors) {
            boolean top = anchor.getY() > base.getY() + height + 5;
            addEllipsoid(candidates, anchor, top ? 7 : 5, top ? 6 : 4, random);
        }
        addEllipsoid(candidates, base.above(height - 3), 9, 4, random);
        candidates.removeIf(l -> logs.containsKey(l) || bp.air.contains(l));

        // расстояние листвы до ближайшего бревна (чтобы листва сразу не засыхала)
        Map<Long, Integer> dist = new HashMap<>();
        Deque<Long> queue = new ArrayDeque<>();
        for (long l : logs.keySet()) {
            dist.put(l, 0);
            queue.add(l);
        }
        while (!queue.isEmpty()) {
            long l = queue.poll();
            int d = dist.get(l);
            if (d >= MAX_LEAF_DISTANCE) continue;
            BlockPos p = BlockPos.of(l);
            for (Direction dir : Direction.values()) {
                long n = p.relative(dir).asLong();
                if (candidates.contains(n) && !dist.containsKey(n)) {
                    dist.put(n, d + 1);
                    queue.add(n);
                }
            }
        }
        for (long l : candidates) {
            Integer d = dist.get(l);
            if (d != null && d <= MAX_LEAF_DISTANCE) bp.leaves.put(l, d);
        }
        return bp;
    }

    private static void addBranch(Map<Long, BlockState> logs, BlockState log, BlockPos base, double angle,
                                  int startY, int startR, int length, double slope, List<BlockPos> anchors) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        Direction.Axis axis = Math.abs(cos) > Math.abs(sin) * 1.5 ? Direction.Axis.X
                : Math.abs(sin) > Math.abs(cos) * 1.5 ? Direction.Axis.Z : Direction.Axis.Y;
        BlockState state = log.setValue(RotatedPillarBlock.AXIS, axis);
        BlockPos last = base.offset((int) Math.round(cos * startR), startY, (int) Math.round(sin * startR));
        for (int s = 0; s <= length; s++) {
            int px = (int) Math.round(cos * (startR + s)), pz = (int) Math.round(sin * (startR + s));
            int py = startY + (int) Math.round(s * slope);
            boolean thick = s < length * 0.6;
            for (int a = 0; a <= (thick ? 1 : 0); a++) {
                for (int b = 0; b <= (thick ? 1 : 0); b++) {
                    logs.putIfAbsent(base.offset(px + a, py + b, pz).asLong(), state);
                }
            }
            last = base.offset(px, py, pz);
            if (s % 3 == 0) anchors.add(last.above());
        }
        anchors.add(last.above(2));
    }

    private static void addEllipsoid(Set<Long> out, BlockPos center, int rxz, int ry, RandomSource random) {
        for (int dx = -rxz; dx <= rxz; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rxz; dz <= rxz; dz++) {
                    double d = (double) (dx * dx) / (rxz * rxz) + (double) (dy * dy) / (ry * ry) + (double) (dz * dz) / (rxz * rxz);
                    if (d <= 1.0 - random.nextFloat() * 0.3F) out.add(center.offset(dx, dy, dz).asLong());
                }
            }
        }
    }
}
