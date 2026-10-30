package com.example.celestial.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


public class CelestiaShearsItem extends ShearsItem {
    public CelestiaShearsItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ShimmerText.of(super.getName(stack));
    }
}
