package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Скелет и анимации из Blockbench (assets/celestial/bbdata/NAME.rig.json): иерархия костей, базовые повороты
 * и ключевые кадры. Анимации складываются поверх базовой позы, как в Blockbench (повороты и сдвиги суммируются,
 * масштаб перемножается), поэтому можно смешивать несколько анимаций сразу. Единицы - пиксели модели.
 */
public final class BBRig {
    private final String[] names;
    private final int[] parent;
    private final float[][] origin;
    private final float[][] rest;
    private final int[][] chain;
    private final Map<String, Integer> index = new HashMap<>();
    private final Map<String, Anim> anims = new HashMap<>();

    private static final class Channel {
        float[] t;
        float[][] v;
        boolean[] smooth;
    }

    private static final class Anim {
        float length;
        String loop;
        Channel[] rot, pos, scl;
    }

    /** Текущая поза: повороты (градусы), сдвиги и масштабы по костям. */
    public final class Pose {
        public final float[] rot = new float[names.length * 3];
        public final float[] pos = new float[names.length * 3];
        public final float[] scl = new float[names.length * 3];

        private Pose() {
            for (int g = 0; g < names.length; g++) {
                System.arraycopy(rest[g], 0, rot, g * 3, 3);
                scl[g * 3] = scl[g * 3 + 1] = scl[g * 3 + 2] = 1.0F;
            }
        }

        public void addRot(int g, float x, float y, float z) {
            rot[g * 3] += x;
            rot[g * 3 + 1] += y;
            rot[g * 3 + 2] += z;
        }
    }

    private BBRig(JsonObject json) {
        JsonArray gs = json.getAsJsonArray("groups");
        int n = gs.size();
        names = new String[n];
        parent = new int[n];
        origin = new float[n][];
        rest = new float[n][];
        chain = new int[n][];
        for (int i = 0; i < n; i++) {
            JsonObject g = gs.get(i).getAsJsonObject();
            names[i] = g.get("n").getAsString();
            parent[i] = g.get("p").getAsInt();
            origin[i] = floats(g.getAsJsonArray("o"));
            rest[i] = floats(g.getAsJsonArray("r"));
            index.put(names[i], i);
        }
        for (int i = 0; i < n; i++) {
            int depth = 0;
            for (int p = i; p >= 0; p = parent[p]) depth++;
            chain[i] = new int[depth];
            int k = depth;
            for (int p = i; p >= 0; p = parent[p]) chain[i][--k] = p; // от корня к самой кости
        }
        JsonObject as = json.getAsJsonObject("anims");
        for (Map.Entry<String, JsonElement> e : as.entrySet()) {
            JsonObject a = e.getValue().getAsJsonObject();
            Anim anim = new Anim();
            anim.length = Math.max(0.001F, a.get("len").getAsFloat());
            anim.loop = a.get("loop").getAsString();
            anim.rot = new Channel[n];
            anim.pos = new Channel[n];
            anim.scl = new Channel[n];
            for (Map.Entry<String, JsonElement> ge : a.getAsJsonObject("g").entrySet()) {
                Integer g = index.get(ge.getKey());
                if (g == null) continue;
                JsonObject chs = ge.getValue().getAsJsonObject();
                if (chs.has("rotation")) anim.rot[g] = channel(chs.getAsJsonArray("rotation"));
                if (chs.has("position")) anim.pos[g] = channel(chs.getAsJsonArray("position"));
                if (chs.has("scale")) anim.scl[g] = channel(chs.getAsJsonArray("scale"));
            }
            anims.put(e.getKey(), anim);
        }
    }

    private static float[] floats(JsonArray a) {
        float[] r = new float[a.size()];
        for (int i = 0; i < r.length; i++) r[i] = a.get(i).getAsFloat();
        return r;
    }

    private static Channel channel(JsonArray kfs) {
        Channel c = new Channel();
        int n = kfs.size();
        c.t = new float[n];
        c.v = new float[n][3];
        c.smooth = new boolean[n];
        for (int i = 0; i < n; i++) {
            JsonArray k = kfs.get(i).getAsJsonArray();
            c.t[i] = k.get(0).getAsFloat();
            for (int j = 0; j < 3; j++) c.v[i][j] = k.get(1 + j).getAsFloat();
            c.smooth[i] = k.get(4).getAsInt() == 1;
        }
        return c;
    }

    public static BBRig load(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, "bbdata/" + name + ".rig.json");
        try (Reader r = new InputStreamReader(Minecraft.getInstance().getResourceManager().open(id), StandardCharsets.UTF_8)) {
            return new BBRig(JsonParser.parseReader(r).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Не удалось загрузить скелет " + id, e);
        }
    }

    public int index(String group) {
        Integer i = index.get(group);
        if (i == null) throw new IllegalArgumentException("Нет кости " + group);
        return i;
    }

    public int groupCount() { return names.length; }

    public boolean hasAnim(String name) { return anims.containsKey(name); }

    public float length(String name) {
        Anim a = anims.get(name);
        return a == null ? 1.0F : a.length;
    }

    /** Новая поза: базовые повороты костей, без анимации. */
    public Pose newPose() {
        return new Pose();
    }

    /** Добавляет к позе анимацию name в момент time (секунды) с весом weight. */
    public void apply(Pose p, String name, float time, float weight) {
        Anim a = anims.get(name);
        if (a == null || weight == 0.0F) return;
        float t = "loop".equals(a.loop) ? (time % a.length + a.length) % a.length : Math.max(0.0F, Math.min(time, a.length));
        float[] out = new float[3];
        for (int g = 0; g < names.length; g++) {
            if (a.rot[g] != null) {
                sample(a.rot[g], t, out);
                for (int j = 0; j < 3; j++) p.rot[g * 3 + j] += out[j] * weight;
            }
            if (a.pos[g] != null) {
                sample(a.pos[g], t, out);
                for (int j = 0; j < 3; j++) p.pos[g * 3 + j] += out[j] * weight;
            }
            if (a.scl[g] != null) {
                sample(a.scl[g], t, out);
                for (int j = 0; j < 3; j++) p.scl[g * 3 + j] *= 1.0F + (out[j] - 1.0F) * weight;
            }
        }
    }

    private static void sample(Channel c, float time, float[] out) {
        int n = c.t.length;
        if (time <= c.t[0]) { System.arraycopy(c.v[0], 0, out, 0, 3); return; }
        if (time >= c.t[n - 1]) { System.arraycopy(c.v[n - 1], 0, out, 0, 3); return; }
        int i = 0;
        while (i < n - 2 && time > c.t[i + 1]) i++;
        float f = (time - c.t[i]) / Math.max(1.0E-5F, c.t[i + 1] - c.t[i]);
        boolean smooth = c.smooth[i] || c.smooth[i + 1];
        for (int j = 0; j < 3; j++) {
            float p1 = c.v[i][j], p2 = c.v[i + 1][j];
            if (smooth) {
                float p0 = i > 0 ? c.v[i - 1][j] : p1;
                float p3 = i + 2 < n ? c.v[i + 2][j] : p2;
                out[j] = 0.5F * ((2 * p1) + (-p0 + p2) * f + (2 * p0 - 5 * p1 + 4 * p2 - p3) * f * f
                        + (-p0 + 3 * p1 - 3 * p2 + p3) * f * f * f);
            } else {
                out[j] = p1 + (p2 - p1) * f;
            }
        }
    }

    /** Накладывает на PoseStack преобразование кости g (родители, затем сама кость): сдвиг, поворот ZYX, масштаб вокруг точки кости. */
    public void transform(PoseStack ps, Pose p, int g) {
        for (int b : chain[g]) {
            float[] o = origin[b];
            ps.translate(p.pos[b * 3], p.pos[b * 3 + 1], p.pos[b * 3 + 2]);
            ps.translate(o[0], o[1], o[2]);
            float rz = p.rot[b * 3 + 2], ry = p.rot[b * 3 + 1], rx = p.rot[b * 3];
            if (rz != 0.0F) ps.mulPose(Axis.ZP.rotationDegrees(rz));
            if (ry != 0.0F) ps.mulPose(Axis.YP.rotationDegrees(ry));
            if (rx != 0.0F) ps.mulPose(Axis.XP.rotationDegrees(rx));
            ps.scale(p.scl[b * 3], p.scl[b * 3 + 1], p.scl[b * 3 + 2]);
            ps.translate(-o[0], -o[1], -o[2]);
        }
    }
}
