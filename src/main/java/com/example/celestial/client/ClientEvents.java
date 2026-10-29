package com.example.celestial.client;

import com.example.celestial.CelestialMod;
import com.example.celestial.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Кастомные надписи (жёлтый -> серый) внизу описания предметов Селестии. */
@EventBusSubscriber(modid = CelestialMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String key = null;
        if (stack.is(ModItems.CELESTIA_SWORD.get())) key = "item.celestial.celestia_sword.lore";
        else if (stack.is(ModItems.CELESTIA_AXE.get())) key = "item.celestial.celestia_axe.lore";
        else if (stack.is(ModItems.CELESTIA_PICKAXE.get())) key = "item.celestial.celestia_pickaxe.lore";
        if (key == null) return;
        event.getToolTip().add(Component.empty());
        event.getToolTip().add(GradientText.of(Component.translatable(key).getString()));
    }
}
