package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.world.CursedSurfaceFeature;
import com.example.celestial.world.CursedIslandFeature;
import com.example.celestial.world.CursedTreeConfig;
import com.example.celestial.world.CursedTreeFeature;
import com.example.celestial.world.GiantCelestialTreeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MODID);

    /** Заглушка совместимости со старыми версиями (ничего не генерирует). */
    public static final DeferredHolder<Feature<?>, GiantCelestialTreeFeature> LEGACY_GIANT_TREE =
            FEATURES.register("giant_celestial_tree", () -> new GiantCelestialTreeFeature(NoneFeatureConfiguration.CODEC));

    /** Проклятое дерево (размер задаётся конфигом: 0 / 1 / 2). */
    public static final DeferredHolder<Feature<?>, CursedTreeFeature> CURSED_TREE =
            FEATURES.register("cursed_tree", () -> new CursedTreeFeature(CursedTreeConfig.CODEC));
    public static final DeferredHolder<Feature<?>, CursedSurfaceFeature> CURSED_SURFACE =
            FEATURES.register("cursed_surface", () -> new CursedSurfaceFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, CursedIslandFeature> CURSED_ISLAND =
            FEATURES.register("cursed_island", () -> new CursedIslandFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {}
}
