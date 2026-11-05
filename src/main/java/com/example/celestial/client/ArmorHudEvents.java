package com.example.celestial.client;

import com.example.celestial.CelestialMod;
import com.example.celestial.item.CelestiaArmorSet;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** С полным комплектом брони Селестии полоска защиты над едой не серая, а светло-жёлтая. */
@EventBusSubscriber(modid = CelestialMod.MODID, value = Dist.CLIENT)
public class ArmorHudEvents {
    private static boolean tinted = false;

    @SubscribeEvent
    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        if (tinted) { // на случай, если Post не пришёл
            event.getGuiGraphics().setColor(1.0F, 1.0F, 1.0F, 1.0F);
            tinted = false;
        }
        if (!event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL)) return;
        Player player = Minecraft.getInstance().player;
        if (player == null || !CelestiaArmorSet.hasFullSet(player)) return;
        event.getGuiGraphics().setColor(1.0F, 0.92F, 0.45F, 1.0F);
        tinted = true;
    }

    @SubscribeEvent
    public static void onLayerPost(RenderGuiLayerEvent.Post event) {
        if (!tinted || !event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL)) return;
        event.getGuiGraphics().setColor(1.0F, 1.0F, 1.0F, 1.0F);
        tinted = false;
    }
}
