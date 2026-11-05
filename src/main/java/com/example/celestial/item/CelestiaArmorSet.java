package com.example.celestial.item;

import com.example.celestial.registry.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

/** Полный комплект брони Селестии: нагрудник + ботинки. */
public final class CelestiaArmorSet {
    private CelestiaArmorSet() {}

    public static boolean hasFullSet(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CELESTIA_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.CELESTIA_BOOTS.get());
    }
}
