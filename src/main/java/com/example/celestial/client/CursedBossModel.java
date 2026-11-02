package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.CursedBoss;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 3D-модель Проклятого: гуманоид с рогами, наплечниками, мантией, парящим кристаллом души над головой
 * и двумя вращающимися красными кольцами вокруг тела. Раскладка текстуры 64x64 (файл 128x128).
 */
public class CursedBossModel extends HumanoidModel<CursedBoss> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MODID, "cursed_boss"), "main");

    private final ModelPart crystal;
    private final ModelPart ring1;
    private final ModelPart ring2;

    public CursedBossModel(ModelPart root) {
        super(root);
        this.crystal = this.head.getChild("crystal");
        this.ring1 = this.body.getChild("ring1");
        this.ring2 = this.body.getChild("ring2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("horn_left", CubeListBuilder.create().texOffs(0, 32).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(3.5F, -7.0F, 0.0F, 0.0F, 0.0F, 0.55F));
        head.addOrReplaceChild("horn_right", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(-3.5F, -7.0F, 0.0F, 0.0F, 0.0F, -0.55F));
        head.addOrReplaceChild("crystal", CubeListBuilder.create().texOffs(8, 32).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, -18.0F, 0.0F));

        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("robe", CubeListBuilder.create().texOffs(0, 44).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 10.0F, 6.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        addRing(body, "ring1", 4.0F);
        addRing(body, "ring2", 15.0F);

        root.getChild("right_arm").addOrReplaceChild("pad_right",
                CubeListBuilder.create().texOffs(24, 32).addBox(-4.5F, -4.0F, -3.5F, 6.0F, 3.0F, 7.0F), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("pad_left",
                CubeListBuilder.create().texOffs(24, 32).mirror().addBox(-1.5F, -4.0F, -3.5F, 6.0F, 3.0F, 7.0F), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Кольцо из четырёх тонких планок 20x1x1 (две повёрнуты на 90 градусов). */
    private static void addRing(PartDefinition body, String name, float y) {
        PartDefinition ring = body.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(0.0F, y, 0.0F));
        CubeListBuilder bar = CubeListBuilder.create().texOffs(0, 60).addBox(-10.0F, 0.0F, -0.5F, 20.0F, 1.0F, 1.0F);
        ring.addOrReplaceChild("n", bar, PartPose.offset(0.0F, 0.0F, -10.0F));
        ring.addOrReplaceChild("s", bar, PartPose.offset(0.0F, 0.0F, 10.0F));
        ring.addOrReplaceChild("w", bar, PartPose.offsetAndRotation(-10.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 2.0), 0.0F));
        ring.addOrReplaceChild("e", bar, PartPose.offsetAndRotation(10.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 2.0), 0.0F));
    }

    @Override
    public void setupAnim(CursedBoss entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.crystal.yRot = ageInTicks * 0.06F;
        this.crystal.y = -18.0F + Mth.sin(ageInTicks * 0.1F) * 1.5F;
        this.ring1.yRot = ageInTicks * 0.05F;
        this.ring2.yRot = -ageInTicks * 0.07F;
        // руки слегка подняты, как при накоплении души
        this.rightArm.xRot = -0.35F + Mth.sin(ageInTicks * 0.08F) * 0.05F;
        this.leftArm.xRot = -0.35F - Mth.sin(ageInTicks * 0.08F) * 0.05F;
    }
}
