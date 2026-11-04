package com.example.celestial.world;

import com.example.celestial.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** «Церковь Проклятых» в биоме «Проклятый Нифельхельм»: /locate structure celestial:cursed_church. */
public class CursedChurchStructure extends Structure {
    public static final MapCodec<CursedChurchStructure> CODEC = simpleCodec(CursedChurchStructure::new);

    public CursedChurchStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    private static boolean solid(NoiseColumn column, int y) {
        BlockState s = column.getBlock(y);
        return !s.isAir() && s.getFluidState().isEmpty();
    }

    /** Высота пола (первый твёрдый блок с воздухом над ним, считая сверху вниз под потолком Незера) или -1. */
    private static int floorY(ChunkGenerator gen, GenerationContext ctx, int x, int z) {
        NoiseColumn column = gen.getBaseColumn(x, z, ctx.heightAccessor(), ctx.randomState());
        for (int y = 92; y >= 34; y--) {
            if (solid(column, y) && column.getBlock(y + 1).isAir() && column.getBlock(y + 2).isAir()) return y;
        }
        return -1;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        Rotation rotation = Rotation.getRandom(context.random());
        int w = CursedChurchPiece.width(rotation);
        int d = CursedChurchPiece.depth(rotation);
        int x0 = chunk.getMinBlockX();
        int z0 = chunk.getMinBlockZ();
        ChunkGenerator gen = context.chunkGenerator();

        // высота земли в 9 точках под зданием
        int[] ys = new int[9];
        int n = 0;
        for (int ix = 0; ix < 3; ix++) {
            for (int iz = 0; iz < 3; iz++) {
                int y = floorY(gen, context, x0 + (w - 1) * ix / 2, z0 + (d - 1) * iz / 2);
                if (y < 0) return Optional.empty();
                ys[n++] = y;
            }
        }
        Arrays.sort(ys);
        if (ys[8] - ys[0] > 18) return Optional.empty();          // слишком рваный рельеф
        int floor = ys[4];                                          // медиана
        int baseY = floor + 1;                                      // нижний слой здания - пол на уровне земли
        if (baseY + CursedChurchBlueprint.SIZE_Y > 120) return Optional.empty();   // под потолком Незера

        BlockPos origin = new BlockPos(x0, baseY - 1, z0);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new CursedChurchPiece(origin, rotation))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CURSED_CHURCH.get();
    }
}
