package com.example.celestial.network;

import static com.example.celestial.CelestialMod.MODID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Сервер -> клиент: есть ли в небе глаза Аида (animate - плавно появиться, а не сразу). */
public record SkyEyesPayload(boolean on, boolean animate) implements CustomPacketPayload {
    public static final Type<SkyEyesPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "sky_eyes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkyEyesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SkyEyesPayload::on,
            ByteBufCodecs.BOOL, SkyEyesPayload::animate,
            SkyEyesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
