package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

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
import net.minecraft.world.entity.boss.wither.WitherBoss;

/**
 * Визер с моделью «Багровый трёхглавый»: три головы следят за целями, анимации idle и death берутся из модели.
 * Вспышка при появлении, рост из маленького и «броня» на половине здоровья сохранены, как у ванильного Визера.
 */
public class CrimsonWitherRenderer extends EntityRenderer<WitherBoss> {
    private static final String NAME = "crimson_three_head_boss";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/" + NAME + ".png");
    private static final ResourceLocation ARMOR =
            ResourceLocation.withDefaultNamespace("textures/entity/wither/wither_armor.png");
    private static final float SCALE = 1.0F;

    private static BBModel model;
    private static BBRig rig;
    private static int head, headL, headR, embers;

    public CrimsonWitherRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.0F;
    }

    private static boolean failed;

    /** Грузит модель один раз; при ошибке пишет её в лог и больше не пытается, чтобы не ронять игру каждый кадр. */
    private static boolean ensure() {
        if (failed) return false;
        if (model != null) return true;
        try {
            BBModel m = BBModel.load(NAME);
            BBRig r = BBRig.load(NAME);
            head = r.index("head_center");
            headL = r.index("head_left");
            headR = r.index("head_right");
            embers = r.index("embers");
            model = m;
            rig = r;
            return true;
        } catch (RuntimeException e) {
            failed = true;
            com.mojang.logging.LogUtils.getLogger().error("Celestial: не удалось загрузить модель Визера {}", NAME, e);
            return false;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(WitherBoss entity) {
        return TEXTURE;
    }

    @Override
    public void render(WitherBoss w, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffer, int light) {
        if (!ensure()) {
            super.render(w, entityYaw, pt, ps, buffer, light);
            return;
        }
        float age = w.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, w.yBodyRotO, w.yBodyRot);
        float headYaw = Mth.rotLerp(pt, w.yHeadRotO, w.yHeadRot);

        BBRig.Pose pose = rig.newPose();
        if (w.deathTime > 0) {
            rig.apply(pose, "death", Mth.clamp((w.deathTime + pt) / 20.0F, 0.0F, 1.0F) * rig.length("death"), 1.0F);
        } else {
            rig.apply(pose, "idle", age / 20.0F, 1.0F);
            // средняя голова смотрит куда смотрит Визер, боковые - на свои цели
            float netYaw = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -70.0F, 70.0F);
            float pitch = Mth.clamp(Mth.lerp(pt, w.xRotO, w.getXRot()), -45.0F, 45.0F);
            pose.addRot(head, -pitch, -netYaw, 0.0F);
            // у WitherBoss только две боковые головы: индексы 0 и 1 (индекс 2 вызывал ArrayIndexOutOfBounds и вылет игры)
            pose.addRot(headR, -Mth.clamp(w.getHeadXRot(0), -45.0F, 45.0F),
                    -Mth.clamp(Mth.wrapDegrees(w.getHeadYRot(0) - bodyYaw), -70.0F, 70.0F), 0.0F);
            pose.addRot(headL, -Mth.clamp(w.getHeadXRot(1), -45.0F, 45.0F),
                    -Mth.clamp(Mth.wrapDegrees(w.getHeadYRot(1) - bodyYaw), -70.0F, 70.0F), 0.0F);
        }

        // при появлении Визер растёт и мигает (как ванильный)
        int inv = w.getInvulnerableTicks();
        float grow = 1.0F;
        if (inv > 0) grow -= ((float) inv - pt) / 220.0F * 0.5F;
        boolean flash = inv > 0 && !(inv <= 80 && inv / 5 % 2 == 1);
        int overlay = OverlayTexture.pack(OverlayTexture.u(flash ? 1.0F : 0.0F),
                OverlayTexture.v(w.hurtTime > 0 || w.deathTime > 0));

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        float s = SCALE * grow / 16.0F;
        ps.scale(s, s, s);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (int g = 0; g < rig.groupCount(); g++) {
            ps.pushPose();
            rig.transform(ps, pose, g);
            model.renderGroup(vc, ps.last(), g == embers ? LightTexture.FULL_BRIGHT : light, overlay, g, BBModel.NO_CLIP);
            ps.popPose();
        }

        if (w.isPowered()) { // вторая стадия: вокруг Визера мерцает энергетическая «броня»
            VertexConsumer pvc = buffer.getBuffer(RenderType.energySwirl(ARMOR, Mth.cos(age * 0.02F) * 3.0F, age * 0.01F));
            ps.pushPose();
            ps.translate(0.0F, 30.0F, 0.0F);
            ps.scale(1.07F, 1.07F, 1.07F);
            ps.translate(0.0F, -30.0F, 0.0F);
            for (int g = 0; g < rig.groupCount(); g++) {
                ps.pushPose();
                rig.transform(ps, pose, g);
                model.renderGroup(pvc, ps.last(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, g, BBModel.NO_CLIP);
                ps.popPose();
            }
            ps.popPose();
        }
        ps.popPose();
        super.render(w, entityYaw, pt, ps, buffer, light);
    }
}
