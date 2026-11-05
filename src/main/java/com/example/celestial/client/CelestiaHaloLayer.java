package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;

/** Нимб над головой игрока, на котором надета хотя бы одна часть брони Селестии. */
public class CelestiaHaloLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MODID, "celestia_halo"), "main");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/celestia_halo.png");
    private static final float HEIGHT = -13.0F; // над макушкой (макушка на -8)

    private final ModelPart halo;

    public CelestiaHaloLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                             EntityModelSet models) {
        super(parent);
        this.halo = models.bakeLayer(LAYER).getChild("halo");
    }

    /** Кольцо из 12 золотых планок и 4 камней по сторонам света. */
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition halo = root.addOrReplaceChild("halo", CubeListBuilder.create(), PartPose.offset(0.0F, HEIGHT, 0.0F));
        float apothem = 6.5F;
        for (int i = 0; i < 12; i++) {
            float ph = i * (float) Math.PI / 6.0F;
            halo.addOrReplaceChild("bar" + i,
                    CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(-apothem * Mth.sin(ph), 0.0F, -apothem * Mth.cos(ph), 0.0F, ph, 0.0F));
        }
        for (int i = 0; i < 4; i++) {
            float ph = i * (float) Math.PI / 2.0F;
            halo.addOrReplaceChild("gem" + i,
                    CubeListBuilder.create().texOffs(12, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                    PartPose.offsetAndRotation(-(apothem + 0.9F) * Mth.sin(ph), 0.0F, -(apothem + 0.9F) * Mth.cos(ph),
                            0.785F, ph, 0.785F));
        }
        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible() || player.isSpectator()) return;
        boolean worn = player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CELESTIA_CHESTPLATE.get())
                || player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.CELESTIA_BOOTS.get());
        if (!worn) return;

        poseStack.pushPose();
        this.getParentModel().head.translateAndRotate(poseStack); // нимб следует за головой (и за приседанием)
        this.halo.yRot = ageInTicks * 0.06F;
        this.halo.y = HEIGHT + Mth.sin(ageInTicks * 0.08F) * 0.8F;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.halo.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
