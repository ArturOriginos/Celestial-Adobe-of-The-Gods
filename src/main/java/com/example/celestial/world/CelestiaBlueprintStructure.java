package com.example.celestial.world;

import com.example.celestial.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Структура мира Селестии из чертежа. Настраивается в worldgen/structure/*.json:
 * blueprint - имя чертежа, max_slope - допустимый перепад высот под зданием,
 * foundation - на сколько блоков вниз достраивать основание, boss_entity/boss_spawn - кто и где появляется внутри.
 */
public class CelestiaBlueprintStructure extends Structure {
    public static final MapCodec<CelestiaBlueprintStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    settingsCodec(instance),
                    Codec.STRING.fieldOf("blueprint").forGetter(s -> s.blueprint),
                    Codec.INT.optionalFieldOf("max_slope", 4).forGetter(s -> s.maxSlope),
                    Codec.INT.optionalFieldOf("foundation", 12).forGetter(s -> s.foundation),
                    Codec.STRING.optionalFieldOf("boss_entity", "").forGetter(s -> s.bossEntity),
                    BlockPos.CODEC.optionalFieldOf("boss_spawn").forGetter(s -> s.bossSpawn)
            ).apply(instance, CelestiaBlueprintStructure::new));

    private final String blueprint;
    private final int maxSlope;
    private final int foundation;
    private final String bossEntity;
    private final Optional<BlockPos> bossSpawn;

    public CelestiaBlueprintStructure(Structure.StructureSettings settings, String blueprint, int maxSlope, int foundation,
                                      String bossEntity, Optional<BlockPos> bossSpawn) {
        super(settings);
        this.blueprint = blueprint;
        this.maxSlope = maxSlope;
        this.foundation = foundation;
        this.bossEntity = bossEntity;
        this.bossSpawn = bossSpawn;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        CelestiaBlueprint bp = CelestiaBlueprint.get(blueprint);
        ChunkPos chunk = context.chunkPos();
        Rotation rotation = Rotation.getRandom(context.random());
        int w = CelestiaBlueprintPiece.width(bp, rotation);
        int d = CelestiaBlueprintPiece.depth(bp, rotation);
        int x0 = chunk.getMinBlockX();
        int z0 = chunk.getMinBlockZ();
        ChunkGenerator gen = context.chunkGenerator();

        // высота земли в сетке точек под зданием
        int n = Math.max(3, Math.min(6, Math.max(w, d) / 8 + 2));
        int[] ys = new int[n * n];
        int k = 0;
        for (int ix = 0; ix < n; ix++) {
            for (int iz = 0; iz < n; iz++) {
                int x = x0 + (w - 1) * ix / (n - 1);
                int z = z0 + (d - 1) * iz / (n - 1);
                int y = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                ys[k++] = y;
            }
        }
        Arrays.sort(ys);
        if (ys[0] < 20) return Optional.empty();                    // край мира / пустота
        if (ys[ys.length - 1] - ys[0] > maxSlope) return Optional.empty();   // слишком рваный рельеф
        int ground = ys[ys.length * 3 / 4];                         // чуть выше медианы: меньше зарываемся в землю
        if (ground + bp.sizeY + 2 >= context.heightAccessor().getMaxBuildHeight()) return Optional.empty();

        BlockPos origin = new BlockPos(x0, ground, z0);             // нижний слой чертежа = уровень земли
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(
                new CelestiaBlueprintPiece(blueprint, origin, rotation, foundation, bossEntity, bossSpawn.orElse(null)))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.CELESTIA_BLUEPRINT.get();
    }
}
