package com.example.celestial.world;

import com.example.celestial.registry.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Рельеф Селестии: облачное море внизу и острова трёх размеров (маленькие, средние, большие) над ним.
 * Чисто математическая функция от (seed, x, y, z): не зависит от соседних чанков, поэтому острова
 * получаются целыми на любых границах чанков.
 */
public final class CelestiaTerrain {
    public static final int MIN_Y = 0;
    public static final int HEIGHT = 320;

    /** Слой островов: сетка клеток, в каждой с шансом chance лежит один остров целиком внутри клетки. */
    private record Layer(int cell, float chance, int rMin, int rMax, int yMin, int yMax,
                         int topAmp, float depthK, long salt) {}

    private record Island(int cx, int cz, int r, int cy, int depth, int topAmp, int flat, long salt) {}

    private static final Layer LARGE = new Layer(224, 0.55F, 44, 64, 112, 150, 8, 0.85F, 0x1A2B3C4DL);
    private static final Layer MEDIUM = new Layer(112, 0.60F, 20, 34, 100, 180, 5, 0.90F, 0x5E6F7A8BL);
    private static final Layer SMALL = new Layer(56, 0.55F, 7, 15, 90, 190, 3, 1.00F, 0x9CADBEC0L);
    private static final Layer[] LAYERS = {LARGE, MEDIUM, SMALL};

    /** Стартовый остров в центре мира: верх строго плоский на высоте SPAWN_TOP_Y в радиусе 12 блоков. */
    private static final Island SPAWN = new Island(0, 0, 56, CelestiaDimension.SPAWN_TOP_Y, 48, 6, 12, 0xC0FFEEL);

    private static final long CLOUD_SALT = 0x77AA55L;

    @FunctionalInterface
    public interface BlockSink {
        void set(int y, BlockState state);
    }

    private final long seed;
    private final BlockState earth = ModBlocks.CELESTIAL_EARTH.get().defaultBlockState();
    private final BlockState stone = ModBlocks.CELESTIAL_STONE.get().defaultBlockState();
    private final BlockState sand = ModBlocks.CELESTIAL_SAND.get().defaultBlockState();
    private final BlockState ice = ModBlocks.CELESTIAL_ICE.get().defaultBlockState();
    private final BlockState ore = ModBlocks.CELESTIAL_ORE.get().defaultBlockState();
    private final BlockState glow = ModBlocks.CELESTIAL_GLOWSTONE.get().defaultBlockState();
    private final BlockState cloud = ModBlocks.CELESTIAL_CLOUD.get().defaultBlockState();
    private final BlockState fluffy = ModBlocks.CELESTIAL_CLOUD_FLUFFY.get().defaultBlockState();

    public CelestiaTerrain(long seed) {
        this.seed = seed;
    }

    // ------------------------------------------------------------------ шум

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static long hash(long seed, long a, long b, long c) {
        return mix(seed ^ mix(a * 0x9E3779B97F4A7C15L ^ mix(b * 0xC2B2AE3D27D4EB4FL ^ mix(c))));
    }

    /** 20 бит хеша как число [0, 1). */
    private static double unit(long h, int shift) {
        return ((h >>> shift) & 0xFFFFFL) / 1048576.0;
    }

    private double lattice(int x, int z, long salt) {
        return unit(hash(seed, salt, x, z), 0);
    }

    /** Гладкий value-noise, значения [-1, 1]. */
    private double noise(double x, double z, long salt) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = x - x0;
        double fz = z - z0;
        double sx = fx * fx * (3 - 2 * fx);
        double sz = fz * fz * (3 - 2 * fz);
        double a = lattice(x0, z0, salt);
        double b = lattice(x0 + 1, z0, salt);
        double c = lattice(x0, z0 + 1, salt);
        double d = lattice(x0 + 1, z0 + 1, salt);
        double v = (a + (b - a) * sx) + ((c + (d - c) * sx) - (a + (b - a) * sx)) * sz;
        return v * 2.0 - 1.0;
    }

    private static double smooth(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3 - 2 * t);
    }

    // ------------------------------------------------------------------ острова

    private Island islandFor(Layer layer, int cellX, int cellZ) {
        long h = hash(seed, layer.salt(), cellX, cellZ);
        if (unit(h, 0) >= layer.chance()) return null;
        int r = layer.rMin() + (int) (unit(h, 20) * (layer.rMax() - layer.rMin() + 1));
        int margin = (int) Math.ceil(r * 1.4) + 2;               // остров целиком помещается в клетку
        int free = Math.max(0, layer.cell() - 2 * margin);
        long h2 = mix(h + 1);
        int cx = cellX * layer.cell() + margin + (int) (unit(h, 40) * (free + 1));
        int cz = cellZ * layer.cell() + margin + (int) (unit(h2, 0) * (free + 1));
        int cy = layer.yMin() + (int) (unit(h2, 20) * (layer.yMax() - layer.yMin() + 1));
        if (Math.hypot(cx, cz) < r * 1.4 + 80) return null;      // вокруг стартового острова пусто
        return new Island(cx, cz, r, cy, (int) (r * layer.depthK()), layer.topAmp(), 0, mix(h2 + 2));
    }

    /** out[0] = низ, out[1] = верх, out[2] = t*1000 (0 - центр, 1000 - край). */
    private boolean column(Island is, int x, int z, int[] out) {
        double dx = x - is.cx();
        double dz = z - is.cz();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d >= is.r() * 1.4) return false;
        double rr = is.r() * (1.0 + 0.28 * noise(x / 24.0, z / 24.0, is.salt())
                + 0.10 * noise(x / 9.0, z / 9.0, is.salt() + 7));
        double t = d / rr;
        if (t >= 1.0) return false;
        double h = 1.0 - t * t;
        double relief = is.topAmp() * (0.45 + 0.55 * (noise(x / 13.0, z / 13.0, is.salt() + 13) * 0.5 + 0.5));
        double f = is.flat() > 0 ? smooth((d - is.flat()) / 10.0) : 1.0;
        int top = is.cy() + (int) Math.round(relief * (1.0 - t * t * t) * f);
        double dn = 0.85 + 0.3 * (noise(x / 11.0, z / 11.0, is.salt() + 29) * 0.5 + 0.5);
        int bottom = is.cy() - (int) Math.round(is.depth() * Math.pow(h, 0.8) * dn) - 1;
        out[0] = Math.max(bottom, MIN_Y + 1);
        out[1] = Math.min(top, MIN_Y + HEIGHT - 8);
        out[2] = (int) (t * 1000);
        return true;
    }

    private void collect(int x, int z, Island[] buf) {
        buf[0] = SPAWN;
        for (int i = 0; i < LAYERS.length; i++) {
            Layer l = LAYERS[i];
            buf[i + 1] = islandFor(l, Math.floorDiv(x, l.cell()), Math.floorDiv(z, l.cell()));
        }
    }

    // ------------------------------------------------------------------ облачное море

    /** Верхняя точка облачного моря в колонке. */
    public int cloudTop(int x, int z) {
        double v = 6.0 * noise(x / 70.0, z / 70.0, CLOUD_SALT)
                + 3.0 * noise(x / 19.0, z / 19.0, CLOUD_SALT + 1)
                + 1.5 * noise(x / 6.0, z / 6.0, CLOUD_SALT + 2);
        return 38 + (int) Math.round(v);
    }

    // ------------------------------------------------------------------ публичное API

    /** Самый высокий твёрдый блок колонки. */
    public int topHeight(int x, int z) {
        if (Math.abs(x) >= CelestiaDimension.WORLD_HALF || Math.abs(z) >= CelestiaDimension.WORLD_HALF) return MIN_Y;
        int best = cloudTop(x, z);
        Island[] buf = new Island[LAYERS.length + 1];
        int[] out = new int[3];
        collect(x, z, buf);
        for (Island is : buf) {
            if (is != null && column(is, x, z, out)) best = Math.max(best, out[1]);
        }
        return best;
    }

    /** Заполняет одну колонку блоков через sink (вызывается по каждому непустому блоку). */
    public void fillColumn(int x, int z, BlockSink sink) {
        if (Math.abs(x) >= CelestiaDimension.WORLD_HALF || Math.abs(z) >= CelestiaDimension.WORLD_HALF) return;

        int ct = cloudTop(x, z);
        for (int y = MIN_Y; y <= ct; y++) {
            sink.set(y, y >= ct - 1 ? fluffy : cloud);
        }

        Island[] buf = new Island[LAYERS.length + 1];
        int[] out = new int[3];
        collect(x, z, buf);
        for (Island is : buf) {
            if (is == null || !column(is, x, z, out)) continue;
            int bottom = out[0];
            int top = out[1];
            boolean edge = out[2] > 880 && noise(x / 7.0, z / 7.0, is.salt() + 41) > -0.25;
            boolean icy = is.r() >= 20 && is.flat() == 0 && noise(x / 20.0, z / 20.0, is.salt() + 99) > 0.62;
            BlockState topBlock = edge ? sand : icy ? ice : earth;
            for (int y = bottom; y <= top; y++) {
                int d = top - y;
                BlockState state;
                if (d == 0) {
                    state = topBlock;
                } else if (d <= 3) {
                    state = topBlock == sand ? sand : earth;
                } else {
                    double r = unit(hash(seed, x, y, z), 0);
                    if (r < 0.012) state = ore;
                    else if (y - bottom <= 2 && r > 0.975) state = glow;
                    else state = stone;
                }
                sink.set(y, state);
            }
        }
    }
}
