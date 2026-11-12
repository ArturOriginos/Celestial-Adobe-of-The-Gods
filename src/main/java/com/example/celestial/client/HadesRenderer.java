package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.Hades;
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

/** Аид: модель из Blockbench (1473 куба) со светящимся слоем, анимацией покоя и позами заклинаний. */
public class HadesRenderer extends EntityRenderer<Hades> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/hades.png");
    private static final ResourceLocation GLOW =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/hades_glow.png");

    private static BBModel model;
    private static BBRig rig;
    private static boolean failed;
    private static int root, armRaised, aura;

    public HadesRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.6F;
    }

    private static boolean ensure() {
        if (failed) return false;
        if (model != null) return true;
        try {
            BBModel m = BBModel.load("hades");
            BBRig r = BBRig.load("hades");
            root = r.index("hades");
            armRaised = r.index("arm_raised");
            aura = r.index("aura");
            rig = r;
            model = m;
            return true;
        } catch (RuntimeException e) {
            failed = true;
            com.mojang.logging.LogUtils.getLogger().error("Celestial: не удалось загрузить модель Аида", e);
            return false;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(Hades entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(Hades entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(Hades e, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffer, int light) {
        if (!ensure()) return;
        float age = e.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
        int stage = e.getStage();

        BBRig.Pose pose = rig.newPose();
        rig.apply(pose, "idle", age / 20.0F, 1.0F);

        float armUp = 0.0F, lift = 3.0F * Mth.sin(age * 0.1F); // парит над землёй
        switch (stage) {
            case Hades.ST_CAST -> armUp = 45.0F + 8.0F * Mth.sin(age * 0.8F);
            case Hades.ST_SLAM -> { armUp = 80.0F; lift -= 4.0F; }
            case Hades.ST_CHARGE -> { armUp = 105.0F; lift += 6.0F + 2.0F * Mth.sin(age * 0.6F); }
            case Hades.ST_ROAR -> { armUp = 100.0F; lift += 4.0F + 2.0F * Mth.sin(age * 0.3F); }
            default -> {}
        }
        pose.addRot(armRaised, armUp, 0.0F, 0.0F);
        pose.pos[root * 3 + 1] += lift;

        int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(e.hurtTime > 0 || e.deathTime > 0));

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        if (e.deathTime > 0) {
            float f = Mth.sqrt(Mth.clamp((e.deathTime + pt - 1.0F) / 20.0F * 1.6F, 0.0F, 1.6F));
            ps.mulPose(Axis.ZP.rotationDegrees(Math.min(f, 1.0F) * 90.0F));
        }
        float s = 1.0F / 16.0F;
        ps.scale(s, s, s);

        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = pass == 0
                    ? buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE))
                    : buffer.getBuffer(RenderType.eyes(GLOW));
            for (int g = 0; g < rig.groupCount(); g++) {
                ps.pushPose();
                rig.transform(ps, pose, g);
                int lt = (pass == 1 || g == aura) ? LightTexture.FULL_BRIGHT : light;
                int ov = pass == 0 && g != aura ? overlay : OverlayTexture.NO_OVERLAY;
                model.renderGroup(vc, ps.last(), lt, ov, g, BBModel.NO_CLIP);
                ps.popPose();
            }
        }
        ps.popPose();
        super.render(e, entityYaw, pt, ps, buffer, light);
    }
}
