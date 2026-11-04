package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.world.CelestiaChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MODID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<CelestiaChunkGenerator>> CELESTIA =
            CHUNK_GENERATORS.register("celestia", () -> CelestiaChunkGenerator.CODEC);

    private ModChunkGenerators() {}
}
