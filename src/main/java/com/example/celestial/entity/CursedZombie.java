package com.example.celestial.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Проклятый зомби: ведёт себя как обычный зомби, но не превращается в утопленника. */
public class CursedZombie extends Zombie {
    public CursedZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }
}
