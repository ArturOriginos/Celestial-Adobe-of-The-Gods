package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.AllKnowingAngel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Рисует Всезнающего Ангела из модели Blockbench (eye_boss): тело покачивается, крылья взмахивают, нимб вращается
 * (анимация idle из архива). Лазер: модель луча повторяется вдоль направления, на конце модель попадания.
 */
public class AllKnowingAngelRenderer extends EntityRenderer<AllKnowingAngel> {
    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/" + name + ".png");
    }

    private static final ResourceLocation TEXTURE = tex("eye_boss");
    private static final ResourceLocation GLOW = tex("eye_boss_glow");
    private static final ResourceLocation LASER = tex("eye_laser");
    private static final ResourceLocation LASER_GLOW = tex("eye_laser_glow");
    private static final ResourceLocation IMPACT = tex("eye_laser_impact");
    private static final ResourceLocation IMPACT_GLOW = tex("eye_laser_impact_glow");

    private static final int G_BODY = 0, G_WING_R = 1, G_WING_L = 2, G_CHAINS = 3, G_HALO = 4;

    private static BBModel bossModel;
    private static BBModel laserModel;
    private static BBModel impactModel;

    public AllKnowingAngelRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    private static void ensureModels() {
        if (bossModel == null) bossModel = BBModel.load("eye_boss");
        if (laserModel == null) laserModel = BBModel.load("eye_laser");
        if (impactModel == null) impactModel = BBModel.load("eye_laser_impact");
    }

    @Override
    public ResourceLocation getTextureLocation(AllKnowingAngel entity) {
        return TEXTURE;
    }

    /** Луч длинный и может уходить за пределы коробки босса, поэтому всегда рисуем. */
    @Override
    public boolean shouldRender(AllKnowingAngel entity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(AllKnowingAngel entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        ensureModels();
        float age = entity.tickCount + partialTick;
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());

        poseStack.pushPose();
        drawBody(entity, poseStack, buffer, packedLight, age, yaw, partialTick);
        int state = entity.getLaserState();
        if (state != 0 && entity.deathTime == 0) drawLaser(entity, poseStack, buffer, age, yaw, state);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void drawBody(AllKnowingAngel entity, PoseStack ps, MultiBufferSource buffer, int light, float age,
                          float yaw, float partialTick) {
        float seconds = age / 20.0F;
        float wave = (1.0F - Mth.cos(Mth.PI * seconds)) * 0.5F; // 0..1, период 2 секунды (как в idle)
        float death = entity.deathTime > 0 ? Math.min(1.0F, (entity.deathTime + partialTick) / 20.0F) : 0.0F;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        float s = (1.0F - death) / 16.0F;
        ps.scale(s, s, s);

        VertexConsumer base = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GLOW));
        for (int g = 0; g <= G_HALO; g++) {
            ps.pushPose();
            ps.translate(0.0F, 2.0F * wave, 0.0F);
            if (g == G_WING_R) {
                ps.translate(13.0F, 41.0F, 0.0F);
                ps.mulPose(Axis.ZP.rotationDegrees(9.0F * wave));
                ps.translate(-13.0F, -41.0F, 0.0F);
            } else if (g == G_WING_L) {
                ps.translate(-13.0F, 41.0F, 0.0F);
                ps.mulPose(Axis.ZP.rotationDegrees(-9.0F * wave));
                ps.translate(13.0F, -41.0F, 0.0F);
            } else if (g == G_HALO) {
                ps.translate(0.0F, 74.0F, 0.0F);
                ps.mulPose(Axis.YP.rotationDegrees(360.0F * (seconds / 4.0F)));
                ps.translate(0.0F, -74.0F, 0.0F);
            }
            bossModel.renderGroup(base, ps.last(), light, g, BBModel.NO_CLIP);
            bossModel.renderGroup(glow, ps.last(), LightTexture.FULL_BRIGHT, g, BBModel.NO_CLIP);
            ps.popPose();
        }
        ps.popPose();
    }

    private void drawLaser(AllKnowingAngel entity, PoseStack ps, MultiBufferSource buffer, float age, float yaw,
                           int state) {
        float lenBlocks = entity.getLaserLength();
        if (lenBlocks <= 0.1F) return;
        float lenPx = lenBlocks * 16.0F;
        double rad = Math.toRadians(yaw);
        // глаз: на блок впереди тела (перёд = (-sin, cos)) и на 2.1 блока вверх
        double ex = -Math.sin(rad), ez = Math.cos(rad);

        ps.pushPose();
        ps.translate(ex, 2.1D, ez);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getLaserYaw()));
        ps.mulPose(Axis.XP.rotationDegrees(-entity.getLaserPitch()));
        ps.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);

        float thick = state == 1
                ? Mth.lerp(entity.getLaserProgress(), 0.05F, 0.2F)
                : 0.9F + 0.1F * Mth.sin(age * 1.7F);
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(LASER_GLOW));
        VertexConsumer base = buffer.getBuffer(RenderType.eyes(LASER));
        int tiles = (int) Math.ceil(lenPx / 64.0F);
        ps.pushPose();
        ps.scale(thick, thick, 1.0F);
        for (int k = 0; k < tiles; k++) {
            ps.pushPose();
            ps.translate(0.0F, 0.0F, -64.0F * k);
            float minZ = -(lenPx - 64.0F * k);
            if (state == 2) laserModel.renderGroup(base, ps.last(), LightTexture.FULL_BRIGHT, 0, minZ);
            laserModel.renderGroup(glow, ps.last(), LightTexture.FULL_BRIGHT, 0, minZ);
            ps.popPose();
        }
        ps.popPose();

        if (state == 2) {
            ps.translate(0.0F, 0.0F, -lenPx);
            ps.mulPose(Axis.ZP.rotationDegrees(age * 18.0F));
            float k = 0.55F + 0.08F * Mth.sin(age * 2.3F);
            ps.scale(k, k, k);
            VertexConsumer ib = buffer.getBuffer(RenderType.eyes(IMPACT));
            VertexConsumer ig = buffer.getBuffer(RenderType.eyes(IMPACT_GLOW));
            for (int g = 0; g <= 1; g++) {
                impactModel.renderGroup(ib, ps.last(), LightTexture.FULL_BRIGHT, g, BBModel.NO_CLIP);
                impactModel.renderGroup(ig, ps.last(), LightTexture.FULL_BRIGHT, g, BBModel.NO_CLIP);
            }
        }
        ps.popPose();
    }
}
