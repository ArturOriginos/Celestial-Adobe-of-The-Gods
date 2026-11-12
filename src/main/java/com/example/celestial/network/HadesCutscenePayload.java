package com.example.celestial.network;

import static com.example.celestial.CelestialMod.MODID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Сервер -> клиент: катсцена появления Аида.
 *  START - камера на Аида (entityId) на ticks тиков;
 *  LINE  - показать реплику (speaker и text - ключи перевода) на ticks тиков;
 *  STOP  - вернуть камеру игроку.
 */
public record HadesCutscenePayload(int action, int entityId, int ticks, String speaker, String text)
        implements CustomPacketPayload {
    public static final int START = 0, LINE = 1, STOP = 2;

    public static final Type<HadesCutscenePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "hades_cutscene"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HadesCutscenePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HadesCutscenePayload::action,
            ByteBufCodecs.VAR_INT, HadesCutscenePayload::entityId,
            ByteBufCodecs.VAR_INT, HadesCutscenePayload::ticks,
            ByteBufCodecs.STRING_UTF8, HadesCutscenePayload::speaker,
            ByteBufCodecs.STRING_UTF8, HadesCutscenePayload::text,
            HadesCutscenePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
