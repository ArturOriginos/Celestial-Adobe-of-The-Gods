package com.example.celestial.network;

import com.example.celestial.CelestialMod;
import com.example.celestial.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * В NeoForge 1.21.1 у вложений (attachments) нет автосинхронизации (.sync(...) появился позже),
 * поэтому состояние мини-души рассылаем вручную: самому игроку и всем, кто его видит.
 */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class MiniSoulSync {
    private MiniSoulSync() {}

    /** Меняет флаг мини-души и сразу рассылает его клиентам. */
    public static void set(ServerPlayer player, boolean value) {
        player.setData(ModAttachments.MINI_SOUL.get(), value);
        broadcast(player);
    }

    /** Отправляет текущее состояние игрока ему самому и всем, кто его отслеживает. */
    public static void broadcast(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payloadOf(player));
    }

    private static MiniSoulSyncPayload payloadOf(ServerPlayer player) {
        return new MiniSoulSyncPayload(player.getId(), player.getData(ModAttachments.MINI_SOUL.get()));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) broadcast(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) broadcast(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) broadcast(player);
    }

    /** Игрок A увидел игрока B (вошёл в зону видимости) - показываем A душу на плече B. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer watcher && event.getTarget() instanceof ServerPlayer target) {
            PacketDistributor.sendToPlayer(watcher, payloadOf(target));
        }
    }
}
