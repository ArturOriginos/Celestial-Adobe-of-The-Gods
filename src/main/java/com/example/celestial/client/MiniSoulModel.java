package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Модель мини-души (51 кубоид из soul_mini_java_model.json), рисуется напрямую.
 * Система координат: пиксели модели (y вверх, лицо к -Z), центр по x/z = 8, низ по y = 0;
 * вызывающий сам задаёт масштаб и поворот. Нимб, глаза и кончик хвоста светятся.
 */
public final class MiniSoulModel {
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/item/mini_soul.png");

    /** x0,y0,z0, x1,y1,z1 (пиксели модели), u0,v0,u1,v1 (пиксели текстуры 16x16), светится (1/0). */
    private static final float[][] CUBOIDS = {
        {10F, 2F, 11F, 11F, 3F, 12F, 5F, 5F, 7F, 7F, 0},
        {6F, 7F, 11F, 7F, 8F, 12F, 5F, 5F, 7F, 7F, 0},
        {6F, 9F, 11F, 11F, 10F, 12F, 13F, 1F, 15F, 3F, 1},
        {9F, 2F, 10F, 10F, 3F, 11F, 9F, 1F, 11F, 3F, 0},
        {7F, 3F, 10F, 10F, 4F, 11F, 9F, 1F, 11F, 3F, 0},
        {7F, 4F, 10F, 9F, 5F, 11F, 9F, 1F, 11F, 3F, 0},
        {5F, 9F, 10F, 7F, 10F, 11F, 13F, 1F, 15F, 3F, 1},
        {10F, 9F, 10F, 12F, 10F, 11F, 13F, 1F, 15F, 3F, 1},
        {9F, 2F, 9F, 10F, 3F, 10F, 9F, 1F, 11F, 3F, 0},
        {6F, 3F, 9F, 7F, 4F, 10F, 9F, 1F, 11F, 3F, 0},
        {6F, 4F, 9F, 7F, 5F, 10F, 9F, 1F, 11F, 3F, 0},
        {6F, 5F, 9F, 7F, 6F, 10F, 9F, 1F, 11F, 3F, 0},
        {7F, 5F, 9F, 10F, 6F, 10F, 1F, 1F, 3F, 3F, 0},
        {7F, 6F, 9F, 10F, 7F, 10F, 1F, 1F, 3F, 3F, 0},
        {7F, 7F, 9F, 10F, 8F, 10F, 1F, 1F, 3F, 3F, 0},
        {5F, 9F, 9F, 6F, 10F, 10F, 13F, 1F, 15F, 3F, 1},
        {11F, 9F, 9F, 12F, 10F, 10F, 13F, 1F, 15F, 3F, 1},
        {8F, 0F, 8F, 9F, 1F, 9F, 13F, 1F, 15F, 3F, 1},
        {7F, 1F, 8F, 8F, 2F, 9F, 9F, 1F, 11F, 3F, 0},
        {9F, 1F, 8F, 10F, 2F, 9F, 9F, 1F, 11F, 3F, 0},
        {5F, 4F, 8F, 6F, 5F, 9F, 9F, 1F, 11F, 3F, 0},
        {11F, 4F, 8F, 12F, 5F, 9F, 9F, 1F, 11F, 3F, 0},
        {5F, 5F, 8F, 6F, 6F, 9F, 9F, 1F, 11F, 3F, 0},
        {7F, 5F, 8F, 10F, 6F, 9F, 1F, 1F, 3F, 3F, 0},
        {11F, 5F, 8F, 12F, 6F, 9F, 9F, 1F, 11F, 3F, 0},
        {7F, 6F, 8F, 8F, 7F, 9F, 1F, 1F, 3F, 3F, 0},
        {9F, 6F, 8F, 10F, 7F, 9F, 1F, 1F, 3F, 3F, 0},
        {7F, 7F, 8F, 10F, 8F, 9F, 1F, 1F, 3F, 3F, 0},
        {4F, 9F, 8F, 6F, 10F, 9F, 13F, 1F, 15F, 3F, 1},
        {11F, 9F, 8F, 13F, 10F, 9F, 13F, 1F, 15F, 3F, 1},
        {8F, 10F, 8F, 9F, 11F, 9F, 13F, 1F, 15F, 3F, 1},
        {8F, 11F, 8F, 9F, 12F, 9F, 13F, 1F, 15F, 3F, 1},
        {7F, 2F, 7F, 8F, 3F, 8F, 9F, 1F, 11F, 3F, 0},
        {5F, 3F, 7F, 6F, 4F, 8F, 5F, 5F, 7F, 7F, 0},
        {10F, 3F, 7F, 11F, 4F, 8F, 9F, 1F, 11F, 3F, 0},
        {10F, 4F, 7F, 11F, 5F, 8F, 9F, 1F, 11F, 3F, 0},
        {7F, 5F, 7F, 10F, 6F, 8F, 1F, 1F, 3F, 3F, 0},
        {10F, 5F, 7F, 11F, 6F, 8F, 9F, 1F, 11F, 3F, 0},
        {7F, 6F, 7F, 8F, 7F, 8F, 1F, 5F, 3F, 7F, 1},
        {8F, 6F, 7F, 9F, 7F, 8F, 1F, 1F, 3F, 3F, 0},
        {9F, 6F, 7F, 10F, 7F, 8F, 1F, 5F, 3F, 7F, 1},
        {7F, 7F, 7F, 10F, 8F, 8F, 1F, 1F, 3F, 3F, 0},
        {5F, 9F, 7F, 6F, 10F, 8F, 13F, 1F, 15F, 3F, 1},
        {11F, 9F, 7F, 12F, 10F, 8F, 13F, 1F, 15F, 3F, 1},
        {7F, 2F, 6F, 8F, 3F, 7F, 9F, 1F, 11F, 3F, 0},
        {7F, 3F, 6F, 10F, 4F, 7F, 9F, 1F, 11F, 3F, 0},
        {8F, 4F, 6F, 10F, 5F, 7F, 9F, 1F, 11F, 3F, 0},
        {11F, 5F, 6F, 12F, 6F, 7F, 5F, 5F, 7F, 7F, 0},
        {6F, 9F, 6F, 7F, 10F, 7F, 13F, 1F, 15F, 3F, 1},
        {10F, 9F, 6F, 11F, 10F, 7F, 13F, 1F, 15F, 3F, 1},
        {7F, 9F, 5F, 11F, 10F, 6F, 13F, 1F, 15F, 3F, 1},
    };

    private MiniSoulModel() {}

    public static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        for (float[] c : CUBOIDS) {
            drawCuboid(vc, pose, c, c[10] > 0.5F ? LightTexture.FULL_BRIGHT : packedLight);
        }
    }

    private static void drawCuboid(VertexConsumer vc, PoseStack.Pose pose, float[] c, int light) {
        float x0 = (c[0] - 8.0F) / 16.0F, y0 = c[1] / 16.0F, z0 = (c[2] - 8.0F) / 16.0F;
        float x1 = (c[3] - 8.0F) / 16.0F, y1 = c[4] / 16.0F, z1 = (c[5] - 8.0F) / 16.0F;
        float u0 = c[6] / 16.0F, v0 = c[7] / 16.0F, u1 = c[8] / 16.0F, v1 = c[9] / 16.0F;
        quad(vc, pose, light, u0, v0, u1, v1, 0, 0, -1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0);
        quad(vc, pose, light, u0, v0, u1, v1, 0, 0, 1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1);
        quad(vc, pose, light, u0, v0, u1, v1, -1, 0, 0, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0);
        quad(vc, pose, light, u0, v0, u1, v1, 1, 0, 0, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1);
        quad(vc, pose, light, u0, v0, u1, v1, 0, -1, 0, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
        quad(vc, pose, light, u0, v0, u1, v1, 0, 1, 0, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, int light,
                             float u0, float v0, float u1, float v1, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vertex(vc, pose, light, ax, ay, az, u0, v0, nx, ny, nz);
        vertex(vc, pose, light, bx, by, bz, u1, v0, nx, ny, nz);
        vertex(vc, pose, light, cx, cy, cz, u1, v1, nx, ny, nz);
        vertex(vc, pose, light, dx, dy, dz, u0, v1, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, int light,
                               float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
