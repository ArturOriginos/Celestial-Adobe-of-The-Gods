package com.example.celestial.world;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.GuardianOfGods;
import com.example.celestial.registry.ModBlocks;
import com.example.celestial.registry.ModEntities;
import com.example.celestial.registry.ModStructures;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/** Единственная часть структуры: рисует свою долю дерева в каждом чанке, который пересекает. */
public class GiantTreePiece extends StructurePiece {
    private static final ResourceKey<LootTable> CHEST_LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(MODID, "chests/giant_tree"));

    private final BlockPos base;
    private final int height;
    private final long seed;

    public GiantTreePiece(BlockPos base, int height, long seed) {
        super(ModStructures.GIANT_TREE_PIECE.get(), 0, new BoundingBox(
                base.getX() - 26, base.getY() - 8, base.getZ() - 26,
                base.getX() + 26, base.getY() + height + 24, base.getZ() + 26));
        this.base = base;
        this.height = height;
        this.seed = seed;
    }

    public GiantTreePiece(CompoundTag tag) {
        super(ModStructures.GIANT_TREE_PIECE.get(), tag);
        this.base = new BlockPos(tag.getInt("bx"), tag.getInt("by"), tag.getInt("bz"));
        this.height = tag.getInt("h");
        this.seed = tag.getLong("seed");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("bx", base.getX());
        tag.putInt("by", base.getY());
        tag.putInt("bz", base.getZ());
        tag.putInt("h", height);
        tag.putLong("seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        GiantTreeBlueprint bp = GiantTreeBlueprint.build(base, height, seed);

        for (Map.Entry<Long, BlockState> e : bp.logs.entrySet()) put(level, box, BlockPos.of(e.getKey()), e.getValue());
        BlockState airState = Blocks.AIR.defaultBlockState();
        for (long l : bp.air) put(level, box, BlockPos.of(l), airState);
        BlockState planks = ModBlocks.CELESTIAL_PLANKS.get().defaultBlockState();
        for (long l : bp.floor) put(level, box, BlockPos.of(l), planks);
        BlockState light = ModBlocks.CELESTIAL_GLOWSTONE.get().defaultBlockState();
        for (BlockPos p : bp.lights) put(level, box, p, light);

        BlockState leaves = ModBlocks.CELESTIAL_LEAVES.get().defaultBlockState();
        for (Map.Entry<Long, Integer> e : bp.leaves.entrySet()) {
            BlockPos p = BlockPos.of(e.getKey());
            if (!box.isInside(p)) continue;
            BlockState old = level.getBlockState(p);
            if (old.isAir() || old.canBeReplaced() || old.is(BlockTags.LEAVES)) {
                level.setBlock(p, leaves.setValue(LeavesBlock.DISTANCE, e.getValue()), 2);
            }
        }

        for (Map.Entry<BlockPos, net.minecraft.core.Direction> e : bp.chests.entrySet()) {
            BlockPos p = e.getKey();
            if (!box.isInside(p)) continue;
            level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, e.getValue()), 2);
            RandomizableContainer.setBlockEntityLootTable(level, random, p, CHEST_LOOT);
        }

        // Хранитель Богов появляется один раз - в том чанке, где центр комнаты
        BlockPos g = bp.guardianPos;
        if (box.isInside(g)) {
            GuardianOfGods guardian = ModEntities.GUARDIAN.get().create(level.getLevel());
            if (guardian != null) {
                guardian.moveTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                guardian.finalizeSpawn(level, level.getCurrentDifficultyAt(g), MobSpawnType.STRUCTURE, null);
                level.addFreshEntity(guardian);
            }
        }
    }

    private static void put(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) level.setBlock(pos, state, 2);
    }
}
