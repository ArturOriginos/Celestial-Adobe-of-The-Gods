package com.example.celestial.world;

import com.example.celestial.CelestialMod;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;

/** Генератор мира Селестии: облачное море и парящие острова (см. {@link CelestiaTerrain}). */
public class CelestiaChunkGenerator extends ChunkGenerator {
    public static final MapCodec<CelestiaChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.getBiomeSource()))
                    .apply(instance, CelestiaChunkGenerator::new));

    private volatile CelestiaTerrain terrain;

    public CelestiaChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    /** Сид рельефа берётся из RandomState мира, то есть он свой у каждого мира. */
    private CelestiaTerrain terrain(RandomState randomState) {
        CelestiaTerrain t = terrain;
        if (t == null) {
            long seed = randomState.getOrCreateRandomFactory(
                    ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "celestia_terrain")).at(0, 0, 0).nextLong();
            t = new CelestiaTerrain(seed);
            terrain = t;
        }
        return t;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // пещер нет
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        // поверхность уже построена в fillFromNoise
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        ChunkPos chunkPos = level.getCenter();
        Holder<Biome> biome = level.getBiome(chunkPos.getWorldPosition().atY(level.getMaxBuildHeight() - 1));
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        random.setDecorationSeed(level.getSeed(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration(level, biome, chunkPos, random);
    }

    @Override
    public int getGenDepth() {
        return CelestiaTerrain.HEIGHT;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        CelestiaTerrain t = terrain(randomState);
        ChunkPos cp = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = cp.getMinBlockX() + lx;
                int z = cp.getMinBlockZ() + lz;
                t.fillColumn(x, z, (y, state) -> chunk.setBlockState(pos.set(x, y, z), state, false));
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return CelestiaTerrain.MIN_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return terrain(randomState).topHeight(x, z) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        BlockState[] states = new BlockState[level.getHeight()];
        java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        int minY = level.getMinBuildHeight();
        terrain(randomState).fillColumn(x, z, (y, state) -> {
            int i = y - minY;
            if (i >= 0 && i < states.length) states[i] = state;
        });
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Celestia terrain top: " + terrain(randomState).topHeight(pos.getX(), pos.getZ()));
    }
}
