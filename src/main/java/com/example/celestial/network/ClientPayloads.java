package com.example.celestial.network;

import com.example.celestial.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Обработчики пакетов на стороне клиента. */
public final class ClientPayloads {
    private ClientPayloads() {}

    public static void handleMiniSoul(MiniSoulSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity entity = mc.level.getEntity(payload.entityId());
            // на входе в мир собственный игрок может быть ещё не добавлен в уровень
            if (entity == null && mc.player != null && mc.player.getId() == payload.entityId()) {
                entity = mc.player;
            }
            if (entity instanceof Player player) {
                player.setData(ModAttachments.MINI_SOUL.get(), payload.value());
            }
        });
    }
}
