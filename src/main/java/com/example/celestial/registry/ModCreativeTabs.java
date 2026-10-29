package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Отдельная вкладка "Celestial: Abode of the Gods" со всеми предметами мода. */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CELESTIAL = TABS.register("celestial",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.celestial"))
                    .icon(() -> new ItemStack(ModItems.CELESTIA_KEY.get()))
                    .displayItems((params, output) -> {
                        ModBlocks.ITEMS.getEntries().forEach(entry -> output.accept(entry.get()));
                        ModItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get()));
                    })
                    .build());

    private ModCreativeTabs() {}
}
