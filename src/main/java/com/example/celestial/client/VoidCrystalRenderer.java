package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

/**
 * Энд-кристалл с моделью «Кристалл Бездны»: клетка и ядро вращаются в разные стороны и пульсируют (idle_spin + pulse),
 * основание рисуется только если у кристалла есть дно, луч к цели (дракон или блок) - ванильный.
 */
public class VoidCrystalRenderer extends EntityRenderer<EndCrystal> {
    private static final String NAME = "void_crystal";
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/" + NAME + ".png");
    private static final float SCALE = 1.0F;

    private static BBModel model;
    private static BBRig rig;
    private static int base;

    public VoidCrystalRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    /** Покачивание луча вместе с кристаллом (та же формула, что у ванильного кристалла). */
    public static float bob(float age) {
        float f = Mth.sin(age * 0.2F) / 2.0F + 0.5F;
        f = (f * f + f) * 0.4F;
        return f - 1.4F;
    }

    @Override
    public ResourceLocation getTextureLocation(EndCrystal entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(EndCrystal crystal, Frustum frustum, double camX, double camY, double camZ) {
        return super.shouldRender(crystal, frustum, camX, camY, camZ) || crystal.getBeamTarget() != null;
    }

    @Override
    public void render(EndCrystal crystal, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffer, int light) {
        if (model == null) {
            model = BBModel.load(NAME);
            rig = BBRig.load(NAME);
            base = rig.index("base");
        }
        float age = crystal.time + pt;
        BBRig.Pose pose = rig.newPose();
        rig.apply(pose, "idle_spin", age / 20.0F, 1.0F);
        rig.apply(pose, "pulse", age / 20.0F, 1.0F);

        ps.pushPose();
        float s = SCALE / 16.0F;
        ps.scale(s, s, s);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (int g = 0; g < rig.groupCount(); g++) {
            if (g == base && !crystal.showsBottom()) continue;
            ps.pushPose();
            rig.transform(ps, pose, g);
            model.renderGroup(vc, ps.last(), g == base ? light : LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, g, BBModel.NO_CLIP);
            ps.popPose();
        }
        ps.popPose();

        BlockPos target = crystal.getBeamTarget();
        if (target != null) { // луч к цели (ванильный)
            float dx = (float) ((double) target.getX() + 0.5D - crystal.getX());
            float dy = (float) ((double) target.getY() + 0.5D - crystal.getY());
            float dz = (float) ((double) target.getZ() + 0.5D - crystal.getZ());
            ps.pushPose();
            ps.translate(dx, dy, dz);
            EnderDragonRenderer.renderCrystalBeams(-dx, -dy + bob(age), -dz, pt, crystal.time, ps, buffer, light);
            ps.popPose();
        }
        super.render(crystal, entityYaw, pt, ps, buffer, light);
    }
}
