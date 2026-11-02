package com.example.celestial.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

/** Предмет проклятых: название переливается в красно-чёрных тонах. */
public class CursedPickShovelItem extends ShovelItem {
    public CursedPickShovelItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ShimmerText.ofCursed(super.getName(stack));
    }
}
