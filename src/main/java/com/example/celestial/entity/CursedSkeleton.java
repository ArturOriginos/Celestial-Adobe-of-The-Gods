package com.example.celestial.entity;

import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Проклятый скелет: стреляет из лука, который всегда лежит у него в руках. */
public class CursedSkeleton extends Skeleton {
    public CursedSkeleton(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    }

    /** Страховка: если лук пропал (например, после /summon без экипировки), выдаём новый. */
    @Override
    public void aiStep() {
        if (!this.level().isClientSide && this.isAlive() && this.getMainHandItem().isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
        super.aiStep();
    }
}
