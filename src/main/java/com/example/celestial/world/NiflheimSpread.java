package com.example.celestial.world;

import com.example.celestial.AEConfig;
import com.example.celestial.registry.ModBlocks;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.MinecraftServer;
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
     * вариантами, трава становится проклятым дёрном, земля - проклятой землёй, песок и гравий - проклятым камнем, растения и листва гибнут.
     * @param sync true - чанк уже у игроков (после изменений он отправляется им заново); false - чанк только что загружен.
     */
    public static void corrupt(ServerLevel level, LevelChunk chunk, boolean sync) {
        Holder<Biome> biome = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(CursedSurfaceFeature.BIOME);
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, level.getChunkSource().randomState().sampler());

        BlockState cursedStone = ModBlocks.CURSED_STONE.get().defaultBlockState();
        BlockState cursedDeepslate = ModBlocks.CURSED_DEEPSLATE.get().defaultBlockState();
        BlockState basalt = ModBlocks.CURSED_BASALT.get().defaultBlockState();
        BlockState earth = ModBlocks.CURSED_EARTH.get().defaultBlockState();
        BlockState turf = ModBlocks.CURSED_TURF.get().defaultBlockState();
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

        // 1б) ВСЯ трава и ВСЯ земля чанка (не только у поверхности): трава под открытым небом -> дёрн, остальное -> земля
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos dirtPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(NiflheimSpread::isDirtLike)) continue;
            int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
            int bx = chunk.getPos().getMinBlockX(), bz = chunk.getPos().getMinBlockZ();
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!isDirtLike(state)) continue;
                        BlockState replacement = earth;
                        if (isGrassLike(state)) {
                            dirtPos.set(bx + x, baseY + y + 1, bz + z);
                            BlockState above = chunk.getBlockState(dirtPos);
                            if (above.isAir() || !above.canOcclude()) replacement = turf;
                        }
                        section.setBlockState(x, y, z, replacement, false);
                    }
                }
            }
        }

        // 2) поверхность: трава -> проклятый дёрн, земля -> проклятая земля, песок/гравий -> проклятый камень, растения и листва гибнут
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // ChunkAccess.getHeight возвращает Y самого верхнего блока, а не первого свободного над ним
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
                int solid = 0;
                int bottom = Math.max(level.getMinBuildHeight(), top - 70);
                for (int y = top - 1; y >= bottom && solid < 3; y--) {
                    pos.set(minX + x, y, minZ + z);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (state.getFluidState().isEmpty() && (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)
                            || state.is(BlockTags.REPLACEABLE) || state.is(BlockTags.CROPS))) {
                        chunk.setBlockState(pos, air, false); // растения и листва гибнут
                    } else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)) {
                        // трава -> проклятый дёрн (под самой поверхностью - проклятая земля)
                        chunk.setBlockState(pos, solid == 0 ? turf : earth, false);
                        solid++;
                    } else if (state.is(BlockTags.DIRT)) {
                        // земля -> проклятая земля
                        chunk.setBlockState(pos, earth, false);
                        solid++;
                    } else if (state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK)) {
                        boolean exposedBasalt = solid == 0 && level.random.nextInt(100) < 8;
                        chunk.setBlockState(pos, exposedBasalt ? basalt : cursedStone, false);
                        solid++;
                    } else if (state.is(earth.getBlock()) || state.is(turf.getBlock())) {
                        solid++; // уже проклято
                    } else if (state.is(cursedStone.getBlock()) || state.is(cursedDeepslate.getBlock())) {
                        if (solid == 0 && level.random.nextInt(100) < 8) chunk.setBlockState(pos, basalt, false);
                        solid++;
                    }
                }
            }
        }

        // 3) проклятая флора на проклятом дёрне: трава, цветы, грибы, кусты, кристаллы и саженцы проклятого дерева
        spawnFlora(level, chunk, earth, turf, cursedStone);

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

    /** Доля столбцов чанка, где появляется флора (около 15 растений на чанк). */
    private static final float FLORA_CHANCE = 0.06F;
    /** true - саженец сразу становится выросшим деревом, false - на земле остаётся сам саженец (растёт от костной муки). */
    private static final boolean SAPLING_GROWS_INSTANTLY = true;

    /**
     * Проклятая флора для заражённых чанков: те же растения, что и в Нифельхельме Незера. Растёт только на проклятом
     * дёрне, на самой поверхности (раньше растения ставились в верхний блок земли, а не над ним, и не появлялись вовсе).
     */
    private static void spawnFlora(ServerLevel level, LevelChunk chunk, BlockState earth, BlockState turf, BlockState cursedStone) {
        RandomSource rnd = level.random;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minX = chunk.getPos().getMinBlockX(), minZ = chunk.getPos().getMinBlockZ();
        CursedTreeGenerator.TreeWorld treeWorld = new CursedTreeGenerator.TreeWorld() {
            @Override
            public boolean hasChunk(BlockPos p) {
                return (p.getX() >> 4) == chunk.getPos().x && (p.getZ() >> 4) == chunk.getPos().z
                        && p.getY() >= level.getMinBuildHeight() && p.getY() < level.getMaxBuildHeight();
            }

            @Override
            public BlockState get(BlockPos p) {
                return chunk.getBlockState(p);
            }

            @Override
            public void set(BlockPos p, BlockState state) {
                chunk.setBlockState(p, state, false);
            }
        };
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (rnd.nextFloat() >= FLORA_CHANCE) continue;
                // getHeight возвращает Y самого верхнего блока: растение ставится на блок выше, а землёй служит верхний блок
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (top <= level.getMinBuildHeight() + 1 || top + 1 >= level.getMaxBuildHeight()) continue;
                pos.set(minX + x, top + 1, minZ + z);
                if (!chunk.getBlockState(pos).isAir()) continue;
                if (!chunk.getBlockState(pos.below()).is(turf.getBlock())) continue; // только на проклятом дёрне

                int roll = rnd.nextInt(34); // веса как у флоры в Нифельхельме Незера
                if (roll >= 31) { // саженец проклятого дерева
                    if (!SAPLING_GROWS_INSTANTLY) {
                        chunk.setBlockState(pos, ModBlocks.CURSED_SAPLING.get().defaultBlockState(), false);
                        continue;
                    }
                    // сразу выросшее дерево (в чанке: маленькое подальше от края, среднее - ближе к центру)
                    boolean inner = x >= 4 && x <= 11 && z >= 4 && z <= 11;
                    if (!inner) continue;
                    boolean center = x >= 6 && x <= 9 && z >= 6 && z <= 9;
                    int size = center && rnd.nextInt(3) == 0 ? CursedTreeGenerator.MEDIUM : CursedTreeGenerator.SMALL;
                    CursedTreeGenerator.grow(treeWorld, pos.immutable(), rnd, size);
                    continue;
                }
                var block = roll < 14 ? ModBlocks.CURSED_GRASS.get()
                        : roll < 17 ? ModBlocks.CURSED_FLOWER.get()
                        : roll < 20 ? ModBlocks.CURSED_MUSHROOM.get()
                        : roll < 24 ? ModBlocks.HELL_BUSH.get()
                        : roll < 26 ? ModBlocks.BLAZING_FLOWER.get()
                        : roll < 28 ? ModBlocks.CURSED_CRYSTAL.get()
                        : ModBlocks.CRYSTAL_SPROUTS.get();
                chunk.setBlockState(pos, block.defaultBlockState(), false);
            }
        }
    }

    /** Трава и подобные верхние слои: становятся проклятым дёрном, если над ними открытое место. */
    private static boolean isGrassLike(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.DIRT_PATH) || state.is(Blocks.FARMLAND) || state.is(Blocks.MOSS_BLOCK);
    }

    /** Любая земля: трава, земля, коренная земля, грязь и так далее. */
    private static boolean isDirtLike(BlockState state) {
        return isGrassLike(state) || state.is(BlockTags.DIRT) || state.is(Blocks.MUD)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.COARSE_DIRT);
    }

    /** Запуск распространения возле точки появления в мире (например, после победы над Богом Селестии). */
    public static void startAtWorldSpawn(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        NiflheimSpread data = get(overworld);
        ChunkPos c = new ChunkPos(overworld.getSharedSpawnPos());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                data.infect(overworld, ChunkPos.asLong(c.x + dx, c.z + dz));
            }
        }
        data.setActive(true);
    }

    private static boolean isReplaceableRock(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE);
    }
}
