package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.CursedBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Рисует Проклятого: 3D-модель + светящийся слой (глаза, крест, кристалл, кольца). */
public class CursedBossRenderer extends MobRenderer<CursedBoss, CursedBossModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/cursed_boss.png");
    private static final ResourceLocation GLOW =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/cursed_boss_glow.png");

    public CursedBossRenderer(EntityRendererProvider.Context context) {
        super(context, new CursedBossModel(context.bakeLayer(CursedBossModel.LAYER)), 1.0F);
        this.addLayer(new EyesLayer<CursedBoss, CursedBossModel>(this) {
            @Override
            public RenderType renderType() {
                return RenderType.eyes(GLOW);
            }
        });
    }

    @Override
    protected void scale(CursedBoss entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.85F, 1.85F, 1.85F);
    }

    @Override
    public ResourceLocation getTextureLocation(CursedBoss entity) {
        return TEXTURE;
    }
}
