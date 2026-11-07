package com.example.celestial.entity;

import com.example.celestial.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.Level;

/** Проклятая версия: поведение ванильного животного, свой вид, текстура и детёныши своего вида. */
public class CursedSheep extends Sheep {
    public CursedSheep(EntityType<? extends Sheep> type, Level level) {
        super(type, level);
    }

    @Override
    public Sheep getBreedOffspring(ServerLevel level, AgeableMob partner) {
        CursedSheep lamb = ModEntities.CURSED_SHEEP.get().create(level);
        if (lamb != null) lamb.setColor(this.getColor());
        return lamb;
    }
}
