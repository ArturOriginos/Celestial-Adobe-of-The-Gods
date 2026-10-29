package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.menu.AdvancedEnchantingMenu;
import com.example.celestial.menu.SolarAnvilMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedEnchantingMenu>> ADVANCED_ENCHANTING =
            MENUS.register("advanced_enchanting", () -> IMenuTypeExtension.create(AdvancedEnchantingMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SolarAnvilMenu>> SOLAR_ANVIL =
            MENUS.register("solar_anvil", () -> IMenuTypeExtension.create(SolarAnvilMenu::new));

    private ModMenus() {}
}
