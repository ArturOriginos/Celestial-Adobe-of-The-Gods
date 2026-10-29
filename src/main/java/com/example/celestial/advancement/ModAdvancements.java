package com.example.celestial.advancement;

import static com.example.celestial.CelestialMod.MODID;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Выдача достижений из кода (критерий "trigger" у достижений с minecraft:impossible). */
public final class ModAdvancements {
    private ModAdvancements() {}

    private static AdvancementHolder find(ServerPlayer player, String id) {
        return player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(MODID, id));
    }

    public static boolean isDone(ServerPlayer player, String id) {
        AdvancementHolder holder = find(player, id);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    public static void grant(ServerPlayer player, String id) {
        AdvancementHolder holder = find(player, id);
        if (holder != null) player.getAdvancements().award(holder, "trigger");
    }
}
