package com.example.celestial.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Предмет-блок с переливающимся названием (Селестийская руда). */
public class ShimmerBlockItem extends BlockItem {
    public ShimmerBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ShimmerText.of(super.getName(stack));
    }
}
