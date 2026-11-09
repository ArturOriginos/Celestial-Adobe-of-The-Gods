package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Модель из Blockbench (.bbmodel), сохранённая как простой текст assets/celestial/bbdata/NAME.txt
 * (по одному кубу на строку: группа, from, to и UV шести граней). Рисуется напрямую, кубами.
 * Координаты в пикселях модели (y вверх, перёд к -Z); вызывающий сам задаёт масштаб (обычно 1/16).
 */
public final class BBModel {
    public static final float NO_CLIP = -1.0E9F;
    private static final int STRIDE = 30; // x0,y0,z0,x1,y1,z1 + 6 граней по (u0,v0,u1,v1)

    private final float texW;
    private final float texH;
    private final int count;
    private final int[] group;
    private final float[] v;
    /** Поворот элемента: ox, oy, oz, ось (0=x,1=y,2=z), угол в градусах (на элемент 5 чисел; угол 0 - нет поворота). */
    private final float[] rot;

    private BBModel(float texW, float texH, int[] group, float[] v, float[] rot) {
        this.texW = texW;
        this.texH = texH;
        this.group = group;
        this.v = v;
        this.rot = rot;
        this.count = group.length;
    }

    public static BBModel load(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, "bbdata/" + name + ".txt");
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                Minecraft.getInstance().getResourceManager().open(id), StandardCharsets.UTF_8))) {
            String[] head = r.readLine().trim().split(" ");
            float w = Float.parseFloat(head[0]);
            float h = Float.parseFloat(head[1]);
            List<float[]> rows = new ArrayList<>();
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(" ");
                float[] row = new float[p.length];
                for (int i = 0; i < p.length; i++) row[i] = Float.parseFloat(p[i]);
                rows.add(row);
            }
            int[] g = new int[rows.size()];
            float[] data = new float[rows.size() * STRIDE];
            float[] rots = new float[rows.size() * 5];
            for (int i = 0; i < rows.size(); i++) {
                float[] row = rows.get(i);
                g[i] = (int) row[0];
                System.arraycopy(row, 1, data, i * STRIDE, STRIDE);
                if (row.length >= 1 + STRIDE + 5) System.arraycopy(row, 1 + STRIDE, rots, i * 5, 5);
            }
            return new BBModel(w, h, g, data, rots);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Не удалось загрузить модель " + id, e);
        }
    }

    /** Рисует все кубы группы g. Кубы, у которых z0 меньше minZ, пропускаются (обрезка луча). */
    public void renderGroup(VertexConsumer vc, PoseStack.Pose pose, int light, int g, float minZ) {
        renderGroup(vc, pose, light, OverlayTexture.NO_OVERLAY, g, minZ);
    }

    /** То же, но с оверлеем (красная вспышка при уроне). */
    public void renderGroup(VertexConsumer vc, PoseStack.Pose pose, int light, int overlay, int g, float minZ) {
        for (int i = 0; i < count; i++) {
            if (group[i] != g) continue;
            int o = i * STRIDE;
            if (v[o + 2] < minZ) continue;
            curOverlay = overlay;
            setRotation(i);
            cuboid(vc, pose, light, o);
        }
    }

    private int curOverlay = OverlayTexture.NO_OVERLAY;
    private boolean hasRot;
    private float rox, roy, roz, rcos, rsin;
    private int raxis;

    private void setRotation(int i) {
        float angle = rot[i * 5 + 4];
        hasRot = angle != 0.0F;
        if (!hasRot) return;
        rox = rot[i * 5];
        roy = rot[i * 5 + 1];
        roz = rot[i * 5 + 2];
        raxis = (int) rot[i * 5 + 3];
        double rad = Math.toRadians(angle);
        rcos = (float) Math.cos(rad);
        rsin = (float) Math.sin(rad);
    }

    private void cuboid(VertexConsumer vc, PoseStack.Pose pose, int light, int o) {
        float x0 = v[o], y0 = v[o + 1], z0 = v[o + 2], x1 = v[o + 3], y1 = v[o + 4], z1 = v[o + 5];
        // north (-z)
        quad(vc, pose, light, o + 6, 0, 0, -1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0);
        // east (+x)
        quad(vc, pose, light, o + 10, 1, 0, 0, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1);
        // south (+z)
        quad(vc, pose, light, o + 14, 0, 0, 1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1);
        // west (-x)
        quad(vc, pose, light, o + 18, -1, 0, 0, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0);
        // up (+y)
        quad(vc, pose, light, o + 22, 0, 1, 0, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        // down (-y)
        quad(vc, pose, light, o + 26, 0, -1, 0, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
    }

    /** Вершины a (лево-верх), b (право-верх), c (право-низ), d (лево-низ) смотрят снаружи; выводятся против часовой стрелки. */
    private void quad(VertexConsumer vc, PoseStack.Pose pose, int light, int uvo, float nx, float ny, float nz,
                      float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz) {
        float u0 = v[uvo] / texW, v0 = v[uvo + 1] / texH, u1 = v[uvo + 2] / texW, v1 = v[uvo + 3] / texH;
        vertex(vc, pose, light, ax, ay, az, u0, v0, nx, ny, nz);
        vertex(vc, pose, light, dx, dy, dz, u0, v1, nx, ny, nz);
        vertex(vc, pose, light, cx, cy, cz, u1, v1, nx, ny, nz);
        vertex(vc, pose, light, bx, by, bz, u1, v0, nx, ny, nz);
    }

    private void vertex(VertexConsumer vc, PoseStack.Pose pose, int light, float x, float y, float z,
                        float u, float vv, float nx, float ny, float nz) {
        if (hasRot) {
            float dx = x - rox, dy = y - roy, dz = z - roz;
            float px, py, pz, qx, qy, qz;
            switch (raxis) {
                case 0 -> { // вокруг X
                    px = dx; py = dy * rcos - dz * rsin; pz = dy * rsin + dz * rcos;
                    qx = nx; qy = ny * rcos - nz * rsin; qz = ny * rsin + nz * rcos;
                }
                case 1 -> { // вокруг Y
                    px = dx * rcos + dz * rsin; py = dy; pz = -dx * rsin + dz * rcos;
                    qx = nx * rcos + nz * rsin; qy = ny; qz = -nx * rsin + nz * rcos;
                }
                default -> { // вокруг Z
                    px = dx * rcos - dy * rsin; py = dx * rsin + dy * rcos; pz = dz;
                    qx = nx * rcos - ny * rsin; qy = nx * rsin + ny * rcos; qz = nz;
                }
            }
            x = rox + px;
            y = roy + py;
            z = roz + pz;
            nx = qx;
            ny = qy;
            nz = qz;
        }
        vc.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, vv)
                .setOverlay(curOverlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
