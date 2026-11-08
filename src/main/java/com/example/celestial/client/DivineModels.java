package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Модели брони Бога и ауры. Сгенерировано из Blockbench-моделей (tools/gen_divine_models.py):
 * divine_chestplate.bbmodel, divine_boots.bbmodel, divine_aura.bbmodel. Ручные правки лучше вносить в .bbmodel и перегенерировать.
 */
public final class DivineModels {
    public static final ModelLayerLocation CHEST_LAYER = layer("divine_chestplate");
    public static final ModelLayerLocation BOOTS_LAYER = layer("divine_boots");
    public static final ModelLayerLocation AURA_LAYER = layer("divine_aura");

    private DivineModels() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MODID, name), "main");
    }

    /** Нагрудник: тело, руки (рукава, наплечники, рога, перья). Текстура 128x128. */
    public static LayerDefinition createChestLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition c_body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.9F)).texOffs(56, 24).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 1.0F, 4.0F, new CubeDeformation(1.15F)).texOffs(76, 30).addBox(-3.0F, 10.0F, -2.0F, 6.0F, 3.0F, 1.0F, new CubeDeformation(1.0F)).texOffs(90, 30).addBox(-2.0F, 12.0F, -3.0F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.3F)).texOffs(100, 30).addBox(-1.0F, 14.0F, -3.2F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.2F)).texOffs(74, 16).addBox(-2.0F, 2.0F, -2.4F, 4.0F, 5.0F, 1.0F, new CubeDeformation(0.9F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition c_right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(1.0F)).texOffs(88, 0).addBox(-3.0F, -4.0F, -3.0F, 5.0F, 2.0F, 6.0F, new CubeDeformation(0.6F)).texOffs(80, 24).addBox(-4.0F, -4.0F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.3F)).texOffs(86, 24).addBox(-4.0F, 5.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.1F)), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition c_horn_r1 = c_right_arm.addOrReplaceChild("horn_r1", CubeListBuilder.create().texOffs(22, 16).addBox(-1.5F, -5.0F, -1.0F, 3.0F, 5.0F, 2.0F).texOffs(90, 24).addBox(-0.5F, -9.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-2.5F, -4.5F, 0.0F, 0.0F, 0.0F, -0.5F));
        PartDefinition c_horn_r2 = c_right_arm.addOrReplaceChild("horn_r2", CubeListBuilder.create().texOffs(32, 16).addBox(-1.0F, -4.0F, -1.5F, 2.0F, 4.0F, 3.0F).texOffs(106, 30).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-3.4F, -3.5F, 0.0F, 0.0F, 0.0F, -1.15F));
        PartDefinition c_feather_r1 = c_right_arm.addOrReplaceChild("feather_r1", CubeListBuilder.create().texOffs(94, 24).addBox(-6.0F, -1.0F, -1.5F, 6.0F, 2.0F, 3.0F).texOffs(20, 35).addBox(-8.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-3.6F, -1.0F, 0.0F, 0.0F, 0.0F, -0.2F));
        PartDefinition c_feather_r2 = c_right_arm.addOrReplaceChild("feather_r2", CubeListBuilder.create().texOffs(112, 24).addBox(-5.0F, -1.0F, -1.5F, 5.0F, 2.0F, 3.0F).texOffs(28, 35).addBox(-7.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-3.6F, 1.0F, 0.0F, 0.0F, 0.0F, -0.6F));
        PartDefinition c_feather_r3 = c_right_arm.addOrReplaceChild("feather_r3", CubeListBuilder.create().texOffs(110, 30).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 2.0F).texOffs(36, 35).addBox(-6.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-3.6F, 3.0F, 0.0F, 0.0F, 0.0F, -1.0F));
        PartDefinition c_left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(1.0F)).texOffs(0, 16).addBox(-2.0F, -4.0F, -3.0F, 5.0F, 2.0F, 6.0F, new CubeDeformation(0.6F)).texOffs(0, 30).addBox(3.0F, -4.0F, -1.0F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.3F)).texOffs(6, 30).addBox(3.0F, 5.0F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.1F)), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition c_horn_l1 = c_left_arm.addOrReplaceChild("horn_l1", CubeListBuilder.create().texOffs(42, 16).addBox(-1.5F, -5.0F, -1.0F, 3.0F, 5.0F, 2.0F).texOffs(10, 30).addBox(-0.5F, -9.0F, -0.5F, 1.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(2.5F, -4.5F, 0.0F, 0.0F, 0.0F, 0.5F));
        PartDefinition c_horn_l2 = c_left_arm.addOrReplaceChild("horn_l2", CubeListBuilder.create().texOffs(52, 16).addBox(-1.0F, -4.0F, -1.5F, 2.0F, 4.0F, 3.0F).texOffs(122, 30).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(3.4F, -3.5F, 0.0F, 0.0F, 0.0F, 1.15F));
        PartDefinition c_feather_l1 = c_left_arm.addOrReplaceChild("feather_l1", CubeListBuilder.create().texOffs(14, 30).addBox(0.0F, -1.0F, -1.5F, 6.0F, 2.0F, 3.0F).texOffs(44, 35).addBox(6.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(3.6F, -1.0F, 0.0F, 0.0F, 0.0F, 0.2F));
        PartDefinition c_feather_l2 = c_left_arm.addOrReplaceChild("feather_l2", CubeListBuilder.create().texOffs(32, 30).addBox(0.0F, -1.0F, -1.5F, 5.0F, 2.0F, 3.0F).texOffs(52, 35).addBox(5.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(3.6F, 1.0F, 0.0F, 0.0F, 0.0F, 0.6F));
        PartDefinition c_feather_l3 = c_left_arm.addOrReplaceChild("feather_l3", CubeListBuilder.create().texOffs(0, 35).addBox(0.0F, -1.0F, -1.0F, 4.0F, 2.0F, 2.0F).texOffs(60, 35).addBox(4.0F, -0.5F, -1.0F, 2.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(3.6F, 3.0F, 0.0F, 0.0F, 0.0F, 1.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Ботинки: ноги (голенище, носок, ремешок) и крылья на лодыжках. Текстура 128x128. */
    public static LayerDefinition createBootsLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition b_right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(84, 16).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(1.2F)).texOffs(56, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.8F)).texOffs(100, 16).addBox(-1.5F, 6.5F, -2.2F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.8F)).texOffs(48, 30).addBox(-2.0F, 10.0F, -4.0F, 4.0F, 2.0F, 3.0F, new CubeDeformation(0.7F)).texOffs(68, 35).addBox(-2.0F, 9.0F, -3.0F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.95F)).texOffs(110, 16).addBox(-2.0F, 11.0F, -3.0F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.5F)), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition b_wing_r1 = b_right_leg.addOrReplaceChild("wing_r1", CubeListBuilder.create().texOffs(62, 16).addBox(-0.5F, -5.0F, -1.0F, 1.0F, 5.0F, 2.0F).texOffs(12, 35).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 9.0F, 1.5F, -0.6F, 0.0F, 0.0F));
        PartDefinition b_wing_r2 = b_right_leg.addOrReplaceChild("wing_r2", CubeListBuilder.create().texOffs(0, 24).addBox(-0.5F, -4.0F, -1.0F, 1.0F, 4.0F, 2.0F).texOffs(80, 35).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-3.1F, 7.5F, 0.5F, -0.15F, 0.0F, -0.7F));
        PartDefinition b_left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(6, 24).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(1.3F)).texOffs(72, 0).addBox(-2.0F, 6.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.9F)).texOffs(22, 24).addBox(-1.5F, 6.5F, -2.2F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.9F)).texOffs(62, 30).addBox(-2.0F, 10.0F, -4.0F, 4.0F, 2.0F, 3.0F, new CubeDeformation(0.8F)).texOffs(84, 35).addBox(-2.0F, 9.0F, -3.0F, 4.0F, 1.0F, 2.0F, new CubeDeformation(1.05F)).texOffs(32, 24).addBox(-2.0F, 11.0F, -3.0F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.6F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        PartDefinition b_wing_l1 = b_left_leg.addOrReplaceChild("wing_l1", CubeListBuilder.create().texOffs(68, 16).addBox(-0.5F, -5.0F, -1.0F, 1.0F, 5.0F, 2.0F).texOffs(16, 35).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(3.0F, 9.0F, 1.5F, -0.6F, 0.0F, 0.0F));
        PartDefinition b_wing_l2 = b_left_leg.addOrReplaceChild("wing_l2", CubeListBuilder.create().texOffs(50, 24).addBox(-0.5F, -4.0F, -1.0F, 1.0F, 4.0F, 2.0F).texOffs(96, 35).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(3.1F, 7.5F, 0.5F, -0.15F, 0.0F, 0.7F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Аура: нимб со шпилем над головой и два вращающихся кольца вокруг тела. Текстура 64x64. */
    public static LayerDefinition createAuraLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition a_halo_root = root.addOrReplaceChild("halo_root", CubeListBuilder.create(), PartPose.offset(0.0F, -12.0F, 0.0F));
        PartDefinition a_halo = a_halo_root.addOrReplaceChild("halo", CubeListBuilder.create().texOffs(0, 6).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg1", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.3927F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg2", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg3", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.1781F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg4", CubeListBuilder.create().texOffs(0, 6).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg5", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.9635F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg6", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.3562F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg7", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.7489F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg8", CubeListBuilder.create().texOffs(0, 6).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.1416F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg9", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.5343F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg10", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.927F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg11", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.3197F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg12", CubeListBuilder.create().texOffs(0, 6).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.7124F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg13", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.1051F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg14", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.4978F, 0.0F));
        a_halo.addOrReplaceChild("halo_seg15", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.8905F, 0.0F));
        PartDefinition a_spire = a_halo_root.addOrReplaceChild("spire", CubeListBuilder.create().texOffs(16, 0).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 1.0F).texOffs(16, 12).addBox(-3.0F, -6.0F, -0.5F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.08F)), PartPose.offset(0.0F, -4.0F, 0.0F));
        PartDefinition a_ring_a_precess = root.addOrReplaceChild("ring_a_precess", CubeListBuilder.create(), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition a_ring_a_tilt = a_ring_a_precess.addOrReplaceChild("ring_a_tilt", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.1868F, 0.0F, 0.0F));
        PartDefinition a_ring_a_spin = a_ring_a_tilt.addOrReplaceChild("ring_a_spin", CubeListBuilder.create().texOffs(0, 6).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg1", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.1963F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg2", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.3927F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg3", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.589F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg4", CubeListBuilder.create().texOffs(0, 6).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg5", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.9817F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg6", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.1781F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg7", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.3744F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg8", CubeListBuilder.create().texOffs(0, 6).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg9", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.7671F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg10", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.9635F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg11", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.1598F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg12", CubeListBuilder.create().texOffs(0, 6).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.3562F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg13", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.5525F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg14", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.7489F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg15", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.9452F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg16", CubeListBuilder.create().texOffs(0, 6).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.1416F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg17", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.3379F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg18", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.5343F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg19", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.7306F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg20", CubeListBuilder.create().texOffs(0, 6).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.927F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg21", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.1233F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg22", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.3197F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg23", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.516F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg24", CubeListBuilder.create().texOffs(0, 6).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.7124F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg25", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.9087F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg26", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.1051F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg27", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.3014F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg28", CubeListBuilder.create().texOffs(0, 6).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.4978F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg29", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.6941F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg30", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.8905F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_seg31", CubeListBuilder.create().texOffs(0, 0).addBox(-16.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -6.0868F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_gem0", CubeListBuilder.create().texOffs(0, 12).addBox(-16.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.0982F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_gem1", CubeListBuilder.create().texOffs(0, 12).addBox(-16.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.669F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_gem2", CubeListBuilder.create().texOffs(0, 12).addBox(-16.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.2398F, 0.0F));
        a_ring_a_spin.addOrReplaceChild("ring_a_gem3", CubeListBuilder.create().texOffs(0, 12).addBox(-16.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.8106F, 0.0F));
        PartDefinition a_ring_b_precess = root.addOrReplaceChild("ring_b_precess", CubeListBuilder.create(), PartPose.offset(0.0F, 8.0F, 0.0F));
        PartDefinition a_ring_b_tilt = a_ring_b_precess.addOrReplaceChild("ring_b_tilt", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.1868F));
        PartDefinition a_ring_b_spin = a_ring_b_tilt.addOrReplaceChild("ring_b_spin", CubeListBuilder.create().texOffs(0, 6).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg1", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2244F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg2", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.4488F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg3", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.6732F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg4", CubeListBuilder.create().texOffs(0, 6).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.8976F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg5", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.122F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg6", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.3464F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg7", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg8", CubeListBuilder.create().texOffs(0, 6).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.7952F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg9", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.0196F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg10", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.244F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg11", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.4684F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg12", CubeListBuilder.create().texOffs(0, 6).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.6928F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg13", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.9172F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg14", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.1416F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg15", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.366F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg16", CubeListBuilder.create().texOffs(0, 6).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.5904F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg17", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.8148F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg18", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.0392F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg19", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.2636F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg20", CubeListBuilder.create().texOffs(0, 6).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.488F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg21", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.7124F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg22", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.9368F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg23", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.1612F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg24", CubeListBuilder.create().texOffs(0, 6).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.3856F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg25", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.61F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg26", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -5.8344F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_seg27", CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.1F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -6.0588F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_gem0", CubeListBuilder.create().texOffs(0, 12).addBox(-14.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.1122F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_gem1", CubeListBuilder.create().texOffs(0, 12).addBox(-14.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.683F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_gem2", CubeListBuilder.create().texOffs(0, 12).addBox(-14.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -3.2538F, 0.0F));
        a_ring_b_spin.addOrReplaceChild("ring_b_gem3", CubeListBuilder.create().texOffs(0, 12).addBox(-14.0F, -1.5F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -4.8246F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
