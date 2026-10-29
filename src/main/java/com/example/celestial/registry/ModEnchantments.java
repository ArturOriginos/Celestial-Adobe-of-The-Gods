package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/**
 * Ключи кастомных зачарований. Сами зачарования описаны в data/celestial/enchantment/*.json
 * (в 1.21 зачарования - датапак-реестр), а поведение реализовано в EnchantEvents.
 */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> MINER_SYNDROME = key("miner_syndrome");
    public static final ResourceKey<Enchantment> SLAYER = key("slayer");
    public static final ResourceKey<Enchantment> BLEEDING = key("bleeding");
    public static final ResourceKey<Enchantment> EXPERIENCE_BOOST = key("experience_boost");
    public static final ResourceKey<Enchantment> MAGNET = key("magnet");
    public static final ResourceKey<Enchantment> HELIOS_BLESSING = key("helios_blessing");

    private ModEnchantments() {}

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(MODID, name));
    }

    /** Уровень зачарования на предмете (0, если нет). */
    public static int getLevel(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) return 0;
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchants.isEmpty()) return 0;
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(key)
                .map(enchants::getLevel)
                .orElse(0);
    }
}
