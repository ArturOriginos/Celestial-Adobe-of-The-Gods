package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.CelestiaGod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Бог Селестии: модель из Blockbench (259 кубов в 14 частях). Руки и кольца управляются сущностью (стадии атак),
 * кольца, звезда, искры и нимб светятся. Смерть: нарастающая белая вспышка, дрожь и набухание светом.
 */
public class CelestiaGodRenderer extends EntityRenderer<CelestiaGod> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/celestia_god.png");
    private static final float SCALE = 1.0F;

    private static final int ROBE = 0, BODY = 1, HEAD = 2, HORN_R = 3, ARM_R = 4, HORN_L = 5, ARM_L = 6, STRANDS = 7,
            HALO_BACK = 8, RING_HEAD = 9, RING_CHEST = 10, RING_WAIST = 11, STAR = 12, SPARKLES = 13;
    private static BBModel model;

    public CelestiaGodRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.6F;
    }

    @Override
    public ResourceLocation getTextureLocation(CelestiaGod entity) {
        return TEXTURE;
    }

    @Override
    public void render(CelestiaGod entity, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffer, int light) {
        if (model == null) model = BBModel.load("celestia_god");
        float age = entity.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(pt, entity.yHeadRotO, entity.yHeadRot);
        float netHead = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -50.0F, 50.0F);
        float pitch = Mth.clamp(Mth.lerp(pt, entity.xRotO, entity.getXRot()), -40.0F, 40.0F);

        float ax = (float) Math.toDegrees(entity.getArmX(pt));
        float az = (float) Math.toDegrees(entity.getArmZ(pt));
        float rs = entity.getRingScale(pt);
        float breathe = (float) Math.toDegrees(Mth.sin(age * 0.08F) * 0.04F);
        float attack = entity.getAttackAnim(pt);
        float swing = (float) Math.toDegrees(Mth.sin(attack * Mth.PI) * 1.4F);

        float white = 0.0F, k = 1.0F, shakeX = 0.0F, shakeZ = 0.0F;
        if (entity.deathTime > 0) {
            float dAge = entity.deathTime + pt;
            float t = Mth.clamp(dAge / CelestiaGod.DEATH_TICKS, 0.0F, 1.0F);
            white = t * t;
            float shake = 0.06F * t;
            shakeX = Mth.sin(dAge * 2.7F) * shake;
            shakeZ = Mth.cos(dAge * 3.1F) * shake;
            k = 1.0F + 0.18F * t * t + 0.04F * Mth.sin(dAge * 0.9F) * t;
        }
        int overlay = OverlayTexture.pack(OverlayTexture.u(white), OverlayTexture.v(entity.hurtTime > 0));

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        ps.translate(shakeX, Mth.sin(age * 0.08F) * 0.12F + entity.getLift(pt), shakeZ);
        float s = SCALE * k / 16.0F;
        ps.scale(s, s, s);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (int g = ROBE; g <= SPARKLES; g++) {
            ps.pushPose();
            boolean glow = false;
            switch (g) {
                case HEAD -> {
                    ps.translate(0.0F, 54.0F, 0.0F);
                    ps.mulPose(Axis.YP.rotationDegrees(-netHead));
                    ps.mulPose(Axis.XP.rotationDegrees(-pitch));
                    ps.translate(0.0F, -54.0F, 0.0F);
                }
                case ARM_R -> BBAnim.rotXZ(ps, 8.0F, 45.0F, 0.0F, -ax + swing, az + breathe);
                case ARM_L -> BBAnim.rotXZ(ps, -8.0F, 45.0F, 0.0F, -ax, -az - breathe);
                case HORN_R -> BBAnim.rotZ(ps, 5.0F, 57.0F, 0.0F, 2.0F * Mth.sin(age * 0.06F));
                case HORN_L -> BBAnim.rotZ(ps, -5.0F, 57.0F, 0.0F, -2.0F * Mth.sin(age * 0.06F));
                case STRANDS -> BBAnim.rotX(ps, 0.0F, 28.0F, 0.0F, 3.0F * Mth.sin(age * 0.07F));
                case HALO_BACK -> {
                    glow = true;
                    ps.translate(0.0F, 0.6F * Mth.sin(age * 0.1F), 0.0F);
                }
                case RING_HEAD -> {
                    glow = true;
                    ps.translate(0.0F, Mth.sin(age * 0.1F), 0.0F);
                    BBAnim.rotY(ps, 0.0F, 61.0F, 0.0F, age * 2.86F);
                    BBAnim.scaleXZ(ps, 0.0F, 61.0F, 0.0F, rs);
                }
                case RING_CHEST -> {
                    glow = true;
                    ps.translate(0.0F, Mth.sin(age * 0.1F + 1.5F), 0.0F);
                    BBAnim.rotY(ps, 0.0F, 42.0F, 0.0F, -age * 4.0F);
                    BBAnim.scaleXZ(ps, 0.0F, 42.0F, 0.0F, rs);
                }
                case RING_WAIST -> {
                    glow = true;
                    BBAnim.rotY(ps, 0.0F, 24.0F, 0.0F, age * 2.3F);
                    BBAnim.scaleXZ(ps, 0.0F, 24.0F, 0.0F, rs);
                }
                case STAR -> {
                    glow = true;
                    ps.translate(0.0F, 1.5F * Mth.sin(age * 0.1F), 0.0F);
                    BBAnim.rotY(ps, 0.0F, 78.0F, 0.0F, age * 4.6F);
                }
                case SPARKLES -> {
                    glow = true;
                    ps.translate(0.0F, 3.0F * Mth.sin(age * 0.07F), 0.0F);
                    BBAnim.rotY(ps, 0.0F, 40.0F, 0.0F, age * 2.9F);
                }
                default -> {}
            }
            model.renderGroup(vc, ps.last(), glow ? LightTexture.FULL_BRIGHT : light, overlay, g, BBModel.NO_CLIP);
            ps.popPose();
        }
        ps.popPose();
        super.render(entity, entityYaw, pt, ps, buffer, light);
    }
}
