package com.example.celestial.world;

import com.example.celestial.registry.ModBlocks;
import com.example.celestial.registry.ModEntities;
import com.example.celestial.registry.ModStructures;
import com.example.celestial.entity.CursedBoss;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** «Церковь Проклятых»: чертёж, повёрнутый на 0/90/180/270 градусов, и Проклятый в главном зале. */
public class CursedChurchPiece extends StructurePiece {
    /** Насколько глубоко под полом достраивается фундамент, если снизу пустота. */
    public static final int FOUNDATION = 20;

    private final BlockPos origin;      // минимальный угол (после поворота)
    private final Rotation rotation;

    public CursedChurchPiece(BlockPos origin, Rotation rotation) {
        super(ModStructures.CURSED_CHURCH_PIECE.get(), 0, boxFor(origin, rotation));
        this.origin = origin;
        this.rotation = rotation;
    }

    public CursedChurchPiece(CompoundTag tag) {
        super(ModStructures.CURSED_CHURCH_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getInt("ox"), tag.getInt("oy"), tag.getInt("oz"));
        this.rotation = Rotation.values()[Math.floorMod(tag.getInt("rot"), 4)];
    }

    public static int width(Rotation r) {
        return (r == Rotation.CLOCKWISE_90 || r == Rotation.COUNTERCLOCKWISE_90)
                ? CursedChurchBlueprint.SIZE_Z : CursedChurchBlueprint.SIZE_X;
    }

    public static int depth(Rotation r) {
        return (r == Rotation.CLOCKWISE_90 || r == Rotation.COUNTERCLOCKWISE_90)
                ? CursedChurchBlueprint.SIZE_X : CursedChurchBlueprint.SIZE_Z;
    }

    private static BoundingBox boxFor(BlockPos o, Rotation r) {
        return new BoundingBox(o.getX(), o.getY() - FOUNDATION, o.getZ(),
                o.getX() + width(r) - 1, o.getY() + CursedChurchBlueprint.SIZE_Y - 1, o.getZ() + depth(r) - 1);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("ox", origin.getX());
        tag.putInt("oy", origin.getY());
        tag.putInt("oz", origin.getZ());
        tag.putInt("rot", rotation.ordinal());
    }

    /** Локальные координаты чертежа -> мировые с учётом поворота. */
    private BlockPos toWorld(int lx, int ly, int lz) {
        int w = CursedChurchBlueprint.SIZE_X, d = CursedChurchBlueprint.SIZE_Z;
        int x, z;
        switch (rotation) {
            case CLOCKWISE_90 -> { x = d - 1 - lz; z = lx; }
            case CLOCKWISE_180 -> { x = w - 1 - lx; z = d - 1 - lz; }
            case COUNTERCLOCKWISE_90 -> { x = lz; z = w - 1 - lx; }
            default -> { x = lx; z = lz; }
        }
        return origin.offset(x, ly, z);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        CursedChurchBlueprint bp = CursedChurchBlueprint.get();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState filler = ModBlocks.CURSED_STONE.get().defaultBlockState();

        // 1. расчистить объём здания от нетерака
        for (int[] a : bp.air) {
            BlockPos p = toWorld(a[0], a[1], a[2]);
            if (box.isInside(p) && !level.getBlockState(p).isAir()) level.setBlock(p, air, 2);
        }

        // 2. блоки здания + фундамент под нижним слоем
        for (CursedChurchBlueprint.Entry e : bp.blocks) {
            BlockPos p = toWorld(e.x(), e.y(), e.z());
            if (!box.isInside(p)) continue;
            level.setBlock(p, e.state().rotate(rotation), 2);
            if (e.y() == 0) {
                for (int i = 1; i <= FOUNDATION; i++) {
                    BlockPos below = p.below(i);
                    if (!box.isInside(below)) break;
                    BlockState old = level.getBlockState(below);
                    if (!old.isAir() && !old.canBeReplaced() && old.getFluidState().isEmpty()) break;
                    level.setBlock(below, filler, 2);
                }
            }
        }

        // 3. Проклятый появляется один раз, в том чанке, где стоит его место
        BlockPos boss = toWorld(CursedChurchBlueprint.BOSS_X, CursedChurchBlueprint.BOSS_Y, CursedChurchBlueprint.BOSS_Z);
        if (box.isInside(boss)) {
            CursedBoss cursed = ModEntities.CURSED_BOSS.get().create(level.getLevel());
            if (cursed != null) {
                cursed.moveTo(boss.getX() + 0.5, boss.getY(), boss.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                cursed.finalizeSpawn(level, level.getCurrentDifficultyAt(boss), MobSpawnType.STRUCTURE, null);
                level.addFreshEntity(cursed);
            }
        }
    }
}
