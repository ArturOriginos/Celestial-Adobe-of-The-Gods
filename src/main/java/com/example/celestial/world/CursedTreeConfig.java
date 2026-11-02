package com.example.celestial.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/** size: 0 - маленькое, 1 - среднее, 2 - большое дерево. */
public record CursedTreeConfig(int size) implements FeatureConfiguration {
    public static final Codec<CursedTreeConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("size").forGetter(CursedTreeConfig::size)).apply(i, CursedTreeConfig::new));
}
