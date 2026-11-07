package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.CelestiaGod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Рисует Бога Селестии: 3D-модель, светящийся слой (золото, кольца, звезда, кристаллы) и паксель в руке. */
public class CelestiaGodRenderer extends HumanoidMobRenderer<CelestiaGod, CelestiaGodModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/celestia_god.png");
    private static final ResourceLocation GLOW =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/celestia_god_glow.png");

    private static final float SCALE = 1.6F;
    /** Подъём модели: кончик мантии (45 ед. ниже оси) парит чуть выше земли, а не уходит под неё. */
    private static final float LIFT = (45.0F - 24.0F) * SCALE / 16.0F + 0.3F;

    public CelestiaGodRenderer(EntityRendererProvider.Context context) {
        super(context, new CelestiaGodModel(context.bakeLayer(CelestiaGodModel.LAYER)), 1.6F);
        this.addLayer(new EyesLayer<CelestiaGod, CelestiaGodModel>(this) {
            @Override
            public RenderType renderType() {
                return RenderType.eyes(GLOW);
            }
        });
    }

    /** Не заваливаться на бок при смерти: вместо этого работает собственная анимация (см. CelestiaGod#tickDeath). */
    @Override
    protected float getFlipDegrees(CelestiaGod entity) {
        return 0.0F;
    }

    /** Нарастающая белая вспышка тела во время смерти. */
    @Override
    protected float getWhiteOverlayProgress(CelestiaGod entity, float partialTick) {
        if (entity.deathTime <= 0) return 0.0F;
        float t = Mth.clamp((entity.deathTime + partialTick) / CelestiaGod.DEATH_TICKS, 0.0F, 1.0F);
        return t * t;
    }

    @Override
    protected void scale(CelestiaGod entity, PoseStack poseStack, float partialTick) {
        float bob = Mth.sin((entity.tickCount + partialTick) * 0.08F) * 0.12F;
        // scale() вызывается уже после poseStack.scale(-1, -1, 1), поэтому ось Y здесь перевёрнута:
        // чтобы поднять модель вверх, сдвиг нужен со знаком минус (иначе босс «проваливается» в землю).
        poseStack.translate(0.0F, -(LIFT + bob + entity.getLift(partialTick)), 0.0F);
        float k = 1.0F;
        if (entity.deathTime > 0) {
            float age = entity.deathTime + partialTick;
            float t = Mth.clamp(age / CelestiaGod.DEATH_TICKS, 0.0F, 1.0F);
            float shake = 0.06F * t;
            poseStack.translate(Mth.sin(age * 2.7F) * shake, 0.0F, Mth.cos(age * 3.1F) * shake);
            k = 1.0F + 0.18F * t * t + 0.04F * Mth.sin(age * 0.9F) * t;   // набухает светом перед взрывом
        }
        poseStack.scale(SCALE * k, SCALE * k, SCALE * k);
    }

    @Override
    public ResourceLocation getTextureLocation(CelestiaGod entity) {
        return TEXTURE;
    }
}
