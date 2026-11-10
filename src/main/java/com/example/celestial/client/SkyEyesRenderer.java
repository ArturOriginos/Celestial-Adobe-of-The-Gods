package com.example.celestial.client;

import com.example.celestial.CelestialMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Красные глаза Аида в небе: появляются после убийства Всезнающего Ангела, моргают и медленно пульсируют. */
@EventBusSubscriber(modid = CelestialMod.MODID, value = Dist.CLIENT)
public final class SkyEyesRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "textures/environment/hades_eye.png");
    private static final float ASPECT = 118.0F / 160.0F; // высота / ширина текстуры 160x118
    private static final float DISTANCE = 100.0F;
    private static final long FADE_MS = 6000L;

    /** азимут (град), высота над горизонтом (град), ширина (блоков на расстоянии 100), сдвиг фазы */
    private static final float[][] EYES = {
            {20, 58, 30, 0.0F}, {95, 42, 20, 1.7F}, {160, 64, 34, 3.1F}, {230, 36, 22, 4.4F},
            {290, 60, 26, 2.2F}, {335, 30, 18, 5.3F}, {60, 24, 16, 0.9F}, {195, 28, 18, 3.8F}};

    private static boolean active;
    private static long startMs;
    private static boolean animate;

    private SkyEyesRenderer() {}

    public static void set(boolean on, boolean animated) {
        active = on;
        animate = animated;
        startMs = Util.getMillis();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!active || event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimensionType().hasSkyLight()) return; // только там, где есть небо

        long now = Util.getMillis();
        float fade = animate ? Mth.clamp((now - startMs) / (float) FADE_MS, 0.0F, 1.0F) : 1.0F;
        float seconds = now / 1000.0F;
        Matrix4f view = new Matrix4f(event.getModelViewMatrix());

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Vector3f dir = new Vector3f();
        Vector3f right = new Vector3f();
        Vector3f up = new Vector3f();
        for (float[] eye : EYES) {
            float az = eye[0] * Mth.DEG_TO_RAD;
            float el = eye[1] * Mth.DEG_TO_RAD;
            dir.set(Mth.sin(az) * Mth.cos(el), Mth.sin(el), -Mth.cos(az) * Mth.cos(el)).normalize();
            right.set(dir).cross(0.0F, 1.0F, 0.0F).normalize();   // право = вперёд x вверх
            up.set(right).cross(dir).normalize();                  // вверх = право x вперёд

            // моргание раз в ~7 секунд (у каждого глаза своя фаза) и плавная пульсация яркости
            float cycle = (seconds + eye[3] * 3.0F) % 7.0F;
            float open = cycle < 6.6F ? 1.0F : 1.0F - Mth.sin((cycle - 6.6F) / 0.4F * Mth.PI);
            float pulse = 0.82F + 0.18F * Mth.sin(seconds * 0.9F + eye[3]);
            int alpha = Mth.clamp((int) (255.0F * fade * pulse), 0, 255);

            float hw = eye[2] * 0.5F;
            float hh = hw * ASPECT * Math.max(0.06F, open);
            float cx = dir.x * DISTANCE, cy = dir.y * DISTANCE, cz = dir.z * DISTANCE;

            vertex(buffer, view, cx - right.x * hw + up.x * hh, cy - right.y * hw + up.y * hh, cz - right.z * hw + up.z * hh, 0.0F, 0.0F, alpha);
            vertex(buffer, view, cx - right.x * hw - up.x * hh, cy - right.y * hw - up.y * hh, cz - right.z * hw - up.z * hh, 0.0F, 1.0F, alpha);
            vertex(buffer, view, cx + right.x * hw - up.x * hh, cy + right.y * hw - up.y * hh, cz + right.z * hw - up.z * hh, 1.0F, 1.0F, alpha);
            vertex(buffer, view, cx + right.x * hw + up.x * hh, cy + right.y * hw + up.y * hh, cz + right.z * hw + up.z * hh, 1.0F, 0.0F, alpha);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f view, float x, float y, float z, float u, float v, int alpha) {
        buffer.addVertex(view, x, y, z).setUv(u, v).setColor(255, 255, 255, alpha);
    }
}
