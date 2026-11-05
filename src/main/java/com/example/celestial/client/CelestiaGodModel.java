package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.CelestiaGod;
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
 * 3D-модель Бога Селестии: шлем с рогами-ветвями и короной, вертикальный нимб, четырёхконечная звезда над головой,
 * наплечники с шипами, длинная многослойная мантия с острым «хвостом», три вращающихся золотых кольца и
 * парящие кристаллы. Код блока GENERATED создаётся скриптом tools/gen_god.py вместе с текстурой.
 */
public class CelestiaGodModel extends HumanoidModel<CelestiaGod> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MODID, "celestia_god"), "main");

    private final ModelPart ring1;
    private final ModelPart ring2;
    private final ModelPart ring3;
    private final ModelPart star;
    private final ModelPart[] shards = new ModelPart[6];

    public CelestiaGodModel(ModelPart root) {
        super(root);
        this.ring1 = this.body.getChild("ring1");
        this.ring2 = this.body.getChild("ring2");
        this.ring3 = this.body.getChild("ring3");
        this.star = this.head.getChild("star");
        for (int i = 0; i < shards.length; i++) shards[i] = this.body.getChild("shard" + i);
        // ног нет: их закрывает мантия
        this.leftLeg.visible = false;
        this.rightLeg.visible = false;
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        // BEGIN GENERATED
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0F, 0F, 0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(40, 29).addBox(-4F, -8F, -4F, 8F, 8F, 8F), PartPose.offset(0F, 0F, 0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 29).addBox(-5F, 0F, -3F, 10F, 12F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(72, 29).addBox(-3F, -2F, -2F, 4F, 12F, 4F), PartPose.offset(-6F, 2F, 0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(88, 29).addBox(-1F, -2F, -2F, 4F, 12F, 4F), PartPose.offset(6F, 2F, 0F));
        PartDefinition crest = head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(86, 47).addBox(-1F, -11F, -3F, 2F, 4F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition antler_l = head.addOrReplaceChild("antler_l", CubeListBuilder.create().texOffs(78, 47).addBox(-1F, -9F, -1F, 2F, 9F, 2F), PartPose.offsetAndRotation(3F, -6F, 0F, 0F, 0F, 0.35F));
        PartDefinition antler_l2 = antler_l.addOrReplaceChild("antler_l2", CubeListBuilder.create().texOffs(8, 61).addBox(-1F, -7F, -1F, 2F, 7F, 2F), PartPose.offsetAndRotation(0F, -8F, 0F, 0F, 0F, 0.75F));
        PartDefinition antler_l3 = antler_l.addOrReplaceChild("antler_l3", CubeListBuilder.create().texOffs(68, 61).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, -4F, 0F, 0.7F, 0F, -0.5F));
        PartDefinition antler_r = head.addOrReplaceChild("antler_r", CubeListBuilder.create().texOffs(78, 47).addBox(-1F, -9F, -1F, 2F, 9F, 2F), PartPose.offsetAndRotation(-3F, -6F, 0F, 0F, 0F, -0.35F));
        PartDefinition antler_r2 = antler_r.addOrReplaceChild("antler_r2", CubeListBuilder.create().texOffs(8, 61).addBox(-1F, -7F, -1F, 2F, 7F, 2F), PartPose.offsetAndRotation(0F, -8F, 0F, 0F, 0F, -0.75F));
        PartDefinition antler_r3 = antler_r.addOrReplaceChild("antler_r3", CubeListBuilder.create().texOffs(68, 61).addBox(-1F, -5F, -1F, 2F, 5F, 2F), PartPose.offsetAndRotation(0F, -4F, 0F, 0.7F, 0F, 0.5F));
        PartDefinition halo_disc = head.addOrReplaceChild("halo_disc", CubeListBuilder.create().texOffs(50, 0).addBox(-12F, -12F, 0F, 24F, 24F, 1F), PartPose.offset(0F, -4F, 6F));
        PartDefinition star = head.addOrReplaceChild("star", CubeListBuilder.create(), PartPose.offset(0F, -27F, 0F));
        PartDefinition star_v = star.addOrReplaceChild("star_v", CubeListBuilder.create().texOffs(32, 29).addBox(-1F, -8F, -1F, 2F, 16F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition star_h = star.addOrReplaceChild("star_h", CubeListBuilder.create().texOffs(0, 71).addBox(-5F, -1F, -1F, 10F, 2F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition star_c = star.addOrReplaceChild("star_c", CubeListBuilder.create().texOffs(76, 61).addBox(-2F, -2F, -1.5F, 4F, 4F, 3F), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0.785F));
        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(16, 61).addBox(-4F, 0F, -4F, 8F, 8F, 1F), PartPose.offset(0F, 0F, 0F));
        PartDefinition gem = body.addOrReplaceChild("gem", CubeListBuilder.create().texOffs(110, 61).addBox(-2F, -2F, -1F, 4F, 4F, 1F), PartPose.offsetAndRotation(0F, 10F, -3F, 0F, 0F, 0.785F));
        PartDefinition cape = body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 0F, 0F, 10F, 28F, 1F), PartPose.offsetAndRotation(0F, 1F, 3F, 0.07F, 0F, 0F));
        PartDefinition cloak_l = body.addOrReplaceChild("cloak_l", CubeListBuilder.create().texOffs(22, 0).addBox(-1F, 0F, 0F, 3F, 24F, 4F), PartPose.offsetAndRotation(5F, 2F, 1F, 0F, 0F, -0.2F));
        PartDefinition cloak_r = body.addOrReplaceChild("cloak_r", CubeListBuilder.create().texOffs(36, 0).addBox(-2F, 0F, 0F, 3F, 24F, 4F), PartPose.offsetAndRotation(-5F, 2F, 1F, 0F, 0F, 0.2F));
        PartDefinition robe1 = body.addOrReplaceChild("robe1", CubeListBuilder.create().texOffs(0, 47).addBox(-5F, 0F, -3F, 10F, 8F, 6F), PartPose.offset(0F, 12F, 0F));
        PartDefinition robe2 = body.addOrReplaceChild("robe2", CubeListBuilder.create().texOffs(32, 47).addBox(-4F, 0F, -2.5F, 8F, 8F, 5F), PartPose.offset(0F, 19F, 0F));
        PartDefinition robe3 = body.addOrReplaceChild("robe3", CubeListBuilder.create().texOffs(58, 47).addBox(-3F, 0F, -2F, 6F, 8F, 4F), PartPose.offset(0F, 26F, 0F));
        PartDefinition robe4 = body.addOrReplaceChild("robe4", CubeListBuilder.create().texOffs(102, 47).addBox(-2F, 0F, -1.5F, 4F, 7F, 3F), PartPose.offset(0F, 33F, 0F));
        PartDefinition robe5 = body.addOrReplaceChild("robe5", CubeListBuilder.create().texOffs(60, 61).addBox(-1F, 0F, -1F, 2F, 6F, 2F), PartPose.offset(0F, 39F, 0F));
        PartDefinition shard0 = body.addOrReplaceChild("shard0", CubeListBuilder.create().texOffs(116, 47).addBox(-1F, -4F, -1F, 2F, 8F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition shard1 = body.addOrReplaceChild("shard1", CubeListBuilder.create().texOffs(116, 47).addBox(-1F, -4F, -1F, 2F, 8F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition shard2 = body.addOrReplaceChild("shard2", CubeListBuilder.create().texOffs(116, 47).addBox(-1F, -4F, -1F, 2F, 8F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition shard3 = body.addOrReplaceChild("shard3", CubeListBuilder.create().texOffs(116, 47).addBox(-1F, -4F, -1F, 2F, 8F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition shard4 = body.addOrReplaceChild("shard4", CubeListBuilder.create().texOffs(116, 47).addBox(-1F, -4F, -1F, 2F, 8F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition shard5 = body.addOrReplaceChild("shard5", CubeListBuilder.create().texOffs(116, 47).addBox(-1F, -4F, -1F, 2F, 8F, 2F), PartPose.offset(0F, 0F, 0F));
        PartDefinition ring1 = body.addOrReplaceChild("ring1", CubeListBuilder.create(), PartPose.offset(0F, -5F, 0F));
        PartDefinition ring1_0 = ring1.addOrReplaceChild("ring1_0", CubeListBuilder.create().texOffs(24, 71).addBox(-4F, 0F, -0.5F, 8F, 1F, 1F), PartPose.offset(0F, 0F, -9F));
        PartDefinition ring1_1 = ring1.addOrReplaceChild("ring1_1", CubeListBuilder.create().texOffs(24, 71).addBox(-4F, 0F, -0.5F, 8F, 1F, 1F), PartPose.offsetAndRotation(-6.364F, 0F, -6.364F, 0F, 0.7854F, 0F));
        PartDefinition ring1_2 = ring1.addOrReplaceChild("ring1_2", CubeListBuilder.create().texOffs(24, 71).addBox(-4F, 0F, -0.5F, 8F, 1F, 1F), PartPose.offsetAndRotation(-9F, 0F, 0F, 0F, 1.5708F, 0F));
        PartDefinition ring1_3 = ring1.addOrReplaceChild("ring1_3", CubeListBuilder.create().texOffs(24, 71).addBox(-4F, 0F, -0.5F, 8F, 1F, 1F), PartPose.offsetAndRotation(-6.364F, 0F, 6.364F, 0F, 2.3562F, 0F));
        PartDefinition ring1_4 = ring1.addOrReplaceChild("ring1_4", CubeListBuilder.create().texOffs(24, 71).addBox(-4F, 0F, -0.5F, 8F, 1F, 1F), PartPose.offsetAndRotation(0F, 0F, 9F, 0F, 3.1416F, 0F));
        PartDefinition ring1_5 = ring1.addOrReplaceChild("ring1_5", CubeListBuilder.create().texOffs(24, 71).addBox(-4F, 0F, -0.5F, 8F, 1F, 1F), PartPose.offsetAndRotation(6.364F, 0F, 6.364F, 0F, 3.927F, 0F));
        PartDefinition ring1_6 = ring1.addOrReplaceChild("ring1_6", CubeListBuilder.create().texOffs(24, 71).addBox(-4F, 0F, -0.5F, 8F, 1F, 1F), PartPose.offsetAndRotation(9F, 0F, 0F, 0F, 4.7124F, 0F));
        PartDefinition ring1_7 = ring1.addOrReplaceChild("ring1_7", CubeListBuilder.create().texOffs(24, 71).addBox(-4F, 0F, -0.5F, 8F, 1F, 1F), PartPose.offsetAndRotation(6.364F, 0F, -6.364F, 0F, 5.4978F, 0F));
        PartDefinition ring2 = body.addOrReplaceChild("ring2", CubeListBuilder.create(), PartPose.offset(0F, 9F, 0F));
        PartDefinition ring2_0 = ring2.addOrReplaceChild("ring2_0", CubeListBuilder.create().texOffs(42, 71).addBox(-5.5F, 0F, -0.5F, 11F, 1F, 1F), PartPose.offset(0F, 0F, -13F));
        PartDefinition ring2_1 = ring2.addOrReplaceChild("ring2_1", CubeListBuilder.create().texOffs(42, 71).addBox(-5.5F, 0F, -0.5F, 11F, 1F, 1F), PartPose.offsetAndRotation(-9.1924F, 0F, -9.1924F, 0F, 0.7854F, 0F));
        PartDefinition ring2_2 = ring2.addOrReplaceChild("ring2_2", CubeListBuilder.create().texOffs(42, 71).addBox(-5.5F, 0F, -0.5F, 11F, 1F, 1F), PartPose.offsetAndRotation(-13F, 0F, 0F, 0F, 1.5708F, 0F));
        PartDefinition ring2_3 = ring2.addOrReplaceChild("ring2_3", CubeListBuilder.create().texOffs(42, 71).addBox(-5.5F, 0F, -0.5F, 11F, 1F, 1F), PartPose.offsetAndRotation(-9.1924F, 0F, 9.1924F, 0F, 2.3562F, 0F));
        PartDefinition ring2_4 = ring2.addOrReplaceChild("ring2_4", CubeListBuilder.create().texOffs(42, 71).addBox(-5.5F, 0F, -0.5F, 11F, 1F, 1F), PartPose.offsetAndRotation(0F, 0F, 13F, 0F, 3.1416F, 0F));
        PartDefinition ring2_5 = ring2.addOrReplaceChild("ring2_5", CubeListBuilder.create().texOffs(42, 71).addBox(-5.5F, 0F, -0.5F, 11F, 1F, 1F), PartPose.offsetAndRotation(9.1924F, 0F, 9.1924F, 0F, 3.927F, 0F));
        PartDefinition ring2_6 = ring2.addOrReplaceChild("ring2_6", CubeListBuilder.create().texOffs(42, 71).addBox(-5.5F, 0F, -0.5F, 11F, 1F, 1F), PartPose.offsetAndRotation(13F, 0F, 0F, 0F, 4.7124F, 0F));
        PartDefinition ring2_7 = ring2.addOrReplaceChild("ring2_7", CubeListBuilder.create().texOffs(42, 71).addBox(-5.5F, 0F, -0.5F, 11F, 1F, 1F), PartPose.offsetAndRotation(9.1924F, 0F, -9.1924F, 0F, 5.4978F, 0F));
        PartDefinition ring3 = body.addOrReplaceChild("ring3", CubeListBuilder.create(), PartPose.offset(0F, 26F, 0F));
        PartDefinition ring3_0 = ring3.addOrReplaceChild("ring3_0", CubeListBuilder.create().texOffs(66, 71).addBox(-7F, 0F, -0.5F, 14F, 1F, 1F), PartPose.offset(0F, 0F, -17F));
        PartDefinition ring3_1 = ring3.addOrReplaceChild("ring3_1", CubeListBuilder.create().texOffs(66, 71).addBox(-7F, 0F, -0.5F, 14F, 1F, 1F), PartPose.offsetAndRotation(-12.0208F, 0F, -12.0208F, 0F, 0.7854F, 0F));
        PartDefinition ring3_2 = ring3.addOrReplaceChild("ring3_2", CubeListBuilder.create().texOffs(66, 71).addBox(-7F, 0F, -0.5F, 14F, 1F, 1F), PartPose.offsetAndRotation(-17F, 0F, 0F, 0F, 1.5708F, 0F));
        PartDefinition ring3_3 = ring3.addOrReplaceChild("ring3_3", CubeListBuilder.create().texOffs(66, 71).addBox(-7F, 0F, -0.5F, 14F, 1F, 1F), PartPose.offsetAndRotation(-12.0208F, 0F, 12.0208F, 0F, 2.3562F, 0F));
        PartDefinition ring3_4 = ring3.addOrReplaceChild("ring3_4", CubeListBuilder.create().texOffs(66, 71).addBox(-7F, 0F, -0.5F, 14F, 1F, 1F), PartPose.offsetAndRotation(0F, 0F, 17F, 0F, 3.1416F, 0F));
        PartDefinition ring3_5 = ring3.addOrReplaceChild("ring3_5", CubeListBuilder.create().texOffs(66, 71).addBox(-7F, 0F, -0.5F, 14F, 1F, 1F), PartPose.offsetAndRotation(12.0208F, 0F, 12.0208F, 0F, 3.927F, 0F));
        PartDefinition ring3_6 = ring3.addOrReplaceChild("ring3_6", CubeListBuilder.create().texOffs(66, 71).addBox(-7F, 0F, -0.5F, 14F, 1F, 1F), PartPose.offsetAndRotation(17F, 0F, 0F, 0F, 4.7124F, 0F));
        PartDefinition ring3_7 = ring3.addOrReplaceChild("ring3_7", CubeListBuilder.create().texOffs(66, 71).addBox(-7F, 0F, -0.5F, 14F, 1F, 1F), PartPose.offsetAndRotation(12.0208F, 0F, -12.0208F, 0F, 5.4978F, 0F));
        PartDefinition pauldron_r = right_arm.addOrReplaceChild("pauldron_r", CubeListBuilder.create().texOffs(34, 61).addBox(-4F, -4F, -3F, 7F, 3F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition pspike_r = right_arm.addOrReplaceChild("pspike_r", CubeListBuilder.create().texOffs(0, 61).addBox(-1F, -8F, -1F, 2F, 8F, 2F), PartPose.offsetAndRotation(-3F, -3F, 0F, 0F, 0F, 0.55F));
        PartDefinition cuff_r = right_arm.addOrReplaceChild("cuff_r", CubeListBuilder.create().texOffs(90, 61).addBox(-3.5F, 8F, -2.5F, 5F, 2F, 5F), PartPose.offset(0F, 0F, 0F));
        PartDefinition pauldron_l = left_arm.addOrReplaceChild("pauldron_l", CubeListBuilder.create().texOffs(34, 61).addBox(-3F, -4F, -3F, 7F, 3F, 6F), PartPose.offset(0F, 0F, 0F));
        PartDefinition pspike_l = left_arm.addOrReplaceChild("pspike_l", CubeListBuilder.create().texOffs(0, 61).addBox(-1F, -8F, -1F, 2F, 8F, 2F), PartPose.offsetAndRotation(3F, -3F, 0F, 0F, 0F, -0.55F));
        PartDefinition cuff_l = left_arm.addOrReplaceChild("cuff_l", CubeListBuilder.create().texOffs(90, 61).addBox(-1.5F, 8F, -2.5F, 5F, 2F, 5F), PartPose.offset(0F, 0F, 0F));
        // END GENERATED
        return LayerDefinition.create(mesh, 128, 80);
    }

    @Override
    public void setupAnim(CelestiaGod entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float pt = ageInTicks - entity.tickCount;
        float breathe = Mth.sin(ageInTicks * 0.08F) * 0.04F;

        // руки: поза зависит от стадии атаки (плавно интерполируется на стороне сущности)
        float ax = entity.getArmX(pt);
        float az = entity.getArmZ(pt);
        float swingKeep = entity.getStage() == CelestiaGod.ST_IDLE ? 0.35F : 0.0F;
        this.rightArm.xRot = ax + this.rightArm.xRot * swingKeep + breathe;
        this.leftArm.xRot = ax + this.leftArm.xRot * swingKeep - breathe;
        this.rightArm.zRot = az + breathe;
        this.leftArm.zRot = -az - breathe;
        if (this.attackTime > 0.0F) { // обычный удар паксилем
            this.rightArm.xRot -= Mth.sin(this.attackTime * (float) Math.PI) * 1.4F;
        }

        // кольца вращаются и «дышат» (расширяются при кастах)
        float rs = entity.getRingScale(pt);
        this.ring1.yRot = ageInTicks * 0.05F;
        this.ring2.yRot = -ageInTicks * 0.07F;
        this.ring3.yRot = ageInTicks * 0.04F;
        for (ModelPart r : new ModelPart[]{ring1, ring2, ring3}) {
            r.xScale = rs;
            r.zScale = rs;
        }
        this.ring1.y = -5.0F + Mth.sin(ageInTicks * 0.1F) * 1.0F;
        this.ring2.y = 9.0F + Mth.sin(ageInTicks * 0.1F + 1.5F) * 1.0F;

        // звезда над головой вращается и парит
        this.star.yRot = ageInTicks * 0.08F;
        this.star.y = -27.0F + Mth.sin(ageInTicks * 0.1F) * 1.5F;

        // парящие кристаллы по орбите
        for (int i = 0; i < shards.length; i++) {
            float ang = ageInTicks * 0.05F + i * ((float) Math.PI * 2.0F / shards.length);
            float r = 17.0F * (0.85F + 0.15F * rs) + Mth.sin(ageInTicks * 0.09F + i) * 1.5F;
            shards[i].x = Mth.cos(ang) * r;
            shards[i].z = Mth.sin(ang) * r;
            shards[i].y = 8.0F + Mth.sin(ageInTicks * 0.07F + i * 1.7F) * 7.0F;
            shards[i].yRot = -ang;
            shards[i].zRot = Mth.sin(ageInTicks * 0.05F + i) * 0.25F;
        }
    }
}
