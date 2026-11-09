package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.CursedBoss;
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

/** Проклятый (красный хранитель): модель из Blockbench, 293 куба в 12 частях; кольца, душа-кристалл светятся. */
public class CursedBossRenderer extends EntityRenderer<CursedBoss> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/cursed_boss.png");
    private static final float SCALE = 0.75F;

    private static final int ROBE = 0, BODY = 1, HEAD = 2, HORN_R = 3, ARM_R = 4, HORN_L = 5, ARM_L = 6,
            RING_HEAD = 7, RING_CHEST = 8, RING_WAIST = 9, SOUL = 10, RUBBLE = 11;
    private static BBModel model;

    public CursedBossRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(CursedBoss entity) {
        return TEXTURE;
    }

    @Override
    public void render(CursedBoss entity, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffer, int light) {
        if (model == null) model = BBModel.load("cursed_boss");
        float age = entity.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
        float headYaw = Mth.rotLerp(pt, entity.yHeadRotO, entity.yHeadRot);
        float netHead = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -50.0F, 50.0F);
        float pitch = Mth.clamp(Mth.lerp(pt, entity.xRotO, entity.getXRot()), -40.0F, 40.0F);
        float armBase = 20.0F + 3.0F * Mth.sin(age * 0.08F); // руки слегка подняты вперёд, как при накоплении души
        int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F),
                OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        if (entity.deathTime > 0) {
            float f = Mth.sqrt(Mth.clamp((entity.deathTime + pt - 1.0F) / 20.0F * 1.6F, 0.0F, 1.6F));
            ps.mulPose(Axis.ZP.rotationDegrees(Math.min(f, 1.0F) * 90.0F));
        }
        float s = SCALE / 16.0F;
        ps.scale(s, s, s);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (int g = ROBE; g <= RUBBLE; g++) {
            ps.pushPose();
            boolean glow = false;
            switch (g) {
                case HEAD -> {
                    ps.translate(0.0F, 53.0F, 0.0F);
                    ps.mulPose(Axis.YP.rotationDegrees(-netHead));
                    ps.mulPose(Axis.XP.rotationDegrees(-pitch));
                    ps.translate(0.0F, -53.0F, 0.0F);
                }
                case ARM_R -> BBAnim.rotXZ(ps, 12.0F, 45.0F, 0.0F, armBase, 2.0F);
                case ARM_L -> BBAnim.rotXZ(ps, -12.0F, 45.0F, 0.0F, armBase, -2.0F);
                case RING_HEAD -> {
                    glow = true;
                    ps.translate(0.0F, Mth.sin(age * 0.1F), 0.0F);
                    BBAnim.rotY(ps, 0.0F, 58.0F, 0.0F, age * 2.86F);
                }
                case RING_CHEST -> {
                    glow = true;
                    BBAnim.rotY(ps, 0.0F, 40.0F, 0.0F, -age * 4.0F);
                }
                case RING_WAIST -> {
                    glow = true;
                    ps.translate(0.0F, Mth.sin(age * 0.1F + 1.5F), 0.0F);
                    BBAnim.rotY(ps, 0.0F, 20.0F, 0.0F, age * 2.3F);
                }
                case SOUL -> {
                    glow = true;
                    ps.translate(0.0F, 1.5F * Mth.sin(age * 0.1F), 0.0F);
                    BBAnim.rotY(ps, 0.0F, 74.0F, 0.0F, age * 3.4F);
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
