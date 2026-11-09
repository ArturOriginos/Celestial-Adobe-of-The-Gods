package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Души как 3D-предметы (модели из Blockbench в assets/celestial/bbdata): медленно вращаются и всегда светятся.
 * Какая модель рисуется - задаёт {@link Spec}.
 */
public class SoulItemRenderer extends BlockEntityWithoutLevelRenderer {
    /**
     * @param model       имя файла bbdata/NAME.txt и текстуры textures/entity/NAME.png
     * @param groups      номера групп в порядке отрисовки (полупрозрачные - в конце)
     * @param centerY     высота центра модели в пикселях Blockbench (середина между min и max по Y)
     * @param fit         размер в пикселях, который занимает почти весь блок (большая из высоты и поперечника)
     * @param translucent true, если в текстуре есть полупрозрачные пиксели (дым)
     */
    public record Spec(String model, int[] groups, float centerY, float fit, boolean translucent) {
        public ResourceLocation texture() {
            return ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/" + model + ".png");
        }
    }

    /** Проклятая душа. */
    public static final Spec RED = new Spec("red_soul", new int[] {0, 1}, 23.0F, 58.0F, false);
    /** Душа Хранителя Богов. */
    public static final Spec YELLOW = new Spec("yellow_soul", new int[] {0, 1}, 23.0F, 58.0F, false);
    /** Объединённая душа: ядро, золотое и красное пламя, осколки. */
    public static final Spec UNITED = new Spec("united_soul", new int[] {0, 1, 2, 3, 4}, -6.0F, 74.0F, false);
    /** Душа Бога Селестии: ядро, ленты, искры, а дым (полупрозрачный) - последним. */
    public static final Spec CELESTIA = new Spec("celestia_soul", new int[] {0, 1, 3, 2}, 7.75F, 84.0F, true);

    private final Spec spec;
    private BBModel model;

    public SoulItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet, Spec spec) {
        super(dispatcher, modelSet);
        this.spec = spec;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ps, MultiBufferSource buffer,
                             int light, int overlay) {
        if (model == null) model = BBModel.load(spec.model());
        ps.pushPose();
        ps.translate(0.5F, 0.5F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees((Util.getMillis() % 12000L) / 12000.0F * 360.0F));
        float s = 0.95F / spec.fit();
        ps.scale(s, s, s);
        ps.translate(0.0F, -spec.centerY(), 0.0F);
        ResourceLocation tex = spec.texture();
        VertexConsumer vc = buffer.getBuffer(spec.translucent()
                ? RenderType.entityTranslucent(tex)
                : RenderType.entityCutoutNoCull(tex));
        for (int g : spec.groups()) {
            model.renderGroup(vc, ps.last(), LightTexture.FULL_BRIGHT, overlay, g, BBModel.NO_CLIP);
        }
        ps.popPose();
    }
}
