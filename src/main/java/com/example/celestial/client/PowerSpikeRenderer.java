package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.GodSpike;
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
 * Шипы-удары боссов: у Бога Селестии столп света с кристаллами (celestia_power), у Проклятого
 * кровавые шипы из земли (red_spikes). Модель вырастает из земли и уходит обратно по прогрессу анимации шипа.
 */
public class PowerSpikeRenderer extends EntityRenderer<GodSpike> {
    /** Модель, текстура, масштаб и номера групп, которые рисуются полупрозрачным светом. */
    public record Kind(String name, float scale, int[] glowGroups, int groups) {}

    public static final Kind CELESTIA = new Kind("celestia_power", 0.5F, new int[]{0, 5, 6, 7, 8, 9}, 10);
    public static final Kind CURSED = new Kind("red_spikes", 0.55F, new int[]{}, 7);

    private final Kind kind;
    private final ResourceLocation texture;
    private BBModel model;

    public PowerSpikeRenderer(EntityRendererProvider.Context context, Kind kind) {
        super(context);
        this.kind = kind;
        this.texture = ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/" + kind.name() + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(GodSpike entity) {
        return texture;
    }

    @Override
    public boolean shouldRender(GodSpike entity, Frustum frustum, double camX, double camY, double camZ) {
        return true; // модель шире и выше хитбокса шипа
    }

    private boolean isGlow(int g) {
        for (int x : kind.glowGroups()) if (x == g) return true;
        return false;
    }

    @Override
    public void render(GodSpike entity, float yaw, float pt, PoseStack ps, MultiBufferSource buffer, int light) {
        float p = entity.getAnimationProgress(pt);
        if (p == 0.0F) return;
        if (model == null) model = BBModel.load(kind.name());
        float grow = p < 0.2F ? p / 0.2F : p > 0.85F ? (1.0F - p) / 0.15F : 1.0F;
        grow = Mth.clamp(grow, 0.0F, 1.0F);
        grow = grow * grow * (3.0F - 2.0F * grow);
        float age = entity.tickCount + pt;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
        float s = kind.scale() * grow / 16.0F;
        ps.scale(s, s, s);
        ps.mulPose(Axis.YP.rotationDegrees(age * 4.0F)); // кольца, звёзды и искры вращаются

        for (int pass = 0; pass < 2; pass++) {
            boolean glowPass = pass == 1;
            VertexConsumer vc = glowPass
                    ? buffer.getBuffer(RenderType.entityTranslucent(texture))
                    : buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
            for (int g = 0; g < kind.groups(); g++) {
                if (isGlow(g) != glowPass) continue;
                ps.pushPose();
                if (!glowPass) ps.mulPose(Axis.YP.rotationDegrees(-age * 4.0F)); // твёрдая часть не вращается
                model.renderGroup(vc, ps.last(), glowPass ? LightTexture.FULL_BRIGHT : light,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, g, BBModel.NO_CLIP);
                ps.popPose();
            }
        }
        ps.popPose();
        super.render(entity, yaw, pt, ps, buffer, light);
    }
}
