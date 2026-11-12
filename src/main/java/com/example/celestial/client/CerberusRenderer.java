package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.Cerberus;
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

/**
 * Цербер: модель из Blockbench (2308 кубов, 3 головы) со светящимся слоем, ходьбой и анимацией покоя.
 * Эффекты способностей (след рывка, огненное дыхание, ударная волна) - отдельные модели из набора cerberus_pack,
 * они рисуются прямо здесь по стадии, которую сервер синхронизирует.
 */
public class CerberusRenderer extends EntityRenderer<Cerberus> {
    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/" + name + ".png");
    }

    private static final ResourceLocation TEXTURE = tex("cerberus");
    private static final ResourceLocation GLOW = tex("cerberus_glow");
    private static final float SCALE = 1.0F;

    /** Модель, её скелет и текстуры (обычная и светящаяся). */
    private static final class Part {
        final BBModel model;
        final BBRig rig;
        final ResourceLocation texture;
        final ResourceLocation glow;

        Part(String name, ResourceLocation texture, ResourceLocation glow) {
            this.model = BBModel.load(name);
            this.rig = BBRig.load(name);
            this.texture = texture;
            this.glow = glow;
        }
    }

    private static Part body, dash, fire, shock;
    private static boolean failed;
    private static int root, headC, jawC, headL, jawL, headR, jawR, legFL, legFR, legBL, legBR, embers;

    public CerberusRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 2.6F;
    }

    /** Грузит модели один раз; при ошибке пишет её в лог и больше не пытается (игра не вылетает). */
    private static boolean ensure() {
        if (failed) return false;
        if (body != null) return true;
        try {
            Part b = new Part("cerberus", TEXTURE, GLOW);
            Part d = new Part("cerberus_dash_trail", tex("cerberus_dash_trail"), tex("cerberus_dash_trail_glow"));
            Part f = new Part("cerberus_fire_breath", tex("cerberus_fire_breath"), tex("cerberus_fire_breath_glow"));
            Part s = new Part("cerberus_shockwave", tex("cerberus_shockwave"), tex("cerberus_shockwave_glow"));
            BBRig r = b.rig;
            root = r.index("cerberus");
            headC = r.index("head_center");
            jawC = r.index("head_center_jaw");
            headL = r.index("head_left");
            jawL = r.index("head_left_jaw");
            headR = r.index("head_right");
            jawR = r.index("head_right_jaw");
            legFL = r.index("leg_front_left");
            legFR = r.index("leg_front_right");
            legBL = r.index("leg_back_left");
            legBR = r.index("leg_back_right");
            embers = r.index("embers");
            dash = d;
            fire = f;
            shock = s;
            body = b;
            return true;
        } catch (RuntimeException e) {
            failed = true;
            com.mojang.logging.LogUtils.getLogger().error("Celestial: не удалось загрузить модель Цербера", e);
            return false;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(Cerberus entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(Cerberus entity, Frustum frustum, double camX, double camY, double camZ) {
        return true; // модель и эффекты длиннее хитбокса
    }

    @Override
    public void render(Cerberus e, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffer, int light) {
        if (!ensure()) return;

        float age = e.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
        float headYaw = Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot);
        float netHead = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -50.0F, 50.0F);
        float pitch = Mth.clamp(Mth.lerp(pt, e.xRotO, e.getXRot()), -30.0F, 30.0F);
        int stage = e.getStage();
        float st = e.getStageTick() + pt;
        float walkPos = e.walkAnimation.position(pt);
        float walkAmt = Math.min(1.0F, e.walkAnimation.speed(pt) * 1.6F);

        // ---- поза: покой + ходьба из модели, затем стадии способностей
        BBRig.Pose pose = body.rig.newPose();
        body.rig.apply(pose, "idle", age / 20.0F, 1.0F);
        body.rig.apply(pose, "walk", walkPos * 0.106F, walkAmt);

        float jawCenter = 0.0F, jawSide = 0.0F, headUp = 0.0F, rear = 0.0F, legFront = 0.0F, crouch = 0.0F;
        switch (stage) {
            case Cerberus.ST_BITE -> { jawCenter = jawSide = 30.0F; }
            case Cerberus.ST_DASH_WINDUP -> { jawCenter = jawSide = 20.0F; crouch = 4.0F; headUp = -6.0F; }
            case Cerberus.ST_DASH -> { jawCenter = jawSide = 12.0F; headUp = -10.0F; }
            case Cerberus.ST_FIRE -> { jawCenter = 40.0F; jawSide = 16.0F; headUp = 8.0F; }
            case Cerberus.ST_SLAM_WINDUP -> { jawCenter = jawSide = 26.0F; rear = Math.min(1.0F, st / 20.0F) * 14.0F; legFront = rear * 2.8F; }
            case Cerberus.ST_SLAM -> { jawCenter = jawSide = 30.0F; rear = -4.0F; legFront = -10.0F; }
            case Cerberus.ST_ROAR -> { jawCenter = jawSide = 36.0F; headUp = 26.0F + 4.0F * Mth.sin(age * 0.5F); }
            default -> {}
        }
        pose.addRot(headC, -pitch + headUp, -netHead, 0.0F);
        pose.addRot(headL, -pitch + headUp, -netHead + 6.0F, 0.0F);
        pose.addRot(headR, -pitch + headUp, -netHead - 6.0F, 0.0F);
        pose.addRot(jawC, -jawCenter, 0.0F, 0.0F);
        pose.addRot(jawL, -jawSide, 0.0F, 0.0F);
        pose.addRot(jawR, -jawSide, 0.0F, 0.0F);
        pose.addRot(root, rear, 0.0F, 0.0F);
        pose.addRot(legFL, legFront, 0.0F, 0.0F);
        pose.addRot(legFR, legFront, 0.0F, 0.0F);
        pose.pos[root * 3 + 1] -= crouch;

        int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F),
                OverlayTexture.v(e.hurtTime > 0 || e.deathTime > 0));

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        if (e.deathTime > 0) {
            float f = Mth.sqrt(Mth.clamp((e.deathTime + pt - 1.0F) / 20.0F * 1.6F, 0.0F, 1.6F));
            ps.mulPose(Axis.ZP.rotationDegrees(Math.min(f, 1.0F) * 90.0F));
        }
        float s = SCALE / 16.0F;
        ps.scale(s, s, s);

        // ---- тело: обычный слой и светящийся слой
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = pass == 0
                    ? buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE))
                    : buffer.getBuffer(RenderType.eyes(GLOW));
            for (int g = 0; g < body.rig.groupCount(); g++) {
                ps.pushPose();
                body.rig.transform(ps, pose, g);
                int lt = (pass == 1 || g == embers) ? LightTexture.FULL_BRIGHT : light;
                int ov = pass == 0 && g != embers ? overlay : OverlayTexture.NO_OVERLAY;
                body.model.renderGroup(vc, ps.last(), lt, ov, g, BBModel.NO_CLIP);
                ps.popPose();
            }
        }

        // ---- эффекты способностей
        if (stage == Cerberus.ST_DASH) {
            drawEffect(dash, "rush", age, ps, buffer);
        } else if (stage == Cerberus.ST_FIRE) {
            float ramp = Mth.clamp(st / 6.0F, 0.0F, 1.0F);
            ps.pushPose();
            ps.translate(0.0F, 48.0F, -42.0F); // из пасти центральной головы
            ps.scale(1.25F * ramp, 1.25F * ramp, 3.2F * ramp);
            drawEffect(fire, "flicker", age, ps, buffer);
            ps.popPose();
        } else if (stage == Cerberus.ST_SLAM) {
            float progress = Mth.clamp(st / 22.0F, 0.05F, 1.0F);
            ps.pushPose();
            ps.translate(0.0F, 1.0F, 0.0F);
            ps.scale(3.1F * progress, 1.0F, 3.1F * progress); // кольца расходятся примерно до 16 блоков
            drawEffect(shock, "spin", age, ps, buffer);
            ps.popPose();
        }
        ps.popPose();
        super.render(e, entityYaw, pt, ps, buffer, light);
    }

    /** Эффект рисуется на полной яркости: обычная текстура и светящаяся поверх. */
    private static void drawEffect(Part part, String anim, float age, PoseStack ps, MultiBufferSource buffer) {
        BBRig.Pose pose = part.rig.newPose();
        part.rig.apply(pose, anim, age / 20.0F, 1.0F);
        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = pass == 0
                    ? buffer.getBuffer(RenderType.entityCutoutNoCull(part.texture))
                    : buffer.getBuffer(RenderType.eyes(part.glow));
            for (int g = 0; g < part.rig.groupCount(); g++) {
                ps.pushPose();
                part.rig.transform(ps, pose, g);
                part.model.renderGroup(vc, ps.last(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, g, BBModel.NO_CLIP);
                ps.popPose();
            }
        }
    }
}
