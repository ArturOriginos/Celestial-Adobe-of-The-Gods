package com.example.celestial.client;

import com.example.celestial.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

/** Мини-душа на правом плече игрока (модель и текстура: MiniSoulModel). */
public class MiniSoulLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Размер души относительно исходной модели (высота ~0,3 блока). */
    public static final float SCALE = 0.4F;

    public MiniSoulLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!player.getData(ModAttachments.MINI_SOUL.get()) || player.isInvisible()) return;

        poseStack.pushPose();
        // пространство слоя: y вниз, начало на уровне верха туловища; правое плечо x < 0
        float bob = Mth.sin(ageInTicks * 0.12F) * 0.025F;
        float shoulderY = (player.isCrouching() ? 0.2F : 0.0F) - 0.02F - bob;
        poseStack.translate(-0.34F, shoulderY, 0.0F);
        // модель: y вверх, лицом к -Z; пространство слоя: y вниз, лицом к +Z
        poseStack.scale(SCALE, -SCALE, -SCALE);
        MiniSoulModel.render(poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
