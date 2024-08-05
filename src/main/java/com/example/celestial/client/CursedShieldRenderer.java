package com.example.celestial.client;

import com.example.celestial.CelestialMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Рисует щит (Проклятых или Селестии) как настоящий ванильный щит с собственной текстурой. */
public class CursedShieldRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ResourceLocation CURSED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "textures/entity/cursed_shield.png");

    private final EntityModelSet modelSet;
    private final ResourceLocation texture;
    private ShieldModel shieldModel;

    public CursedShieldRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        this(dispatcher, modelSet, CURSED_TEXTURE);
    }

    /** Один и тот же ванильный щит с разными текстурами: Щит проклятых и Защитная Руна Селестии. */
    public CursedShieldRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet, ResourceLocation texture) {
        super(dispatcher, modelSet);
        this.modelSet = modelSet;
        this.texture = texture;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (shieldModel == null) {
            shieldModel = new ShieldModel(modelSet.bakeLayer(ModelLayers.SHIELD));
        }
        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);
        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(
                buffer, RenderType.entityCutoutNoCull(texture), true, stack.hasFoil());
        shieldModel.handle().render(poseStack, consumer, packedLight, packedOverlay);
        shieldModel.plate().render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
