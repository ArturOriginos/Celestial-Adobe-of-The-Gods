package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.item.CelestiaArmorSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Бонус полного комплекта брони Селестии: +20 максимального здоровья (ещё одна строчка сердец). */
@EventBusSubscriber(modid = CelestialMod.MODID)
public class ArmorSetEvents {
    private static final ResourceLocation SET_HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "celestia_set_health");
    public static final double SET_HEALTH_BONUS = 20.0D;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 5 != 0) return;
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        boolean full = CelestiaArmorSet.hasFullSet(player);
        boolean has = health.getModifier(SET_HEALTH_ID) != null;
        if (full && !has) {
            health.addTransientModifier(new AttributeModifier(SET_HEALTH_ID, SET_HEALTH_BONUS, AttributeModifier.Operation.ADD_VALUE));
        } else if (!full && has) {
            health.removeModifier(SET_HEALTH_ID);
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        }
    }
}
