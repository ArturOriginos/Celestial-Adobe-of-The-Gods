package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.world.UnknownDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Правила измерения «???»: блоки ломать и ставить нельзя (кроме креатива), взрывы ничего не разрушают. */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class UnknownDimensionEvents {
    /** Тиков до появления Ангела: за это время прогружаются сущности зала, чтобы не создать второго Ангела. */
    private static int angelCountdown = -1;

    private UnknownDimensionEvents() {}

    public static void scheduleAngel() {
        angelCountdown = 30;
    }

    private static boolean inUnknown(LevelAccessor level) {
        return level instanceof Level l && l.dimension().equals(UnknownDimension.LEVEL);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (inUnknown(event.getLevel()) && !event.getPlayer().isCreative()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!inUnknown(event.getLevel())) return;
        if (event.getEntity() instanceof Player p && p.isCreative()) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel().dimension().equals(UnknownDimension.LEVEL)) event.getAffectedBlocks().clear();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (angelCountdown < 0 || --angelCountdown > 0) return;
        angelCountdown = -1;
        ServerLevel level = event.getServer().getLevel(UnknownDimension.LEVEL);
        if (level != null && !level.players().isEmpty()) UnknownDimension.ensureAngel(level);
    }
}
