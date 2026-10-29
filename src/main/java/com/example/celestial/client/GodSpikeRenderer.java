package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.GodSpike;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EvokerFangsModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Рисует шип моделью клыков вызывателя с золотой текстурой (увеличенной). */
public class GodSpikeRenderer extends EntityRenderer<GodSpike> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/god_spike.png");
    private final EvokerFangsModel<GodSpike> model;

    public GodSpikeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new EvokerFangsModel<>(context.bakeLayer(ModelLayers.EVOKER_FANGS));
    }

    @Override
    public void render(GodSpike entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        float progress = entity.getAnimationProgress(partialTicks);
        if (progress != 0.0F) {
            float size = 3.0F;
            if (progress > 0.9F) size *= (1.0F - progress) / 0.1F;
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F - entity.getYRot()));
            poseStack.scale(-size, -size, size);
            poseStack.translate(0.0F, -0.626F, 0.0F);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            this.model.setupAnim(entity, progress, 0.0F, 0.0F, entity.getYRot(), 0.0F);
            VertexConsumer consumer = buffer.getBuffer(this.model.renderType(TEXTURE));
            this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(GodSpike entity) {
        return TEXTURE;
    }
}
