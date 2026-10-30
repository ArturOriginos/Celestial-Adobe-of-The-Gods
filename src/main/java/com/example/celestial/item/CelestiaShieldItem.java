package com.example.celestial.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


public class CelestiaShieldItem extends ShieldItem {
    public CelestiaShieldItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ShimmerText.of(super.getName(stack));
    }
}
