package com.example.celestial.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/** Щит проклятых: возвращает урон атакующему (см. CursedEvents). */
public class CursedShieldItem extends ShieldItem {
    public CursedShieldItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ShimmerText.ofCursed(super.getName(stack));
    }
}
