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

    @Override
    protected void scale(CelestiaGod entity, PoseStack poseStack, float partialTick) {
        float bob = Mth.sin((entity.tickCount + partialTick) * 0.08F) * 0.12F;
        poseStack.translate(0.0F, LIFT + bob + entity.getLift(partialTick), 0.0F);
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(CelestiaGod entity) {
        return TEXTURE;
    }
}
