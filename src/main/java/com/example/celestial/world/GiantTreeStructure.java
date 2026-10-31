package com.example.celestial.world;

import com.example.celestial.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** Гигантское Селестийское дерево как структура: ищется командой /locate structure celestial:giant_celestial_tree. */
public class GiantTreeStructure extends Structure {
    public static final MapCodec<GiantTreeStructure> CODEC = simpleCodec(GiantTreeStructure::new);

    public GiantTreeStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getMiddleBlockX();
        int cz = chunk.getMiddleBlockZ();
        var gen = context.chunkGenerator();
        int ground = gen.getFirstOccupiedHeight(cx, cz, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
        int surface = gen.getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (ground != surface) return Optional.empty(); // под водой
        for (int[] o : new int[][]{{12, 0}, {-12, 0}, {0, 12}, {0, -12}}) {
            int h = gen.getFirstOccupiedHeight(cx + o[0], cz + o[1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
            if (Math.abs(h - ground) > 8) return Optional.empty(); // слишком крутой склон
        }
        int height = 44 + context.random().nextInt(8);
        if (ground + height + 28 >= context.heightAccessor().getMaxBuildHeight()) return Optional.empty();
        long seed = context.random().nextLong();
        BlockPos base = new BlockPos(cx, ground, cz);
        return Optional.of(new GenerationStub(base, builder -> builder.addPiece(new GiantTreePiece(base, height, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.GIANT_TREE.get();
    }
}
