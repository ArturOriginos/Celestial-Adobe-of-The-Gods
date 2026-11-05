package com.example.celestial.world;

import com.example.celestial.registry.ModBlocks;
import com.example.celestial.registry.ModStructures;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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

/** Часть структуры из чертежа: повёрнутое здание, основание под ним и (по желанию) босс внутри. */
public class CelestiaBlueprintPiece extends StructurePiece {
    private final String blueprint;
    private final BlockPos origin;       // минимальный угол (после поворота), нижний слой чертежа
    private final Rotation rotation;
    private final int foundation;
    private final String bossEntity;
    @Nullable
    private final BlockPos bossLocal;    // локальные координаты чертежа, где появляется босс

    public CelestiaBlueprintPiece(String blueprint, BlockPos origin, Rotation rotation, int foundation,
                                  String bossEntity, @Nullable BlockPos bossLocal) {
        super(ModStructures.CELESTIA_BLUEPRINT_PIECE.get(), 0, boxFor(CelestiaBlueprint.get(blueprint), origin, rotation, foundation));
        this.blueprint = blueprint;
        this.origin = origin;
        this.rotation = rotation;
        this.foundation = foundation;
        this.bossEntity = bossEntity;
        this.bossLocal = bossLocal;
    }

    public CelestiaBlueprintPiece(CompoundTag tag) {
        super(ModStructures.CELESTIA_BLUEPRINT_PIECE.get(), tag);
        this.blueprint = tag.getString("bp");
        this.origin = new BlockPos(tag.getInt("ox"), tag.getInt("oy"), tag.getInt("oz"));
        this.rotation = Rotation.values()[Math.floorMod(tag.getInt("rot"), 4)];
        this.foundation = tag.getInt("foundation");
        this.bossEntity = tag.getString("boss");
        this.bossLocal = tag.contains("bossx") ? new BlockPos(tag.getInt("bossx"), tag.getInt("bossy"), tag.getInt("bossz")) : null;
    }

    private static boolean swapped(Rotation r) {
        return r == Rotation.CLOCKWISE_90 || r == Rotation.COUNTERCLOCKWISE_90;
    }

    public static int width(CelestiaBlueprint bp, Rotation r) {
        return swapped(r) ? bp.sizeZ : bp.sizeX;
    }

    public static int depth(CelestiaBlueprint bp, Rotation r) {
        return swapped(r) ? bp.sizeX : bp.sizeZ;
    }

    private static BoundingBox boxFor(CelestiaBlueprint bp, BlockPos o, Rotation r, int foundation) {
        return new BoundingBox(o.getX(), o.getY() - foundation, o.getZ(),
                o.getX() + width(bp, r) - 1, o.getY() + bp.sizeY - 1, o.getZ() + depth(bp, r) - 1);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("bp", blueprint);
        tag.putInt("ox", origin.getX());
        tag.putInt("oy", origin.getY());
        tag.putInt("oz", origin.getZ());
        tag.putInt("rot", rotation.ordinal());
        tag.putInt("foundation", foundation);
        tag.putString("boss", bossEntity);
        if (bossLocal != null) {
            tag.putInt("bossx", bossLocal.getX());
            tag.putInt("bossy", bossLocal.getY());
            tag.putInt("bossz", bossLocal.getZ());
        }
    }

    /** Локальные координаты чертежа -> мировые с учётом поворота. */
    private BlockPos toWorld(CelestiaBlueprint bp, int lx, int ly, int lz) {
        int w = bp.sizeX, d = bp.sizeZ;
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
        CelestiaBlueprint bp = CelestiaBlueprint.get(blueprint);
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState filler = ModBlocks.CELESTIAL_STONE.get().defaultBlockState();

        // 1. расчистить объём здания от рельефа
        for (int[] a : bp.air) {
            BlockPos p = toWorld(bp, a[0], a[1], a[2]);
            if (box.isInside(p) && !level.getBlockState(p).isAir()) level.setBlock(p, air, 2);
        }

        // 2. блоки здания + основание под нижним слоем
        for (CelestiaBlueprint.Entry e : bp.blocks) {
            BlockPos p = toWorld(bp, e.x(), e.y(), e.z());
            if (!box.isInside(p)) continue;
            level.setBlock(p, e.state().rotate(rotation), 2);
            if (e.y() == 0) {
                for (int i = 1; i <= foundation; i++) {
                    BlockPos below = p.below(i);
                    if (!box.isInside(below)) break;
                    BlockState old = level.getBlockState(below);
                    if (!old.isAir() && !old.canBeReplaced() && old.getFluidState().isEmpty()) break;
                    level.setBlock(below, filler, 2);
                }
            }
        }

        // 3. босс появляется один раз - в том чанке, где находится его точка
        if (bossLocal != null && !bossEntity.isEmpty()) {
            BlockPos spot = toWorld(bp, bossLocal.getX(), bossLocal.getY(), bossLocal.getZ());
            if (box.isInside(spot)) {
                ResourceLocation id = ResourceLocation.tryParse(bossEntity);
                if (id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                    EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
                    Entity entity = type.create(level.getLevel());
                    if (entity != null) {
                        entity.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                        if (entity instanceof Mob mob) {
                            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.STRUCTURE, null);
                        }
                        level.addFreshEntity(entity);
                    }
                }
            }
        }
    }
}
