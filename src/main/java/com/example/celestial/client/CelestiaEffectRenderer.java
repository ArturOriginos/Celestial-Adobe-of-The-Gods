package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.CelestiaEffect;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Рисует светящиеся столпы, падающие звёзды и лучи Бога Селестии (аддитивный свет, без света окружения). */
public class CelestiaEffectRenderer extends EntityRenderer<CelestiaEffect> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/light_beam.png");
    private static final int SIDES = 14;

    public CelestiaEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(CelestiaEffect entity, Frustum frustum, double camX, double camY, double camZ) {
        return true; // столп высокий: не отсекаем по основанию
    }

    @Override
    public ResourceLocation getTextureLocation(CelestiaEffect entity) {
        return TEXTURE;
    }

    @Override
    public void render(CelestiaEffect e, float yaw, float pt, PoseStack pose, MultiBufferSource buffer, int light) {
        float t = e.tickCount + pt;
        int warm = e.getWarmup();
        float life = Math.max(1, e.getLife());
        float r = e.getRadius();
        boolean active = t >= warm;
        float f = active ? Mth.clamp(1.0F - (t - warm) / life, 0.0F, 1.0F) : 0.0F; // 1 -> 0 после вспышки
        VertexConsumer vc = buffer.getBuffer(RenderType.eyes(TEXTURE));

        pose.pushPose();
        switch (e.getKind()) {
            case CelestiaEffect.KIND_PILLAR -> {
                if (!active) {
                    float pulse = 0.5F + 0.5F * Mth.sin(t * 0.6F);
                    ring(vc, pose, r, r - 0.4F, 0.06F, 255, 170, 60, (int) (150 + 90 * pulse));
                    disc(vc, pose, r, 0.05F, 255, 190, 80, 40);
                    column(vc, pose, 0.12F, 0.0F, 45.0F, 255, 240, 190, 90, 0.3F);
                } else {
                    float rad = r * (0.30F + 0.30F * f) + 0.2F;
                    disc(vc, pose, r, 0.07F, 255, 215, 110, (int) (140 * f));
                    column(vc, pose, rad, 0.0F, 60.0F, 255, 222, 130, (int) (170 * f), 0.35F);
                    column(vc, pose, rad * 0.5F, 0.0F, 60.0F, 255, 252, 230, (int) (240 * f), 0.5F);
                }
            }
            case CelestiaEffect.KIND_STAR -> {
                if (!active) {
                    float remaining = warm - t;
                    float y = remaining * 1.4F;                  // звезда снижается к земле
                    ring(vc, pose, r, r - 0.3F, 0.06F, 255, 170, 60, 150);
                    column(vc, pose, 0.28F, y, y + 7.0F, 255, 250, 225, 235, 0.0F);
                    column(vc, pose, 0.55F, y, y + 4.0F, 255, 215, 110, 120, 0.0F);
                } else {
                    disc(vc, pose, r, 0.07F, 255, 215, 110, (int) (170 * f));
                    column(vc, pose, r * 0.3F * f + 0.2F, 0.0F, 9.0F, 255, 240, 170, (int) (210 * f), 0.2F);
                }
            }
            default -> { // луч: рисуем вдоль +X как стрелу, колонна строится вдоль Y и поворачивается на X
                pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pt, e.yRotO, e.getYRot()) - 90.0F));
                pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, e.xRotO, e.getXRot())));
                pose.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                float len = e.getLength();
                if (!active) {
                    column(vc, pose, 0.07F, 0.0F, len, 255, 190, 80, 140, 0.4F);
                } else {
                    float rad = 0.25F + 0.45F * f;
                    column(vc, pose, rad, 0.0F, len, 255, 222, 130, (int) (170 * f), 0.5F);
                    column(vc, pose, rad * 0.45F, 0.0F, len, 255, 252, 235, (int) (240 * f), 0.7F);
                }
            }
        }
        pose.popPose();
    }

    // ---------------------------------------------------------------- примитивы
    private static void vert(VertexConsumer vc, Matrix4f m, float x, float y, float z, int r, int g, int b, int a) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, Mth.clamp(a, 0, 255)).setUv(0.5F, 0.5F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0F, 1.0F, 0.0F);
    }

    /** Четырёхугольник, нарисованный с двух сторон (отсечение граней отключаем вручную). */
    private static void quad(VertexConsumer vc, Matrix4f m, float[] p, int[] rgb, int a0, int a1) {
        // p: x0,y0,z0, x1,y1,z1, x2,y2,z2, x3,y3,z3; a0 - альфа вершин 0 и 1, a1 - вершин 2 и 3
        int[] al = {a0, a0, a1, a1};
        for (int i = 0; i < 4; i++) vert(vc, m, p[i * 3], p[i * 3 + 1], p[i * 3 + 2], rgb[0], rgb[1], rgb[2], al[i]);
        for (int i = 3; i >= 0; i--) vert(vc, m, p[i * 3], p[i * 3 + 1], p[i * 3 + 2], rgb[0], rgb[1], rgb[2], al[i]);
    }

    /** Вертикальная призма (столп). topFade - доля альфы, остающаяся у верхнего края. */
    private static void column(VertexConsumer vc, PoseStack pose, float radius, float y0, float y1,
                               int cr, int cg, int cb, int alpha, float topFade) {
        if (alpha <= 0) return;
        Matrix4f m = pose.last().pose();
        int[] rgb = {cr, cg, cb};
        int top = (int) (alpha * topFade);
        for (int i = 0; i < SIDES; i++) {
            float a0 = Mth.TWO_PI * i / SIDES, a1 = Mth.TWO_PI * (i + 1) / SIDES;
            float x0 = Mth.cos(a0) * radius, z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius, z1 = Mth.sin(a1) * radius;
            quad(vc, m, new float[]{x0, y0, z0, x1, y0, z1, x1, y1, z1, x0, y1, z0}, rgb, alpha, top);
        }
    }

    /** Заполненный диск на земле. */
    private static void disc(VertexConsumer vc, PoseStack pose, float radius, float y, int cr, int cg, int cb, int alpha) {
        if (alpha <= 0) return;
        Matrix4f m = pose.last().pose();
        int[] rgb = {cr, cg, cb};
        for (int i = 0; i < SIDES * 2; i++) {
            float a0 = Mth.TWO_PI * i / (SIDES * 2), a1 = Mth.TWO_PI * (i + 1) / (SIDES * 2);
            quad(vc, m, new float[]{0, y, 0, 0, y, 0, Mth.cos(a1) * radius, y, Mth.sin(a1) * radius,
                    Mth.cos(a0) * radius, y, Mth.sin(a0) * radius}, rgb, alpha / 3, alpha);
        }
    }

    /** Кольцо на земле (предупреждение). */
    private static void ring(VertexConsumer vc, PoseStack pose, float outer, float inner, float y,
                             int cr, int cg, int cb, int alpha) {
        Matrix4f m = pose.last().pose();
        int[] rgb = {cr, cg, cb};
        int n = SIDES * 2;
        for (int i = 0; i < n; i++) {
            float a0 = Mth.TWO_PI * i / n, a1 = Mth.TWO_PI * (i + 1) / n;
            quad(vc, m, new float[]{
                    Mth.cos(a0) * inner, y, Mth.sin(a0) * inner, Mth.cos(a1) * inner, y, Mth.sin(a1) * inner,
                    Mth.cos(a1) * outer, y, Mth.sin(a1) * outer, Mth.cos(a0) * outer, y, Mth.sin(a0) * outer}, rgb, alpha, alpha);
        }
    }
}
