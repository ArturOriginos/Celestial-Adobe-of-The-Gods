package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.world.CelestiaDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Граница мира Селестии: 50000 x 50000 блоков с центром в (0, 0). */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class CelestiaDimensionEvents {
    private CelestiaDimensionEvents() {}

    private static void applyBorder(ServerLevel level) {
        WorldBorder border = level.getWorldBorder();
        if (border.getSize() != CelestiaDimension.WORLD_SIZE || border.getCenterX() != 0.0 || border.getCenterZ() != 0.0) {
            border.setCenter(0.0, 0.0);
            border.setSize(CelestiaDimension.WORLD_SIZE);
        }
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(CelestiaDimension.LEVEL)) {
            applyBorder(level);
        }
    }

    /** Страховка: граница общая с обычным миром по умолчанию, поэтому выставляем её ещё раз после запуска сервера. */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().getLevel(CelestiaDimension.LEVEL);
        if (level != null) applyBorder(level);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.getTo().equals(CelestiaDimension.LEVEL)) return;
        if (event.getEntity().level() instanceof ServerLevel level) applyBorder(level);
    }
}
