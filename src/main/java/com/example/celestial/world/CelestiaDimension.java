package com.example.celestial.world;

import com.example.celestial.CelestialMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Общие константы измерения «Селестия». */
public final class CelestiaDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "celestia"));

    /** Размер мира: 50000 x 50000 блоков (от -25000 до +25000 по X и Z). */
    public static final int WORLD_SIZE = 50000;
    public static final int WORLD_HALF = WORLD_SIZE / 2;

    /** Высота поверхности центрального (стартового) острова: здесь строится портал обратно. */
    public static final int SPAWN_TOP_Y = 130;

    private CelestiaDimension() {}
}
