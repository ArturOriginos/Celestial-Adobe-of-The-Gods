package com.example.celestial.client;

import com.example.celestial.CelestialMod;
import com.example.celestial.network.HadesCutscenePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Катсцена появления Аида (клиент): камера отлетает от игрока и медленно кружит перед Аидом, сверху и снизу
 * чёрные полосы, внизу реплики с эффектом печатной машинки. Камерой служит временная сущность, которая в мир не
 * добавляется, поэтому игрок и мир не затрагиваются; в конце камера возвращается к игроку.
 */
@EventBusSubscriber(modid = CelestialMod.MODID, value = Dist.CLIENT)
public final class HadesCutscene {
    private static int entityId = -1;
    private static int total;
    private static int ticksLeft;
    private static ArmorStand camera;

    private static String speaker = "";
    private static String text = "";
    private static int lineTicks;
    private static int lineTotal;

    private HadesCutscene() {}

    public static boolean isActive() {
        return ticksLeft > 0;
    }

    public static void handle(HadesCutscenePayload payload) {
        switch (payload.action()) {
            case HadesCutscenePayload.START -> start(payload.entityId(), payload.ticks());
            case HadesCutscenePayload.LINE -> {
                speaker = payload.speaker();
                text = payload.text();
                lineTicks = lineTotal = payload.ticks();
            }
            default -> stop();
        }
    }

    private static void start(int id, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        entityId = id;
        total = ticksLeft = ticks;
        speaker = text = "";
        lineTicks = 0;
        camera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
        Entity hades = mc.level.getEntity(id);
        Entity from = hades != null ? hades : mc.player;
        if (from != null) {
            camera.setPos(from.getX(), from.getY() + 3.0D, from.getZ());
            camera.xo = camera.getX();
            camera.yo = camera.getY();
            camera.zo = camera.getZ();
        }
        mc.setCameraEntity(camera);
    }

    private static void stop() {
        ticksLeft = 0;
        lineTicks = 0;
        Minecraft mc = Minecraft.getInstance();
        if (camera != null && mc.getCameraEntity() == camera && mc.player != null) mc.setCameraEntity(mc.player);
        camera = null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (ticksLeft <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || camera == null) {
            stop();
            return;
        }
        ticksLeft--;
        if (lineTicks > 0) lineTicks--;
        Entity hades = mc.level.getEntity(entityId);
        if (ticksLeft <= 0 || (hades == null && total - ticksLeft > 40)) {
            stop();
            return;
        }
        if (hades == null) return;
        if (mc.getCameraEntity() != camera) mc.setCameraEntity(camera);

        // кадр: спереди от Аида, ближе и выше по ходу сцены, лёгкий облёт слева направо
        float p = (total - ticksLeft) / (float) total;
        float ease = p * p * (3.0F - 2.0F * p);
        double radius = Mth.lerp(ease, 12.0D, 6.5D);
        double height = Mth.lerp(ease, 1.4D, 3.4D);
        double yawRad = Math.toRadians(hades.getYRot());
        double base = Math.atan2(Math.cos(yawRad), -Math.sin(yawRad)); // направление взгляда Аида в плоскости XZ
        double angle = base + Math.toRadians(Mth.lerp(ease, -38.0D, 38.0D));
        double cx = hades.getX() + Math.cos(angle) * radius;
        double cz = hades.getZ() + Math.sin(angle) * radius;
        double cy = hades.getY() + height;

        double tx = hades.getX(), ty = hades.getY() + 4.3D, tz = hades.getZ();
        double dx = tx - cx, dy = ty - cy, dz = tz - cz;
        float yaw = (float) (Mth.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0D / Math.PI);

        camera.xo = camera.getX();
        camera.yo = camera.getY();
        camera.zo = camera.getZ();
        camera.xOld = camera.xo;
        camera.yOld = camera.yo;
        camera.zOld = camera.zo;
        camera.setPos(cx, cy - camera.getEyeHeight(), cz);
        camera.yRotO = camera.getYRot();
        camera.xRotO = camera.getXRot();
        camera.setYRot(yaw);
        camera.setXRot(pitch);
    }

    /** Чёрные полосы и реплика. Вызывается из отрисовки полосы здоровья Аида. */
    public static void drawOverlay(GuiGraphics g) {
        if (ticksLeft <= 0) return;
        int w = g.guiWidth(), h = g.guiHeight();
        int elapsed = total - ticksLeft;
        float appear = Math.min(1.0F, Math.min(elapsed / 20.0F, ticksLeft / 20.0F));
        int bar = (int) (h * 0.12F * appear);
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);

        if (lineTicks > 0 && !text.isEmpty()) {
            Font font = Minecraft.getInstance().font;
            Component line = Component.translatable(text);
            String full = line.getString();
            float reveal = Mth.clamp((lineTotal - lineTicks) / (lineTotal * 0.55F), 0.0F, 1.0F);
            String shown = full.substring(0, Math.min(full.length(), Math.round(full.length() * reveal)));
            float fade = Math.min(1.0F, lineTicks / 10.0F);
            int alpha = Mth.clamp((int) (255 * fade), 4, 255);
            float scale = 1.5F;
            int y = h - bar - 34;

            Component name = Component.translatable(speaker);
            g.pose().pushPose();
            g.pose().scale(scale, scale, 1.0F);
            boolean hades = speaker.equals("celestial.hades");
            int nameColor = hades ? 0xFFB03A5B : 0xFFB9A6FF;
            int textColor = hades ? 0xFFD9A0FF : 0xFFFFE9A8;
            g.drawString(font, name, (int) ((w / 2.0F) / scale - font.width(name) / 2.0F), (int) ((y - 14) / scale), (alpha << 24) | (nameColor & 0xFFFFFF), true);
            g.drawString(font, shown, (int) ((w / 2.0F) / scale - font.width(full) / 2.0F), (int) (y / scale), (alpha << 24) | (textColor & 0xFFFFFF), true);
            g.pose().popPose();
        }
    }
}
