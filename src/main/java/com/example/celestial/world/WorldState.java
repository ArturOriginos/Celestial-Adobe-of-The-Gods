package com.example.celestial.world;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Глобальные изменения мира после убийства боссов (хранятся в данных обычного мира):
 *  skyEyes - глаза Аида в небе (после Всезнающего Ангела),
 *  eternalNight - вечная ночь (после Фенрира),
 *  cerberusSlain - Цербер повержен: Проклятый Нифельхельм распространяется вдвое быстрее.
 */
public class WorldState extends SavedData {
    private static final String NAME = "celestial_world_state";

    private boolean skyEyes;
    private boolean eternalNight;
    private boolean cerberusSlain;

    public static WorldState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WorldState::new, WorldState::load, DataFixTypes.LEVEL), NAME);
    }

    private static WorldState load(CompoundTag tag, HolderLookup.Provider registries) {
        WorldState s = new WorldState();
        s.skyEyes = tag.getBoolean("SkyEyes");
        s.eternalNight = tag.getBoolean("EternalNight");
        s.cerberusSlain = tag.getBoolean("CerberusSlain");
        return s;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("SkyEyes", skyEyes);
        tag.putBoolean("EternalNight", eternalNight);
        tag.putBoolean("CerberusSlain", cerberusSlain);
        return tag;
    }

    public boolean skyEyes() {
        return skyEyes;
    }

    public boolean eternalNight() {
        return eternalNight;
    }

    public boolean cerberusSlain() {
        return cerberusSlain;
    }

    public void setCerberusSlain(boolean value) {
        if (cerberusSlain != value) {
            cerberusSlain = value;
            setDirty();
        }
    }

    public void setSkyEyes(boolean value) {
        if (skyEyes != value) {
            skyEyes = value;
            setDirty();
        }
    }

    public void setEternalNight(boolean value) {
        if (eternalNight != value) {
            eternalNight = value;
            setDirty();
        }
    }
}
