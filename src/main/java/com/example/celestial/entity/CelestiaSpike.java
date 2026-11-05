package com.example.celestial.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Световой кристалл Бога Селестии: бьёт сильнее шипов Хранителя и Проклятого, растёт со стадией боя. */
public class CelestiaSpike extends GodSpike {
    public static final float BASE_DAMAGE = 36.0F; // 18 сердец

    public CelestiaSpike(EntityType<? extends GodSpike> type, Level level) {
        super(type, level);
    }

    @Override
    protected float damage() {
        LivingEntity owner = getSpikeOwner();
        float mult = owner instanceof CelestiaGod god ? god.damageMultiplier() : 1.0F;
        return BASE_DAMAGE * mult;
    }
}
