package com.example.celestial.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


public class CelestiaAxeItem extends AxeItem {
    public CelestiaAxeItem(net.minecraft.world.item.Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ShimmerText.of(super.getName(stack));
    }
}
