package com.example.celestial.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Багровый шип Проклятого: бьёт сильнее золотого шипа Хранителя. */
public class CursedSpike extends GodSpike {
    public CursedSpike(EntityType<? extends GodSpike> type, Level level) {
        super(type, level);
    }

    @Override
    protected float damage() {
        return 22.0F; // 11 сердец
    }
}
