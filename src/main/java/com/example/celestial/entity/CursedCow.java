package com.example.celestial.entity;

import com.example.celestial.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;

/** Проклятая версия: поведение ванильного животного, свой вид, текстура и детёныши своего вида. */
public class CursedCow extends Cow {
    public CursedCow(EntityType<? extends Cow> type, Level level) {
        super(type, level);
    }

    @Override
    public Cow getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.CURSED_COW.get().create(level);
    }
}
