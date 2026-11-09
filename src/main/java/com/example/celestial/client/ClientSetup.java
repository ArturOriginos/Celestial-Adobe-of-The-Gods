package com.example.celestial.client;

import com.example.celestial.CelestialMod;
import com.example.celestial.registry.ModEntities;
import com.example.celestial.registry.ModItems;
import com.example.celestial.registry.ModMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = CelestialMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ADVANCED_ENCHANTING.get(), AdvancedEnchantingScreen::new);
        event.register(ModMenus.SOLAR_ANVIL.get(), SolarAnvilScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GUARDIAN.get(), GuardianRenderer::new);
        event.registerEntityRenderer(ModEntities.GOD_SPIKE.get(), GodSpikeRenderer::new);
        event.registerEntityRenderer(ModEntities.CURSED_BOSS.get(), CursedBossRenderer::new);
        event.registerEntityRenderer(ModEntities.ALL_KNOWING_ANGEL.get(), AllKnowingAngelRenderer::new);
        event.registerEntityRenderer(ModEntities.DARK_ORB.get(), DarkOrbRenderer::new);
        event.registerEntityRenderer(ModEntities.CELESTIA_GOD.get(), CelestiaGodRenderer::new);
        event.registerEntityRenderer(ModEntities.CELESTIA_EFFECT.get(), CelestiaEffectRenderer::new);
        event.registerEntityRenderer(ModEntities.FLYING_SOUL.get(), FlyingSoulRenderer::new);
        event.registerEntityRenderer(ModEntities.CURSED_PIG.get(), CursedMobRenderers.PigR::new);
        event.registerEntityRenderer(ModEntities.CURSED_COW.get(), CursedMobRenderers.CowR::new);
        event.registerEntityRenderer(ModEntities.CURSED_SHEEP.get(), CursedMobRenderers.SheepR::new);
        event.registerEntityRenderer(ModEntities.CURSED_CHICKEN.get(), CursedMobRenderers.ChickenR::new);
        event.registerEntityRenderer(ModEntities.CURSED_ZOMBIE.get(), CursedMobRenderers.ZombieR::new);
        event.registerEntityRenderer(ModEntities.CURSED_SKELETON.get(), CursedMobRenderers.SkeletonR::new);
        event.registerEntityRenderer(ModEntities.CELESTIA_SPIKE.get(),
                context -> new PowerSpikeRenderer(context, PowerSpikeRenderer.CELESTIA));
        event.registerEntityRenderer(ModEntities.CURSED_SPIKE.get(),
                context -> new PowerSpikeRenderer(context, PowerSpikeRenderer.CURSED));
    }

    /** Щит проклятых и Защитная Руна Селестии рисуются как ванильный щит (встроенная модель + свой рендерер) (встроенная модель + свой рендерер). */
    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(shieldExtension(CursedShieldRenderer.CURSED_TEXTURE), ModItems.CURSED_SHIELD.get());
        event.registerItem(shieldExtension(CursedShieldRenderer.CELESTIA_TEXTURE), ModItems.CELESTIA_SHIELD.get());
        event.registerItem(soulExtension(SoulItemRenderer.RED), ModItems.CURSED_SOUL.get());
        event.registerItem(soulExtension(SoulItemRenderer.YELLOW), ModItems.GUARDIAN_SOUL.get());
        event.registerItem(soulExtension(SoulItemRenderer.UNITED), ModItems.UNITED_SOUL.get());
        event.registerItem(soulExtension(SoulItemRenderer.CELESTIA), ModItems.CELESTIA_GOD_SOUL.get());
        event.registerItem(armorExtension(DivineModels.CHEST_LAYER), ModItems.DIVINE_CHESTPLATE.get());
        event.registerItem(armorExtension(DivineModels.BOOTS_LAYER), ModItems.DIVINE_BOOTS.get());
    }

    /** Своя 3D-модель брони (крылья, рога, перья), вместо стандартной одежды. */
    private static IClientItemExtensions armorExtension(net.minecraft.client.model.geom.ModelLayerLocation layer) {
        return new IClientItemExtensions() {
            private net.minecraft.client.model.HumanoidModel<net.minecraft.world.entity.LivingEntity> model;

            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                    net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.item.ItemStack stack,
                    net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.client.model.HumanoidModel<?> original) {
                if (model == null) {
                    model = new net.minecraft.client.model.HumanoidModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(layer));
                }
                ((net.minecraft.client.model.HumanoidModel) original).copyPropertiesTo(model);
                return model;
            }
        };
    }

    /** Души (проклятая, Хранителя Богов, объединённая, Бога Селестии) рисуются своими 3D-моделями. */
    private static IClientItemExtensions soulExtension(SoulItemRenderer.Spec spec) {
        return new IClientItemExtensions() {
            private SoulItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new SoulItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), spec);
                }
                return renderer;
            }
        };
    }

    private static IClientItemExtensions shieldExtension(ResourceLocation texture) {
        return new IClientItemExtensions() {
            private CursedShieldRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new CursedShieldRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), texture);
                }
                return renderer;
            }
        };
    }

    /** Нимб брони Селестии добавляется ко всем скинам игрока (широкий и тонкий). */
    @SubscribeEvent
    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(new CelestiaHaloLayer(playerRenderer, event.getEntityModels()));
                playerRenderer.addLayer(new MiniSoulLayer(playerRenderer));
                playerRenderer.addLayer(new DivineAuraLayer(playerRenderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CelestiaHaloLayer.LAYER, CelestiaHaloLayer::createLayer);
        event.registerLayerDefinition(DivineModels.CHEST_LAYER, DivineModels::createChestLayer);
        event.registerLayerDefinition(DivineModels.BOOTS_LAYER, DivineModels::createBootsLayer);
        event.registerLayerDefinition(DivineModels.AURA_LAYER, DivineModels::createAuraLayer);
    }

    /** Свойство модели "enchanted": книга с зачарованиями получает второй (переливающийся) спрайт. */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.ADVANCED_BOOK.get(),
                ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "enchanted"),
                (stack, level, entity, seed) ->
                        stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
                                ? 0.0F : 1.0F));
        // щит Селестии: предикат "blocking" как у ванильного щита
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.CELESTIA_SHIELD.get(),
                ResourceLocation.withDefaultNamespace("blocking"),
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F));
        // щит проклятых: то же самое
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.CURSED_SHIELD.get(),
                ResourceLocation.withDefaultNamespace("blocking"),
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F));
    }
}
