package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.DarkOrb;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Тёмная сфера: модель dark_orb (ядро + кольца), кольца вращаются, всё светится. */
public class DarkOrbRenderer extends EntityRenderer<DarkOrb> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/dark_orb.png");
    private static final ResourceLocation GLOW =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/dark_orb_glow.png");
    private static BBModel model;

    public DarkOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(DarkOrb entity) {
        return TEXTURE;
    }

    @Override
    public void render(DarkOrb entity, float entityYaw, float partialTick, PoseStack ps, MultiBufferSource buffer,
                       int packedLight) {
        if (model == null) model = BBModel.load("dark_orb");
        float age = entity.tickCount + partialTick;
        VertexConsumer base = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(GLOW));

        ps.pushPose();
        ps.translate(0.0F, entity.getBbHeight() / 2.0F, 0.0F);
        float pulse = 0.7F + 0.04F * Mth.sin(age * 0.5F);
        float s = pulse / 16.0F;
        ps.scale(s, s, s);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(age * 3.0F));
        model.renderGroup(base, ps.last(), LightTexture.FULL_BRIGHT, 0, BBModel.NO_CLIP);
        model.renderGroup(glow, ps.last(), LightTexture.FULL_BRIGHT, 0, BBModel.NO_CLIP);
        ps.popPose();

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(age * 14.0F));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.sin(age * 0.1F) * 25.0F));
        model.renderGroup(base, ps.last(), LightTexture.FULL_BRIGHT, 1, BBModel.NO_CLIP);
        model.renderGroup(glow, ps.last(), LightTexture.FULL_BRIGHT, 1, BBModel.NO_CLIP);
        ps.popPose();

        ps.popPose();
        super.render(entity, entityYaw, partialTick, ps, buffer, packedLight);
    }
}
