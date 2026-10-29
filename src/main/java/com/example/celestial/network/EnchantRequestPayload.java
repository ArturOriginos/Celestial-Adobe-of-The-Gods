package com.example.celestial.network;

import static com.example.celestial.CelestialMod.MODID;

import io.netty.handler.codec.DecoderException;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Клиент -> сервер: какие зачарования (id -> уровень) игрок хочет наложить. */
public record EnchantRequestPayload(Map<ResourceLocation, Integer> enchants) implements CustomPacketPayload {
    public static final Type<EnchantRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "enchant_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantRequestPayload> STREAM_CODEC =
            StreamCodec.of(EnchantRequestPayload::write, EnchantRequestPayload::read);

    private static final int MAX_ENTRIES = 64;

    private static void write(RegistryFriendlyByteBuf buf, EnchantRequestPayload payload) {
        buf.writeVarInt(payload.enchants.size());
        payload.enchants.forEach((id, level) -> {
            buf.writeResourceLocation(id);
            buf.writeVarInt(level);
        });
    }

    private static EnchantRequestPayload read(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new DecoderException("Too many enchantments in request: " + size);
        }
        Map<ResourceLocation, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(buf.readResourceLocation(), buf.readVarInt());
        }
        return new EnchantRequestPayload(map);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
