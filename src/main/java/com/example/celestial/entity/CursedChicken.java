package com.example.celestial.entity;

import com.example.celestial.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;

/** Проклятая версия: поведение ванильного животного, свой вид, текстура и детёныши своего вида. */
public class CursedChicken extends Chicken {
    public CursedChicken(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
    }

    @Override
    public Chicken getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.CURSED_CHICKEN.get().create(level);
    }
}
