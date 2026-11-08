package com.example.celestial.network;

import static com.example.celestial.CelestialMod.MODID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Сервер -> клиент: открыть Всезнающую книгу (текст читается на клиенте из языкового файла). */
public record OpenTomePayload() implements CustomPacketPayload {
    public static final Type<OpenTomePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "open_tome"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTomePayload> STREAM_CODEC =
            StreamCodec.unit(new OpenTomePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
