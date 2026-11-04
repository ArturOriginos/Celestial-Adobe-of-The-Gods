package com.example.celestial.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

/**
 * Паксель Богов и Проклятых: мульти-тул (кирка, топор, лопата, мотыга и меч в одном).
 * Мгновенная добыча задаётся в CelestiaEvents, неразрушим (UNBREAKABLE), 50 урона.
 * Как топор умеет обтёсывать брёвна (ПКМ).
 */
public class PaxelItem extends AxeItem {
    public PaxelItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ShimmerText.ofPaxel(super.getName(stack));
    }
}
