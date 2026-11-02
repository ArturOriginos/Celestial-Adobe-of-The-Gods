package com.example.celestial.world;

import com.example.celestial.CelestialMod;
import com.example.celestial.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Превращает незерак в биоме «Проклятый Нифельхельм» в незерский камень (а местами в базальт).
 * Работает в пределах чанка, для которого запущена генерация.
 */
public class CursedSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "cursed_niflheim"));

    public CursedSurfaceFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockState stone = ModBlocks.CURSED_STONE.get().defaultBlockState();
        BlockState basalt = ModBlocks.CURSED_BASALT.get().defaultBlockState();

        int minY = Math.max(level.getMinBuildHeight(), 0);
        int maxY = Math.min(level.getMaxBuildHeight(), 128);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean changed = false;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(origin.getX() + x, y, origin.getZ() + z);
                    if (!level.getBlockState(pos).is(Blocks.NETHERRACK)) continue;
                    if (!level.getBiome(pos).is(BIOME)) continue;
                    boolean exposed = level.getBlockState(pos.above()).isAir();
                    level.setBlock(pos, exposed && context.random().nextInt(100) < 8 ? basalt : stone, 2);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
