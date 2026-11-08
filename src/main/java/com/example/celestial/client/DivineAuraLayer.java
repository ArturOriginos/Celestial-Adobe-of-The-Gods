package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
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

/**
 * Аура Нагрудника Бога: нимб со шпилем над головой и два вращающихся кольца вокруг тела
 * (модель divine_aura.bbmodel, анимации idle и appear из неё). Видна только от третьего лица.
 */
public class DivineAuraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/divine_aura.png");
    private static final float HALO_BASE_Y = -12.0F; // halo_root: y=36 в Blockbench -> 24-36
    /** Когда (в тиках существа) аура впервые появилась у игрока: для анимации appear. */
    private static final Map<UUID, Float> APPEARED = new HashMap<>();

    private final ModelPart root;
    private final ModelPart haloRoot;
    private final ModelPart halo;
    private final ModelPart ringAPrecess;
    private final ModelPart ringASpin;
    private final ModelPart ringBPrecess;
    private final ModelPart ringBSpin;

    public DivineAuraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                           EntityModelSet models) {
        super(parent);
        this.root = models.bakeLayer(DivineModels.AURA_LAYER);
        this.haloRoot = root.getChild("halo_root");
        this.halo = haloRoot.getChild("halo");
        this.ringAPrecess = root.getChild("ring_a_precess");
        this.ringASpin = ringAPrecess.getChild("ring_a_tilt").getChild("ring_a_spin");
        this.ringBPrecess = root.getChild("ring_b_precess");
        this.ringBSpin = ringBPrecess.getChild("ring_b_tilt").getChild("ring_b_spin");
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (!player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.DIVINE_CHESTPLATE.get())) {
            APPEARED.remove(player.getUUID());
            return;
        }
        if (player.isInvisible() || player.isSpectator()) return;
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) return; // только от 3 лица

        float start = APPEARED.computeIfAbsent(player.getUUID(), id -> ageInTicks);
        float since = Math.max(0.0F, (ageInTicks - start) / 20.0F);
        // appear: масштаб 0.01 -> 1.1 (0.6 с) -> 1.0 (1 с)
        float appear = since >= 1.0F ? 1.0F
                : since < 0.6F ? Mth.lerp(since / 0.6F, 0.01F, 1.1F)
                : Mth.lerp((since - 0.6F) / 0.4F, 1.1F, 1.0F);

        float t = ageInTicks / 20.0F; // секунды, цикл анимации 8 с
        float cycle = t / 8.0F * Mth.TWO_PI;
        // idle (в Blockbench ось Y направлена вверх и X отражён, поэтому знаки у Y-поворотов обратные)
        this.haloRoot.y = HALO_BASE_Y - 0.7F * Mth.sin(t / 4.0F * Mth.TWO_PI);
        this.halo.yRot = -cycle;
        this.ringASpin.yRot = -cycle * 2.0F;
        this.ringBSpin.yRot = cycle * 3.0F;
        this.ringAPrecess.yRot = -cycle;
        this.ringBPrecess.yRot = cycle;
        for (ModelPart part : new ModelPart[] {haloRoot, ringAPrecess, ringBPrecess}) {
            part.xScale = appear;
            part.yScale = appear;
            part.zScale = appear;
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.root.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }
}
