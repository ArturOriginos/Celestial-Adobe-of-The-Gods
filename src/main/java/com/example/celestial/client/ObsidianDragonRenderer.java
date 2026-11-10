package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/**
 * Эндер-дракон с моделью «Обсидиановый дракон». Положение и наклон берутся из истории полёта дракона (как у
 * ванильного), взмахи крыльев привязаны к его flapTime, сидя на портале он складывает крылья, при смерти
 * играет анимация death. Лучи от кристаллов исцеления рисуются ванильным кодом.
 */
public class ObsidianDragonRenderer extends EntityRenderer<EnderDragon> {
    private static final String NAME = "obsidian_dragon";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/" + NAME + ".png");
    /** 1.6: размах крыльев около 13 блоков и длина около 17, как у ванильного дракона. */
    private static final float SCALE = 1.6F;

    private static BBModel model;
    private static BBRig rig;

    public ObsidianDragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(EnderDragon entity) {
        return TEXTURE;
    }

    @Override
    public void render(EnderDragon dragon, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffer, int light) {
        if (model == null) {
            model = BBModel.load(NAME);
            rig = BBRig.load(NAME);
        }
        float age = dragon.tickCount + pt;

        BBRig.Pose pose = rig.newPose();
        if (dragon.dragonDeathTime > 0) {
            rig.apply(pose, "death", Math.min(rig.length("death"), (dragon.dragonDeathTime + pt) / 20.0F), 1.0F);
        } else if (dragon.getPhaseManager().getCurrentPhase().isSitting()) {
            rig.apply(pose, "idle", age / 20.0F, 1.0F);
        } else {
            float flap = Mth.lerp(pt, dragon.oFlapTime, dragon.flapTime);
            rig.apply(pose, "fly", ((flap % 1.0F) + 1.0F) % 1.0F * rig.length("fly"), 1.0F);
        }
        int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(dragon.hurtTime > 0));

        ps.pushPose();
        float yaw = (float) dragon.getLatencyPos(7, pt)[0];
        float climb = (float) (dragon.getLatencyPos(5, pt)[1] - dragon.getLatencyPos(10, pt)[1]);
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        ps.mulPose(Axis.XP.rotationDegrees(climb * 10.0F));
        ps.translate(0.0F, 0.0F, 1.0F);
        float s = SCALE / 16.0F;
        ps.scale(s, s, s);
        ps.translate(0.0F, -21.0F, 0.0F); // тело модели (y = 24 пикс.) около уровня сущности

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (int g = 0; g < rig.groupCount(); g++) {
            ps.pushPose();
            rig.transform(ps, pose, g);
            model.renderGroup(vc, ps.last(), light, overlay, g, BBModel.NO_CLIP);
            ps.popPose();
        }
        ps.popPose();

        if (dragon.nearestCrystal != null) { // луч от кристалла исцеления
            ps.pushPose();
            float dx = (float) (dragon.nearestCrystal.getX() - Mth.lerp(pt, dragon.xo, dragon.getX()));
            float dy = (float) (dragon.nearestCrystal.getY() - Mth.lerp(pt, dragon.yo, dragon.getY()));
            float dz = (float) (dragon.nearestCrystal.getZ() - Mth.lerp(pt, dragon.zo, dragon.getZ()));
            EnderDragonRenderer.renderCrystalBeams(dx, dy + VoidCrystalRenderer.bob(dragon.nearestCrystal.time + pt), dz,
                    pt, dragon.tickCount, ps, buffer, light);
            ps.popPose();
        }
        super.render(dragon, entityYaw, pt, ps, buffer, light);
    }
}
