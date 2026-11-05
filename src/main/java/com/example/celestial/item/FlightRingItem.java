package com.example.celestial.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Кольцо полёта богов: пока лежит в инвентаре, игрок летает как в креативе (логика в FlightRingEvents). */
public class FlightRingItem extends Item {
    public FlightRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.celestial.ring_of_gods_flight.desc"));
    }
}
