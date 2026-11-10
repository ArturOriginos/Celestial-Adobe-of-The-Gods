package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.FenrirEffect;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Эффекты Фенрира: дух воя (fenrir_howl_spirit), след прыжка (fenrir_leap_slash) и всплеск шипов
 * (fenrir_spike_burst). Анимации (дыхание пламени, бег полос, рост шипов) повторяют анимации из моделей.
 */
public class FenrirEffectRenderer extends EntityRenderer<FenrirEffect> {
    private static final Map<String, BBModel> MODELS = new HashMap<>();

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/" + name + ".png");
    }

    private static BBModel model(String name) {
        return MODELS.computeIfAbsent(name, BBModel::load);
    }

    public FenrirEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(FenrirEffect entity) {
        return tex("fenrir_howl_spirit");
    }

    @Override
    public boolean shouldRender(FenrirEffect entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(FenrirEffect e, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffer, int light) {
        float t = e.tickCount + pt;
        float life = Math.max(1, e.getLife());
        float fade = Mth.clamp(t / 6.0F, 0.0F, 1.0F) * Mth.clamp((life - t) / 8.0F, 0.0F, 1.0F);
        if (fade <= 0.0F) return;
        float seconds = t / 20.0F;
        float yaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        int full = LightTexture.FULL_BRIGHT;
        int none = OverlayTexture.NO_OVERLAY;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        float s = e.getScale() / 16.0F;

        switch (e.getKind()) {
            case FenrirEffect.KIND_HOWL -> {
                String name = "fenrir_howl_spirit";
                BBModel m = model(name);
                float grow = 0.55F + 0.45F * fade;
                ps.scale(s * grow, s * grow, s * grow);
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(tex(name)));
                for (int g = 0; g <= 4; g++) { // 0 дух, 1 челюсть, 2 пламя, 3 дуги, 4 обломки
                    ps.pushPose();
                    switch (g) {
                        case 1 -> BBAnim.rotX(ps, 0.0F, 54.0F, -4.0F, -8.0F * (0.5F - 0.5F * Mth.cos(Mth.PI * seconds)));
                        case 2 -> { // пламя «дышит» по высоте
                            float sy = 1.0F + 0.12F * (0.5F - 0.5F * Mth.cos(Mth.PI * seconds));
                            ps.scale(1.0F, sy, 1.0F);
                        }
                        case 3 -> ps.mulPose(Axis.YP.rotationDegrees(t * 2.0F));
                        case 4 -> ps.translate(0.0F, 2.0F * Mth.sin(t * 0.15F), 0.0F);
                        default -> {}
                    }
                    m.renderGroup(vc, ps.last(), full, none, g, BBModel.NO_CLIP);
                    ps.popPose();
                }
            }
            case FenrirEffect.KIND_LEAP -> {
                String name = "fenrir_leap_slash";
                BBModel m = model(name);
                ps.scale(s, s, s);
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(tex(name)));
                for (int g = 0; g <= 2; g++) { // 0 полумесяц, 1 полосы, 2 шипы
                    ps.pushPose();
                    if (g == 1) ps.translate(0.0F, 0.0F, 30.0F * ((t / 10.0F) % 1.0F)); // rush: полосы бегут назад
                    m.renderGroup(vc, ps.last(), full, none, g, BBModel.NO_CLIP);
                    ps.popPose();
                }
            }
            default -> {
                String name = "fenrir_spike_burst";
                BBModel m = model(name);
                ps.scale(s, s, s);
                VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(tex(name)));
                // rise: шипы быстро вырастают (0.05 -> 1.1 -> 1.0), в конце уходят обратно в землю
                float rise = t < 8.0F ? Mth.lerp(t / 8.0F, 0.05F, 1.1F) : t < 16.0F ? Mth.lerp((t - 8.0F) / 8.0F, 1.1F, 1.0F) : 1.0F;
                float sink = Mth.clamp((life - t) / 10.0F, 0.0F, 1.0F);
                float sy = Math.max(0.01F, rise * sink);
                for (int g = 0; g <= 1; g++) { // 0 шипы, 1 обломки
                    ps.pushPose();
                    if (g == 0) ps.scale(1.0F, sy, 1.0F);
                    else ps.translate(0.0F, 2.0F * Mth.sin(t * 0.2F) * sink, 0.0F);
                    m.renderGroup(vc, ps.last(), full, none, g, BBModel.NO_CLIP);
                    ps.popPose();
                }
            }
        }
        ps.popPose();
        super.render(e, entityYaw, pt, ps, buffer, light);
    }
}
