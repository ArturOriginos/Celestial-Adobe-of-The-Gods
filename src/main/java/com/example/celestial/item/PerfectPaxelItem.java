package com.example.celestial.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

/**
 * Совершенный Паксель: те же свойства, что у Пакселя Богов и Проклятых (мгновенная добыча,
 * обтёсывание брёвен, бесконечная прочность), но с уроном 250. Делается в Солнечной наковальне
 * из Пакселя Богов и Проклятых и Души Бога Селестии.
 */
public class PerfectPaxelItem extends PaxelItem {
    public PerfectPaxelItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ShimmerText.ofPerfectPaxel(Component.translatable(this.getDescriptionId(stack)));
    }
}
