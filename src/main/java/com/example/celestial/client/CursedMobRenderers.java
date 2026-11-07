package com.example.celestial.client;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.CursedSheep;
import com.example.celestial.entity.CursedSkeleton;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;

/**
 * Рендереры проклятых мобов. Геометрия у них ванильная (модели из Blockbench совпадают с ванильными),
 * меняются текстуры, плюс светящийся слой из файлов *_e.png.
 */
public final class CursedMobRenderers {
    private CursedMobRenderers() {}

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "textures/entity/" + name + ".png");
    }

    // ------------------------------------------------------------------ свинья
    public static class PigR extends PigRenderer {
        private static final ResourceLocation TEXTURE = tex("cursed_pig");
        private static final ResourceLocation GLOW = tex("cursed_pig_e");

        public PigR(EntityRendererProvider.Context context) {
            super(context);
            this.addLayer(new EyesLayer<Pig, PigModel<Pig>>(this) {
                @Override
                public RenderType renderType() {
                    return RenderType.eyes(GLOW);
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(Pig entity) {
            return TEXTURE;
        }
    }

    // ------------------------------------------------------------------ корова
    public static class CowR extends CowRenderer {
        private static final ResourceLocation TEXTURE = tex("cursed_cow");
        private static final ResourceLocation GLOW = tex("cursed_cow_e");

        public CowR(EntityRendererProvider.Context context) {
            super(context);
            this.addLayer(new EyesLayer<Cow, CowModel<Cow>>(this) {
                @Override
                public RenderType renderType() {
                    return RenderType.eyes(GLOW);
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(Cow entity) {
            return TEXTURE;
        }
    }

    // ------------------------------------------------------------------ курица
    public static class ChickenR extends ChickenRenderer {
        private static final ResourceLocation TEXTURE = tex("cursed_chicken");
        private static final ResourceLocation GLOW = tex("cursed_chicken_e");

        public ChickenR(EntityRendererProvider.Context context) {
            super(context);
            this.addLayer(new EyesLayer<Chicken, ChickenModel<Chicken>>(this) {
                @Override
                public RenderType renderType() {
                    return RenderType.eyes(GLOW);
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(Chicken entity) {
            return TEXTURE;
        }
    }

    // ------------------------------------------------------------------ зомби
    public static class ZombieR extends ZombieRenderer {
        private static final ResourceLocation TEXTURE = tex("cursed_zombie");
        private static final ResourceLocation GLOW = tex("cursed_zombie_e");

        public ZombieR(EntityRendererProvider.Context context) {
            super(context);
            this.addLayer(new EyesLayer<Zombie, ZombieModel<Zombie>>(this) {
                @Override
                public RenderType renderType() {
                    return RenderType.eyes(GLOW);
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(Zombie entity) {
            return TEXTURE;
        }
    }

    // ------------------------------------------------------------------ скелет (с луком в руках)
    public static class SkeletonR extends HumanoidMobRenderer<CursedSkeleton, SkeletonModel<CursedSkeleton>> {
        private static final ResourceLocation TEXTURE = tex("cursed_skeleton");
        private static final ResourceLocation GLOW = tex("cursed_skeleton_e");

        public SkeletonR(EntityRendererProvider.Context context) {
            super(context, new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.5F);
            this.addLayer(new HumanoidArmorLayer<>(this,
                    new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)),
                    new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR)),
                    context.getModelManager()));
            this.addLayer(new EyesLayer<CursedSkeleton, SkeletonModel<CursedSkeleton>>(this) {
                @Override
                public RenderType renderType() {
                    return RenderType.eyes(GLOW);
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(CursedSkeleton entity) {
            return TEXTURE;
        }
    }

    // ------------------------------------------------------------------ овца (тело + шерсть, обе со свечением)
    public static class SheepR extends MobRenderer<CursedSheep, SheepModel<CursedSheep>> {
        private static final ResourceLocation TEXTURE = tex("cursed_sheep");
        private static final ResourceLocation GLOW = tex("cursed_sheep_e");
        private static final ResourceLocation WOOL = tex("cursed_sheep_wool");
        private static final ResourceLocation WOOL_GLOW = tex("cursed_sheep_wool_e");

        public SheepR(EntityRendererProvider.Context context) {
            super(context, new SheepModel<>(context.bakeLayer(ModelLayers.SHEEP)), 0.7F);
            this.addLayer(new WoolLayer(this, context.getModelSet()));
            this.addLayer(new EyesLayer<CursedSheep, SheepModel<CursedSheep>>(this) {
                @Override
                public RenderType renderType() {
                    return RenderType.eyes(GLOW);
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(CursedSheep entity) {
            return TEXTURE;
        }

        /** Шерсть проклятой овцы: своя текстура и светящийся слой (без окраски красителями). */
        private static final class WoolLayer extends RenderLayer<CursedSheep, SheepModel<CursedSheep>> {
            private final SheepFurModel<CursedSheep> furModel;

            WoolLayer(RenderLayerParent<CursedSheep, SheepModel<CursedSheep>> parent, EntityModelSet models) {
                super(parent);
                this.furModel = new SheepFurModel<>(models.bakeLayer(ModelLayers.SHEEP_FUR));
            }

            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, CursedSheep sheep,
                               float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                               float netHeadYaw, float headPitch) {
                if (sheep.isSheared() || sheep.isInvisible()) return;
                this.getParentModel().copyPropertiesTo(this.furModel);
                this.furModel.prepareMobModel(sheep, limbSwing, limbSwingAmount, partialTick);
                this.furModel.setupAnim(sheep, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                int overlay = LivingEntityRenderer.getOverlayCoords(sheep, 0.0F);
                this.furModel.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(WOOL)),
                        packedLight, overlay, -1);
                this.furModel.renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(WOOL_GLOW)),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
            }
        }
    }
}
