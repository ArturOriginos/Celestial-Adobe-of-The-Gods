package com.example.celestial.world;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Игроки, которым после убийства Бога Селестии задан вопрос "Вы готовы перейти во 2 фазу?" и которые ещё не ответили.
 * Хранится в данных мира, поэтому вопрос не теряется при выходе из игры или перезапуске сервера.
 */
public class Phase2Pending extends SavedData {
    private static final String NAME = "celestial_phase2";

    /** Где погиб бог (оттуда вылетит душа). */
    public record Entry(String dimension, double x, double y, double z) {}

    private final Map<UUID, Entry> pending = new HashMap<>();

    public static Phase2Pending get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(Phase2Pending::new, Phase2Pending::load, DataFixTypes.LEVEL), NAME);
    }

    private static Phase2Pending load(CompoundTag tag, HolderLookup.Provider registries) {
        Phase2Pending data = new Phase2Pending();
        ListTag list = tag.getList("Pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            data.pending.put(e.getUUID("Id"),
                    new Entry(e.getString("Dim"), e.getDouble("X"), e.getDouble("Y"), e.getDouble("Z")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Entry> en : pending.entrySet()) {
            CompoundTag e = new CompoundTag();
            e.putUUID("Id", en.getKey());
            e.putString("Dim", en.getValue().dimension());
            e.putDouble("X", en.getValue().x());
            e.putDouble("Y", en.getValue().y());
            e.putDouble("Z", en.getValue().z());
            list.add(e);
        }
        tag.put("Pending", list);
        return tag;
    }

    public void put(UUID id, Entry entry) {
        pending.put(id, entry);
        setDirty();
    }

    @Nullable
    public Entry get(UUID id) {
        return pending.get(id);
    }

    public boolean has(UUID id) {
        return pending.containsKey(id);
    }

    public void remove(UUID id) {
        if (pending.remove(id) != null) setDirty();
    }
}
