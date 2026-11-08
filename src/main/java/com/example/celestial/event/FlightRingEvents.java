package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Кольцо полёта богов: креативный полёт, пока кольцо лежит где угодно в инвентаре (или надеты Ботинки Бога). Забрал кольцо - полёт пропадает. */
@EventBusSubscriber(modid = CelestialMod.MODID)
public class FlightRingEvents {
    private static final String KEY = "celestial_ring_flight";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 5 != 0) return;
        CompoundTag data = player.getPersistentData();
        // в творческом и наблюдении полёт и так есть, ничего не трогаем
        if (player.isCreative() || player.isSpectator()) {
            data.putBoolean(KEY, false);
            return;
        }
        Abilities abilities = player.getAbilities();
        if (hasRing(player) || DivineArmorEvents.wearsBoots(player)) {
            if (!abilities.mayfly) {
                abilities.mayfly = true;
                player.onUpdateAbilities();
            }
            data.putBoolean(KEY, true);
        } else if (data.getBoolean(KEY)) {
            abilities.mayfly = false;
            abilities.flying = false;
            player.onUpdateAbilities();
            data.putBoolean(KEY, false);
        }
    }

    private static boolean hasRing(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(ModItems.FLIGHT_RING.get())) return true;
        }
        return false;
    }
}
