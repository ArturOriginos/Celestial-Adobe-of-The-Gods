package com.example.celestial.jei;

import net.minecraft.world.item.ItemStack;

/** Одна запись Солнечной наковальни для JEI: предмет + катализатор (душа/книга) => результат за N уровней. */
public record SolarAnvilJeiRecipe(ItemStack tool, ItemStack catalyst, ItemStack result, int cost) {}
