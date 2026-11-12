package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.HadesEffect;
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
 * Эффекты Аида: копьё пламени, колонны, извержение, шипы, цепи, череп, комета и ритуальный круг.
 * Модели и анимации (rise / spin / flicker / pulse) берутся из набора hades_boss, всё рисуется на полной яркости
 * (обычная текстура и светящаяся поверх).
 */
public class HadesEffectRenderer extends EntityRenderer<HadesEffect> {
    private static final String[] NAMES = {"hades_flame_spear", "hades_dark_pillars", "hades_eruption", "hades_crystal_spikes",
            "hades_chain_ring", "hades_ritual_circle", "hades_skull_flame", "hades_comet"};
    private static final String[] ANIMS = {"flicker", "rise", "rise", "rise", "spin", "spin", "flicker", "pulse"};

    private static final class Part {
        final BBModel model;
        final BBRig rig;
        final ResourceLocation texture;
        final ResourceLocation glow;

        Part(String name) {
            this.model = BBModel.load(name);
            this.rig = BBRig.load(name);
            this.texture = ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/" + name + ".png");
            this.glow = ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/" + name + "_glow.png");
        }
    }

    private static Part[] parts;
    private static boolean failed;

    public HadesEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private static boolean ensure() {
        if (failed) return false;
        if (parts != null) return true;
        try {
            Part[] p = new Part[NAMES.length];
            for (int i = 0; i < NAMES.length; i++) p[i] = new Part(NAMES[i]);
            parts = p;
            return true;
        } catch (RuntimeException e) {
            failed = true;
            com.mojang.logging.LogUtils.getLogger().error("Celestial: не удалось загрузить эффекты Аида", e);
            return false;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(HadesEffect entity) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/hades_flame_spear.png");
    }

    @Override
    public boolean shouldRender(HadesEffect entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(HadesEffect e, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffer, int light) {
        if (!ensure()) return;
        int kind = Mth.clamp(e.getKind(), 0, NAMES.length - 1);
        Part part = parts[kind];
        float t = e.tickCount + pt;
        float life = Math.max(1, e.getLife());
        float scale = e.getScale();
        float yaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());

        float sx = 1.0F, sy = 1.0F;
        switch (kind) {
            case HadesEffect.KIND_SPEAR -> { // копьё вылетает из земли, когда проходит задержка
                float start = e.getDelay() - 6.0F;
                if (t < start) return;
                sy = Mth.clamp((t - start) / 6.0F, 0.0F, 1.0F);
                sx = 0.5F + 0.5F * sy;
            }
            case HadesEffect.KIND_CHAINS, HadesEffect.KIND_CIRCLE -> { // появляются и гаснут плавно
                float grow = Mth.clamp(t / 10.0F, 0.0F, 1.0F) * Mth.clamp((life - t) / 10.0F, 0.0F, 1.0F);
                if (grow <= 0.0F) return;
                sx = grow;
                sy = Math.max(0.2F, grow);
            }
            default -> {}
        }

        BBRig.Pose pose = part.rig.newPose();
        part.rig.apply(pose, ANIMS[kind], t / 20.0F, 1.0F);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        if (kind == HadesEffect.KIND_COMET) {
            ps.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(pt, e.xRotO, e.getXRot())));
        } else if (kind == HadesEffect.KIND_SKULL) {
            ps.mulPose(Axis.YP.rotationDegrees(t * 12.0F)); // череп вращается
        }
        float s = scale / 16.0F;
        ps.scale(s * sx, s * sy, s * sx);

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
        ps.popPose();
    }
}
