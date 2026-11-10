package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.item.CelestiaArmorSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Сопротивление урону от полных комплектов брони:
 *  Броня Бога (нагрудник + ботинки) - Сопротивление урону III,
 *  Броня Селестии (нагрудник + ботинки) - Сопротивление урону II.
 * Эффект скрытый (без частиц) и пропадает вскоре после снятия брони; зелье посильнее не перебивается.
 */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class ArmorResistanceEvents {
    public static final int DIVINE_AMPLIFIER = 2;   // уровень III
    public static final int CELESTIA_AMPLIFIER = 1; // уровень II

    private ArmorResistanceEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0) return;

        int amplifier = -1;
        if (DivineArmorEvents.wearsChestplate(player) && DivineArmorEvents.wearsBoots(player)) amplifier = DIVINE_AMPLIFIER;
        else if (CelestiaArmorSet.hasFullSet(player)) amplifier = CELESTIA_AMPLIFIER;

        MobEffectInstance current = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (amplifier >= 0) {
            boolean needs = current == null || current.getAmplifier() < amplifier
                    || (current.getAmplifier() == amplifier && current.getDuration() < 30);
            if (needs) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, amplifier, true, false, true));
        } else if (current != null && current.isAmbient() && !current.isVisible()) {
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE); // наш скрытый эффект
        }
    }
}
