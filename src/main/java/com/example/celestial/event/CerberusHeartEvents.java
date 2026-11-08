package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Сердце Цербера: в хотбаре или во второй руке даёт Восстановление IV и Силу III, голод не расходуется. */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class CerberusHeartEvents {
    private CerberusHeartEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !hasHeart(player)) return;
        player.getFoodData().setExhaustion(0.0F); // голод не тратится
        if (player.tickCount % 10 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 3, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2, true, false, true));
        }
    }

    private static boolean hasHeart(ServerPlayer player) {
        if (player.getOffhandItem().is(ModItems.CERBERUS_HEART.get())) return true;
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(ModItems.CERBERUS_HEART.get())) return true;
        }
        return false;
    }
}
