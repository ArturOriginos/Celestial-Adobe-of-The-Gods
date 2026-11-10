package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.Fenrir;
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
 * Фенрир: модель из Blockbench (2557 кубов в 10 частях). Ходьба, челюсть и хвост повторяют анимации walk/idle
 * из модели; светящиеся кубы (глаза, руны, угли) рисуются вторым проходом полной яркостью.
 */
public class FenrirRenderer extends EntityRenderer<Fenrir> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/fenrir.png");
    private static final ResourceLocation GLOW =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/fenrir_glow.png");
    private static final float SCALE = 1.0F;

    private static final int BODY = 0, CHAINS = 1, HEAD = 2, JAW = 3, LEG_FL = 4, LEG_FR = 5, LEG_BL = 6, LEG_BR = 7,
            TAIL = 8, EMBERS = 9;
    private static BBModel model;
    private static BBModel glowModel;

    public FenrirRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 2.4F;
    }

    @Override
    public ResourceLocation getTextureLocation(Fenrir entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(Fenrir entity, Frustum frustum, double camX, double camY, double camZ) {
        return true; // модель длиннее хитбокса
    }

    @Override
    public void render(Fenrir e, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffer, int light) {
        if (model == null) model = BBModel.load("fenrir");
        if (glowModel == null) glowModel = BBModel.load("fenrir_glow");

        float age = e.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
        float headYaw = Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot);
        float netHead = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -60.0F, 60.0F);
        float pitch = Mth.clamp(Mth.lerp(pt, e.xRotO, e.getXRot()), -35.0F, 35.0F);
        int stage = e.getStage();
        float walkPos = e.walkAnimation.position(pt);
        float walkAmt = Math.min(1.0F, e.walkAnimation.speed(pt) * 1.6F);
        float seconds = age / 20.0F;

        // челюсть: в покое медленно открывается (idle), при укусе, рыке и прыжке - шире
        float jaw = 12.0F * (0.5F - 0.5F * Mth.cos(Mth.PI * seconds));
        float headUp = 0.0F;
        float crouch = 0.0F;
        switch (stage) {
            case Fenrir.ST_BITE -> jaw = 30.0F;
            case Fenrir.ST_CROUCH -> { jaw = 18.0F; crouch = 6.0F; headUp = -8.0F; }
            case Fenrir.ST_LEAP -> jaw = 34.0F;
            case Fenrir.ST_SPIKES -> { jaw = 22.0F; headUp = -6.0F; }
            case Fenrir.ST_HOWL, Fenrir.ST_TRANSITION -> { jaw = 32.0F; headUp = 34.0F; }
            default -> {}
        }
        float legSwing = Mth.cos(walkPos * 0.5F) * 25.0F * walkAmt;
        float legFront = 0.0F, legBack = 0.0F;
        if (stage == Fenrir.ST_LEAP) { legFront = 38.0F; legBack = -38.0F; legSwing = 0.0F; }
        float tailYaw = (walkAmt > 0.1F ? 8.0F : 10.0F) * Mth.sin(Mth.PI * seconds * (walkAmt > 0.1F ? 2.0F : 1.0F));
        float bob = Math.abs(Mth.sin(walkPos * 0.5F)) * walkAmt;
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
        ps.translate(0.0F, bob - crouch, 0.0F);

        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = pass == 0
                    ? buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE))
                    : buffer.getBuffer(RenderType.eyes(GLOW));
            BBModel m = pass == 0 ? model : glowModel;
            int lt = pass == 0 ? light : LightTexture.FULL_BRIGHT;
            int ov = pass == 0 ? overlay : OverlayTexture.NO_OVERLAY;
            for (int g = BODY; g <= EMBERS; g++) {
                ps.pushPose();
                switch (g) {
                    case HEAD, JAW -> {
                        ps.translate(0.0F, 56.0F, -33.0F);
                        ps.mulPose(Axis.YP.rotationDegrees(-netHead));
                        ps.mulPose(Axis.XP.rotationDegrees(-pitch + headUp));
                        ps.translate(0.0F, -56.0F, 33.0F);
                        if (g == JAW) BBAnim.rotX(ps, 0.0F, 56.0F, -37.0F, -jaw);
                    }
                    case LEG_FL -> BBAnim.rotX(ps, -12.0F, 30.0F, -12.0F, legSwing + legFront);
                    case LEG_FR -> BBAnim.rotX(ps, 12.0F, 30.0F, -12.0F, -legSwing + legFront);
                    case LEG_BL -> BBAnim.rotX(ps, -10.0F, 30.0F, 18.0F, -legSwing + legBack);
                    case LEG_BR -> BBAnim.rotX(ps, 10.0F, 30.0F, 18.0F, legSwing + legBack);
                    case TAIL -> BBAnim.rotY(ps, 0.0F, 40.0F, 28.0F, tailYaw);
                    case EMBERS -> ps.translate(0.0F, 1.5F * Mth.sin(age * 0.12F), 0.0F);
                    default -> {}
                }
                m.renderGroup(vc, ps.last(), lt, ov, g, BBModel.NO_CLIP);
                ps.popPose();
            }
        }
        ps.popPose();
        super.render(e, entityYaw, pt, ps, buffer, light);
    }
}
