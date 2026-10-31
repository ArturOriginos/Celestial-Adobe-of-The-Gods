package com.example.celestial.world;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.GuardianOfGods;
import com.example.celestial.registry.ModBlocks;
import com.example.celestial.registry.ModEntities;
import com.mojang.serialization.Codec;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Огромное Селестийское дерево (вырастить нельзя, только найти в мире): толстый ствол, 10 ветвей,
 * гигантская крона и комната внутри с сундуками и Хранителем Богов.
 * Строится вокруг центра чанка, радиус не больше ~24 блоков (допустимая зона записи при генерации).
 */
public class GiantCelestialTreeFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceKey<LootTable> CHEST_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(MODID, "chests/giant_tree"));
    private static final int MAX_LEAF_DISTANCE = 6;
    private static final int[][] DIR4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public GiantCelestialTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        int cx = ctx.origin().getX() + 8;
        int cz = ctx.origin().getZ() + 8;

        int gy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx, cz);
        if (gy != level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cx, cz)) return false; // вода
        for (int[] o : new int[][]{{12, 0}, {-12, 0}, {0, 12}, {0, -12}}) {
            if (Math.abs(level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx + o[0], cz + o[1]) - gy) > 6) return false;
        }
        int height = 44 + random.nextInt(8);
        if (gy + height + 20 >= level.getMaxBuildHeight() || gy < level.getMinBuildHeight() + 12) return false;
        BlockPos base = new BlockPos(cx, gy, cz);

        BlockState log = ModBlocks.CELESTIAL_LOG.get().defaultBlockState();
        Map<Long, BlockState> logs = new HashMap<>();

        // ---- ствол (с сужением кверху) и фундамент
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

        // ---- корни
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2 * i / 12 + random.nextDouble() * 0.3;
            for (int step = 6; step <= 11; step++) {
                int rootH = Math.max(0, 3 - (step - 6) / 2) + random.nextInt(2) - 1;
                int px = (int) Math.round(Math.cos(angle) * step), pz = (int) Math.round(Math.sin(angle) * step);
                for (int y = -1; y < rootH; y++) {
                    BlockPos p = base.offset(px, y, pz);
                    if (canReplace(level, p) || y < 0) logs.putIfAbsent(p.asLong(), log);
                }
            }
        }

        // ---- ветви: 8 крупных + 6 коронных
        List<BlockPos> leafAnchors = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8 + random.nextDouble() * 0.4;
            addBranch(level, logs, log, base, angle, (int) (height * 0.55) + random.nextInt(8), 6,
                    10 + random.nextInt(4), 0.35, leafAnchors);
        }
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6 + random.nextDouble() * 0.5;
            addBranch(level, logs, log, base, angle, height - 6 + random.nextInt(4), 3, 8 + random.nextInt(3), 0.55, leafAnchors);
        }

        // ---- комната внутри ствола и вход
        int[] entrance = DIR4[random.nextInt(4)];
        Set<Long> air = new HashSet<>();
        Set<Long> floor = new HashSet<>();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                floor.add(base.offset(dx, -1, dz).asLong());
                for (int y = 0; y <= 7; y++) air.add(base.offset(dx, y, dz).asLong());
            }
        }
        for (int t = 4; t <= 8; t++) {
            for (int lat = -1; lat <= 1; lat++) {
                int px = entrance[0] * t + (entrance[0] == 0 ? lat : 0);
                int pz = entrance[1] * t + (entrance[1] == 0 ? lat : 0);
                for (int y = 0; y <= 3; y++) air.add(base.offset(px, y, pz).asLong());
                floor.add(base.offset(px, -1, pz).asLong());
            }
        }
        for (long l : air) logs.remove(l);

        // ---- крона: шары листвы вокруг ветвей и вершины
        Set<Long> candidates = new HashSet<>();
        leafAnchors.add(base.above(height + 9));
        for (BlockPos anchor : leafAnchors) {
            boolean top = anchor.getY() > gy + height + 5;
            addEllipsoid(candidates, anchor, top ? 7 : 5, top ? 6 : 4, random);
        }
        addEllipsoid(candidates, base.above(height - 3), 9, 4, random);
        candidates.removeIf(l -> logs.containsKey(l) || air.contains(l) || !canReplace(level, BlockPos.of(l)));

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

        // ---- размещение
        for (Map.Entry<Long, BlockState> e : logs.entrySet()) set(level, BlockPos.of(e.getKey()), e.getValue());
        for (long l : air) set(level, BlockPos.of(l), Blocks.AIR.defaultBlockState());
        BlockState planks = ModBlocks.CELESTIAL_PLANKS.get().defaultBlockState();
        for (long l : floor) set(level, BlockPos.of(l), planks);
        BlockState light = ModBlocks.CELESTIAL_GLOWSTONE.get().defaultBlockState();
        for (int[] o : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}, {0, 3}, {0, -3}, {3, 0}, {-3, 0}, {0, 0}}) {
            set(level, base.offset(o[0], 7, o[1]), light);
        }
        BlockState leaves = ModBlocks.CELESTIAL_LEAVES.get().defaultBlockState();
        for (long l : candidates) {
            Integer d = dist.get(l);
            if (d != null && d <= MAX_LEAF_DISTANCE) set(level, BlockPos.of(l), leaves.setValue(LeavesBlock.DISTANCE, d));
        }

        // ---- сундуки: 4 угла и стена напротив входа
        List<int[]> chests = new ArrayList<>(List.of(new int[]{3, 3}, new int[]{-3, 3}, new int[]{3, -3}, new int[]{-3, -3}));
        chests.add(new int[]{-entrance[0] * 3, -entrance[1] * 3});
        for (int[] c : chests) {
            BlockPos pos = base.offset(c[0], 0, c[1]);
            Direction facing = Math.abs(c[0]) >= Math.abs(c[1])
                    ? (c[0] > 0 ? Direction.WEST : Direction.EAST) : (c[1] > 0 ? Direction.NORTH : Direction.SOUTH);
            set(level, pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
            RandomizableContainer.setBlockEntityLootTable(level, random, pos, CHEST_LOOT);
        }

        // ---- Хранитель Богов в центре комнаты (спит, пока рядом нет игрока)
        GuardianOfGods guardian = ModEntities.GUARDIAN.get().create(level.getLevel());
        if (guardian != null) {
            guardian.moveTo(cx + 0.5, gy, cz + 0.5, random.nextFloat() * 360.0F, 0.0F);
            guardian.finalizeSpawn(level, level.getCurrentDifficultyAt(base), MobSpawnType.STRUCTURE, null);
            level.addFreshEntity(guardian);
        }
        return true;
    }

    private static void addBranch(WorldGenLevel level, Map<Long, BlockState> logs, BlockState log, BlockPos base,
                                  double angle, int startY, int startR, int length, double slope, List<BlockPos> anchors) {
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
                    BlockPos p = base.offset(px + a, py + b, pz);
                    if (canReplace(level, p)) logs.putIfAbsent(p.asLong(), state);
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

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (level.ensureCanWrite(pos)) level.setBlock(pos, state, 2);
    }

    private static boolean canReplace(WorldGenLevel level, BlockPos pos) {
        if (!level.ensureCanWrite(pos)) return false;
        BlockState s = level.getBlockState(pos);
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES);
    }
}
