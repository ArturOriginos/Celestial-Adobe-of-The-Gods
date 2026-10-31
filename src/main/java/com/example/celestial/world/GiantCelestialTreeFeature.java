package com.example.celestial.world;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Заглушка совместимости: ничего не генерирует. Настоящее дерево - структура GiantTreeStructure.
 * Нужна, чтобы устаревшие файлы worldgen старых версий мода не ломали загрузку мира.
 */
public class GiantCelestialTreeFeature extends Feature<NoneFeatureConfiguration> {
    public GiantCelestialTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return false;
    }
}
