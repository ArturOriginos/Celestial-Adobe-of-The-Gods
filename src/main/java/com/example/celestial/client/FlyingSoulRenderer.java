package com.example.celestial.client;

import com.example.celestial.entity.FlyingSoul;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Летящая душа: та же модель, что и на плече, чуть крупнее, всегда светится и летит «лицом» по курсу. */
public class FlyingSoulRenderer extends EntityRenderer<FlyingSoul> {
    public FlyingSoulRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingSoul entity) {
        return MiniSoulModel.TEXTURE;
    }

    @Override
    public void render(FlyingSoul entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float age = entity.tickCount + partialTick;
        poseStack.translate(0.0D, Mth.sin(age * 0.3F) * 0.05D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        poseStack.scale(0.55F, 0.55F, 0.55F);
        MiniSoulModel.render(poseStack, buffer, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }
}
