package com.example.celestial.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Тир инструментов Селестии. Прочность не используется: предметы неразрушимы (компонент UNBREAKABLE). */
public record CelestiaTier(float speed) implements Tier {
    public static final CelestiaTier SWORD = new CelestiaTier(18.0F);
    public static final CelestiaTier AXE = new CelestiaTier(18.0F);   // 2x незерит (9.0)
    public static final CelestiaTier PICK = new CelestiaTier(18.0F);

    @Override
    public int getUses() {
        return 2031;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return 0.0F;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; // пустой тег: подходит ко всему
    }

    @Override
    public int getEnchantmentValue() {
        return 25;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
}
