package com.example.celestial.world;

import com.example.celestial.AEConfig;
import com.example.celestial.registry.ModBlocks;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Распространение биома «Проклятый Нифельхельм» по верхнему миру.
 * Заражённые чанки хранятся в данных мира. Каждый шаг граница расширяется на соседние чанки: у них меняется биом
 * и камень: обычный камень и глубинный сланец заменяются проклятыми вариантами, поверхность темнеет, растения гибнут.
 * Если чанк ещё не загружен, он будет изменён в момент загрузки.
 */
public class NiflheimSpread extends SavedData {
    private static final String NAME = "celestial_niflheim";
    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final LongOpenHashSet infected = new LongOpenHashSet();
    private final LongOpenHashSet pending = new LongOpenHashSet();   // заражены, но ещё не изменены (чанк не загружен)
    private final LongArrayList frontier = new LongArrayList();      // чанки, у которых могут быть чистые соседи
    private boolean active;

    public static NiflheimSpread get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(NiflheimSpread::new, NiflheimSpread::load, DataFixTypes.LEVEL), NAME);
    }

    private static NiflheimSpread load(CompoundTag tag, HolderLookup.Provider registries) {
        NiflheimSpread data = new NiflheimSpread();
        for (long c : tag.getLongArray("Infected")) data.infected.add(c);
        for (long c : tag.getLongArray("Pending")) data.pending.add(c);
        data.active = tag.getBoolean("Active");
        for (long c : data.infected) {
            if (data.hasCleanNeighbor(c)) data.frontier.add(c);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("Infected", infected.toLongArray());
        tag.putLongArray("Pending", pending.toLongArray());
        tag.putBoolean("Active", active);
        return tag;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        setDirty();
    }

    public int infectedCount() {
        return infected.size();
    }

    public boolean isPending(ChunkPos pos) {
        return !pending.isEmpty() && pending.contains(pos.toLong());
    }

    public void markDone(ChunkPos pos) {
        if (pending.remove(pos.toLong())) setDirty();
    }

    private boolean hasCleanNeighbor(long c) {
        int cx = ChunkPos.getX(c), cz = ChunkPos.getZ(c);
        for (int[] d : DIRS) {
            if (!infected.contains(ChunkPos.asLong(cx + d[0], cz + d[1]))) return true;
        }
        return false;
    }

    /** Заражает чанк; если он загружен, сразу меняет биом и поверхность. */
    public boolean infect(ServerLevel level, long chunk) {
        if (!infected.add(chunk)) return false;
        pending.add(chunk);
        frontier.add(chunk);
        setDirty();
        LevelChunk loaded = level.getChunkSource().getChunkNow(ChunkPos.getX(chunk), ChunkPos.getZ(chunk));
        if (loaded != null) {
            corrupt(level, loaded, true);
            pending.remove(chunk);
        }
        return true;
    }

    /** Один шаг распространения: граница расширяется на несколько случайных соседних чанков. */
    public void step(ServerLevel level) {
        if (!active) return;
        int perStep = AEConfig.SPREAD_CHUNKS_PER_STEP.get();
        int max = AEConfig.SPREAD_MAX_CHUNKS.get();
        for (int i = 0; i < perStep && !frontier.isEmpty() && infected.size() < max; i++) {
            int idx = level.random.nextInt(frontier.size());
            long c = frontier.getLong(idx);
            int cx = ChunkPos.getX(c), cz = ChunkPos.getZ(c);
            long[] options = new long[4];
            int n = 0;
            for (int[] d : DIRS) {
                long nb = ChunkPos.asLong(cx + d[0], cz + d[1]);
                if (!infected.contains(nb)) options[n++] = nb;
            }
            if (n == 0) { // окружён: убираем из границы
                frontier.set(idx, frontier.getLong(frontier.size() - 1));
                frontier.removeLong(frontier.size() - 1);
                continue;
            }
            infect(level, options[level.random.nextInt(n)]);
        }
    }

    /**
     * Меняет биом всего чанка и портит его: весь обычный камень и глубинный сланец заменяются проклятыми
     * вариантами, поверхность (земля, песок, гравий) становится проклятым камнем, растения и листва гибнут.
     * @param sync true - чанк уже у игроков (после изменений он отправляется им заново); false - чанк только что загружен.
     */
    public static void corrupt(ServerLevel level, LevelChunk chunk, boolean sync) {
        Holder<Biome> biome = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(CursedSurfaceFeature.BIOME);
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, level.getChunkSource().randomState().sampler());

        BlockState cursedStone = ModBlocks.CURSED_STONE.get().defaultBlockState();
        BlockState cursedDeepslate = ModBlocks.CURSED_DEEPSLATE.get().defaultBlockState();
        BlockState basalt = ModBlocks.CURSED_BASALT.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // 1) весь камень и глубинный сланец -> проклятые варианты (напрямую в секциях: быстро, свет не меняется)
        for (LevelChunkSection section : chunk.getSections()) {
            if (section.hasOnlyAir() || !section.maybeHas(NiflheimSpread::isReplaceableRock)) continue;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.is(Blocks.DEEPSLATE)) {
                            section.setBlockState(x, y, z, cursedDeepslate, false);
                        } else if (state.is(Blocks.STONE) || state.is(Blocks.GRANITE)
                                || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE)) {
                            section.setBlockState(x, y, z, cursedStone, false);
                        }
                    }
                }
            }
        }

        // 2) поверхность: земля/песок/гравий -> проклятый камень, растения и листва гибнут
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                int solid = 0;
                int bottom = Math.max(level.getMinBuildHeight(), top - 70);
                for (int y = top - 1; y >= bottom && solid < 3; y--) {
                    pos.set(minX + x, y, minZ + z);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (state.getFluidState().isEmpty() && (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)
                            || state.is(BlockTags.REPLACEABLE) || state.is(BlockTags.CROPS))) {
                        chunk.setBlockState(pos, air, false); // растения и листва гибнут
                    } else if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)
                            || state.is(Blocks.SNOW_BLOCK)) {
                        boolean exposedBasalt = solid == 0 && level.random.nextInt(100) < 8;
                        chunk.setBlockState(pos, exposedBasalt ? basalt : cursedStone, false);
                        solid++;
                    } else if (state.is(cursedStone.getBlock()) || state.is(cursedDeepslate.getBlock())) {
                        if (solid == 0 && level.random.nextInt(100) < 8) chunk.setBlockState(pos, basalt, false);
                        solid++;
                    }
                }
            }
        }

        chunk.setUnsaved(true);
        if (sync) {
            // заново отправляем чанк игрокам, которые его видят (блоки + биом сразу)
            ClientboundLevelChunkWithLightPacket packet =
                    new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null);
            for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false)) {
                player.connection.send(packet);
            }
        }
    }

    private static boolean isReplaceableRock(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE);
    }
}
