package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.GuardianOfGods;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Хранитель Богов (Солнечный Страж): модель из Blockbench, 169 кубов в 9 частях.
 * Анимация: голова смотрит на цель, ноги и руки шагают, руки поднимаются при ударе, нимб вращается,
 * крылья дышат, орбиты кружатся вокруг босса.
 */
public class GuardianRenderer extends EntityRenderer<GuardianOfGods> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/celestial_guardian.png");
    private static final float SCALE = 1.4F;

    private static final int HEAD = 0, TORSO = 1, ARM_L = 2, ARM_R = 3, LEG_L = 4, LEG_R = 5, WINGS = 6, HALO = 7, ORBITALS = 8;
    private static BBModel model;

    public GuardianRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.4F;
    }

    @Override
    public ResourceLocation getTextureLocation(GuardianOfGods entity) {
        return TEXTURE;
    }

    @Override
    public void render(GuardianOfGods entity, float entityYaw, float partialTick, PoseStack ps,
                       MultiBufferSource buffer, int light) {
        if (model == null) model = BBModel.load("guardian");
        float age = entity.tickCount + partialTick;
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        float netHead = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -50.0F, 50.0F);
        float pitch = Mth.clamp(Mth.lerp(partialTick, entity.xRotO, entity.getXRot()), -40.0F, 40.0F);

        float walkPos = entity.walkAnimation.position(partialTick);
        float walkSpeed = Math.min(1.0F, entity.walkAnimation.speed(partialTick));
        float swing = Mth.cos(walkPos * 0.6662F) * 1.2F * walkSpeed;   // радианы
        float attack = entity.getAttackAnim(partialTick);
        float armRaise = (float) Math.toDegrees(0.0) + Mth.sin(attack * Mth.PI) * 115.0F;

        int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F),
                OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        if (entity.deathTime > 0) {
            float f = Mth.sqrt(Mth.clamp((entity.deathTime + partialTick - 1.0F) / 20.0F * 1.6F, 0.0F, 1.6F));
            ps.mulPose(Axis.ZP.rotationDegrees(Math.min(f, 1.0F) * 90.0F));
        }
        float s = SCALE / 16.0F;
        ps.scale(s, s, s);
        ps.translate(-8.0F, 12.0F, -8.0F); // центр модели (8,8,8), ступни на y=-12 -> ступни в точке сущности
        ps.translate(0.0F, 0.6F * Mth.sin(age * 0.1F), 0.0F);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (int g = HEAD; g <= ORBITALS; g++) {
            ps.pushPose();
            switch (g) {
                case HEAD -> {
                    pivot(ps, 8.0F, 19.8F, 8.0F);
                    ps.mulPose(Axis.YP.rotationDegrees(-netHead));
                    ps.mulPose(Axis.XP.rotationDegrees(-pitch));
                    unpivot(ps, 8.0F, 19.8F, 8.0F);
                }
                case ARM_L -> {
                    pivot(ps, 17.0F, 18.4F, 8.0F);
                    ps.mulPose(Axis.XP.rotationDegrees((float) Math.toDegrees(-swing * 0.6F) + armRaise));
                    ps.mulPose(Axis.ZP.rotationDegrees(-2.0F - 1.5F * Mth.sin(age * 0.09F)));
                    unpivot(ps, 17.0F, 18.4F, 8.0F);
                }
                case ARM_R -> {
                    pivot(ps, -1.0F, 18.4F, 8.0F);
                    ps.mulPose(Axis.XP.rotationDegrees((float) Math.toDegrees(swing * 0.6F) + armRaise));
                    ps.mulPose(Axis.ZP.rotationDegrees(2.0F + 1.5F * Mth.sin(age * 0.09F)));
                    unpivot(ps, -1.0F, 18.4F, 8.0F);
                }
                case LEG_L -> {
                    pivot(ps, 11.3F, -0.4F, 8.0F);
                    ps.mulPose(Axis.XP.rotationDegrees((float) Math.toDegrees(swing)));
                    unpivot(ps, 11.3F, -0.4F, 8.0F);
                }
                case LEG_R -> {
                    pivot(ps, 4.7F, -0.4F, 8.0F);
                    ps.mulPose(Axis.XP.rotationDegrees((float) Math.toDegrees(-swing)));
                    unpivot(ps, 4.7F, -0.4F, 8.0F);
                }
                case WINGS -> {
                    pivot(ps, 8.0F, 14.6F, 12.8F);
                    ps.mulPose(Axis.XP.rotationDegrees(3.0F * Mth.sin(age * 0.07F)));
                    unpivot(ps, 8.0F, 14.6F, 12.8F);
                }
                case HALO -> {
                    pivot(ps, 8.0F, 22.8F, 12.0F);
                    ps.mulPose(Axis.ZP.rotationDegrees(age * 1.5F));
                    unpivot(ps, 8.0F, 22.8F, 12.0F);
                }
                case ORBITALS -> {
                    pivot(ps, 8.0F, 16.0F, 8.0F);
                    ps.mulPose(Axis.YP.rotationDegrees(age * 3.0F));
                    unpivot(ps, 8.0F, 16.0F, 8.0F);
                }
                default -> {}
            }
            model.renderGroup(vc, ps.last(), light, overlay, g, BBModel.NO_CLIP);
            ps.popPose();
        }
        ps.popPose();
        super.render(entity, entityYaw, partialTick, ps, buffer, light);
    }

    private static void pivot(PoseStack ps, float x, float y, float z) {
        ps.translate(x, y, z);
    }

    private static void unpivot(PoseStack ps, float x, float y, float z) {
        ps.translate(-x, -y, -z);
    }
}
