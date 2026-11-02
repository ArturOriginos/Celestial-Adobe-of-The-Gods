package com.example.celestial.client;

import com.example.celestial.CelestialMod;
import com.example.celestial.registry.ModEntities;
import com.example.celestial.registry.ModItems;
import com.example.celestial.registry.ModMenus;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

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
        event.registerEntityRenderer(ModEntities.CURSED_SPIKE.get(),
                context -> new GodSpikeRenderer(context, GodSpikeRenderer.CURSED_TEXTURE));
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CursedBossModel.LAYER, CursedBossModel::createBodyLayer);
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
    }
}
