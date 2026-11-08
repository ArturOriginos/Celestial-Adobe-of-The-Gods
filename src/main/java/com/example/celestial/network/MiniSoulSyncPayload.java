package com.example.celestial.network;

import static com.example.celestial.CelestialMod.MODID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Сервер -> клиент: есть ли у игрока (по id сущности) мини-душа на плече. */
public record MiniSoulSyncPayload(int entityId, boolean value) implements CustomPacketPayload {
    public static final Type<MiniSoulSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "mini_soul_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MiniSoulSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MiniSoulSyncPayload::entityId,
            ByteBufCodecs.BOOL, MiniSoulSyncPayload::value,
            MiniSoulSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
