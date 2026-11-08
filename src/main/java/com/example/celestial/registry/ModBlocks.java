package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.block.AdvancedEnchantingTableBlock;
import com.example.celestial.block.CelestialGlassBlock;
import com.example.celestial.block.CelestiaPortalBlock;
import com.example.celestial.block.CelestiaPortalFieldBlock;
import com.example.celestial.block.CelestialLeavesBlock;
import com.example.celestial.block.CelestialSaplingBlock;
import com.example.celestial.block.SolarAnvilBlock;
import com.example.celestial.block.CelestialCrystalBlock;
import com.example.celestial.block.CelestialPlantBlock;
import com.example.celestial.block.CursedGlassBlock;
import com.example.celestial.block.CursedHangingLeavesBlock;
import com.example.celestial.block.CursedLogBlock;
import com.example.celestial.block.CursedPlantBlock;
import com.example.celestial.block.CursedSaplingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.SoundType;
import com.example.celestial.item.ShimmerBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredBlock<AdvancedEnchantingTableBlock> ADVANCED_ENCHANTING_TABLE = BLOCKS.registerBlock(
            "advanced_enchanting_table",
            AdvancedEnchantingTableBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 1200.0F)
                    .lightLevel(state -> 7)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> ADVANCED_ENCHANTING_TABLE_ITEM =
            ITEMS.registerSimpleBlockItem(ADVANCED_ENCHANTING_TABLE);

    public static final DeferredBlock<SolarAnvilBlock> SOLAR_ANVIL = BLOCKS.registerBlock(
            "solar_anvil",
            SolarAnvilBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.ANVIL)
                    .lightLevel(state -> 10)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> SOLAR_ANVIL_ITEM = ITEMS.registerSimpleBlockItem(SOLAR_ANVIL);

    // ---------------- Селестийское дерево ----------------
    public static final DeferredBlock<RotatedPillarBlock> CELESTIAL_LOG = BLOCKS.registerBlock(
            "celestial_log", RotatedPillarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(2.0F).sound(SoundType.WOOD).ignitedByLava());

    public static final DeferredBlock<Block> CELESTIAL_PLANKS = BLOCKS.registerBlock(
            "celestial_planks", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());

    public static final DeferredBlock<SlabBlock> CELESTIAL_SLAB = BLOCKS.registerBlock(
            "celestial_slab", SlabBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());

    public static final DeferredBlock<StairBlock> CELESTIAL_STAIRS = BLOCKS.registerBlock(
            "celestial_stairs", props -> new StairBlock(CELESTIAL_PLANKS.get().defaultBlockState(), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());

    public static final DeferredBlock<CelestialLeavesBlock> CELESTIAL_LEAVES = BLOCKS.registerBlock(
            "celestial_leaves", CelestialLeavesBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.2F).randomTicks()
                    .sound(SoundType.GRASS).noOcclusion().ignitedByLava()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));

    public static final DeferredBlock<CelestialSaplingBlock> CELESTIAL_SAPLING = BLOCKS.registerBlock(
            "celestial_sapling", CelestialSaplingBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).noCollission().instabreak()
                    .sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<BlockItem> CELESTIAL_LOG_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_LOG);
    public static final DeferredItem<BlockItem> CELESTIAL_PLANKS_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_PLANKS);
    public static final DeferredItem<BlockItem> CELESTIAL_SLAB_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_SLAB);
    public static final DeferredItem<BlockItem> CELESTIAL_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_STAIRS);
    public static final DeferredItem<BlockItem> CELESTIAL_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_LEAVES);
    public static final DeferredItem<BlockItem> CELESTIAL_SAPLING_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_SAPLING);

    // ---------------- Основа мира (селестийский слой) ----------------
    private static BlockBehaviour.Properties solid(float hardness, float resistance, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(hardness, resistance).sound(sound);
    }

    private static BlockBehaviour.Properties plant(SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).noCollission().instabreak().sound(sound)
                .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY);
    }

    public static final DeferredBlock<Block> CELESTIAL_EARTH = BLOCKS.registerBlock(
            "celestial_earth", Block::new, solid(0.6F, 0.6F, SoundType.GRASS));
    public static final DeferredBlock<Block> CELESTIAL_STONE = BLOCKS.registerBlock(
            "celestial_stone", Block::new, solid(1.5F, 6.0F, SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<ColoredFallingBlock> CELESTIAL_SAND = BLOCKS.registerBlock(
            "celestial_sand", props -> new ColoredFallingBlock(new ColorRGBA(0xE9DDB0), props),
            solid(0.5F, 0.5F, SoundType.SAND));
    public static final DeferredBlock<DropExperienceBlock> CELESTIAL_ORE = BLOCKS.registerBlock(
            "celestial_ore", props -> new DropExperienceBlock(UniformInt.of(3, 7), props),
            solid(3.0F, 3.0F, SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> CELESTIAL_ICE = BLOCKS.registerBlock(
            "celestial_ice", Block::new, solid(0.5F, 0.5F, SoundType.GLASS).friction(0.98F));
    public static final DeferredBlock<Block> CELESTIAL_CLOUD = BLOCKS.registerBlock(
            "celestial_cloud", Block::new, solid(0.3F, 0.3F, SoundType.WOOL));
    public static final DeferredBlock<HalfTransparentBlock> CELESTIAL_CLOUD_FLUFFY = BLOCKS.registerBlock(
            "celestial_cloud_fluffy", HalfTransparentBlock::new,
            solid(0.2F, 0.2F, SoundType.WOOL).noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));
    public static final DeferredBlock<Block> CELESTIAL_GLOWSTONE = BLOCKS.registerBlock(
            "celestial_glowstone", Block::new, solid(0.5F, 0.5F, SoundType.GLASS).lightLevel(state -> 15));

    public static final DeferredBlock<CelestialPlantBlock> CELESTIAL_GRASS = BLOCKS.registerBlock(
            "celestial_grass", CelestialPlantBlock::new, plant(SoundType.GRASS).replaceable());
    public static final DeferredBlock<CelestialPlantBlock> CELESTIAL_FLOWER = BLOCKS.registerBlock(
            "celestial_flower", CelestialPlantBlock::new, plant(SoundType.GRASS));
    public static final DeferredBlock<CelestialPlantBlock> CELESTIAL_MUSHROOM = BLOCKS.registerBlock(
            "celestial_mushroom", CelestialPlantBlock::new, plant(SoundType.GRASS).lightLevel(state -> 4));
    public static final DeferredBlock<CelestialCrystalBlock> CELESTIAL_CRYSTAL = BLOCKS.registerBlock(
            "celestial_crystal", CelestialCrystalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.5F).sound(SoundType.AMETHYST_CLUSTER)
                    .lightLevel(state -> 10).noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<BlockItem> CELESTIAL_EARTH_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_EARTH);
    public static final DeferredItem<BlockItem> CELESTIAL_STONE_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_STONE);
    public static final DeferredItem<BlockItem> CELESTIAL_SAND_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_SAND);
    public static final DeferredItem<ShimmerBlockItem> CELESTIAL_ORE_ITEM = ITEMS.register("celestial_ore",
            () -> new ShimmerBlockItem(CELESTIAL_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CELESTIAL_ICE_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_ICE);
    public static final DeferredItem<BlockItem> CELESTIAL_CLOUD_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_CLOUD);
    public static final DeferredItem<BlockItem> CELESTIAL_CLOUD_FLUFFY_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_CLOUD_FLUFFY);
    public static final DeferredItem<BlockItem> CELESTIAL_GLOWSTONE_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_GLOWSTONE);
    public static final DeferredItem<BlockItem> CELESTIAL_GRASS_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_GRASS);
    public static final DeferredItem<BlockItem> CELESTIAL_FLOWER_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_FLOWER);
    public static final DeferredItem<BlockItem> CELESTIAL_MUSHROOM_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_MUSHROOM);
    public static final DeferredItem<BlockItem> CELESTIAL_CRYSTAL_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_CRYSTAL);

    /** Блок Селестии: ломается только кирко-лопатой Селестии. */
    public static final DeferredBlock<Block> CELESTIA_BLOCK = BLOCKS.registerBlock(
            "celestia_block", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(50.0F, 3600000.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(state -> 10));
    public static final DeferredItem<BlockItem> CELESTIA_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(CELESTIA_BLOCK);

    /** Рамка портала Селестии: ключ в центре, по бокам души, остальное руны. ПКМ ключом по рамке зажигает портал. */
    public static final DeferredBlock<CelestiaPortalBlock> CELESTIA_PORTAL = BLOCKS.registerBlock(
            "celestia_portal", CelestiaPortalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(state -> 15));
    public static final DeferredItem<BlockItem> CELESTIA_PORTAL_ITEM = ITEMS.registerSimpleBlockItem(CELESTIA_PORTAL);

    /** Светящееся поле внутри рамки (предмета нет, создаётся ключом). */
    public static final DeferredBlock<CelestiaPortalFieldBlock> CELESTIA_PORTAL_FIELD = BLOCKS.registerBlock(
            "celestia_portal_field", CelestiaPortalFieldBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).noCollission().strength(-1.0F)
                    .sound(SoundType.AMETHYST).lightLevel(state -> 11).noLootTable()
                    .pushReaction(PushReaction.BLOCK));

    // ============ Проклятый Нифельхельм (beta 1.3) ============
    private static BlockBehaviour.Properties nether(float hardness, float resistance, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(hardness, resistance).sound(sound);
    }

    private static BlockBehaviour.Properties netherPlant(SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().instabreak().sound(sound)
                .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY);
    }

    // --- основа мира ---
    public static final DeferredBlock<Block> CURSED_STONE = BLOCKS.registerBlock(
            "cursed_stone", Block::new, nether(1.5F, 6.0F, SoundType.NETHERRACK).requiresCorrectToolForDrops());
    public static final DeferredBlock<RotatedPillarBlock> CURSED_BASALT = BLOCKS.registerBlock(
            "cursed_basalt", RotatedPillarBlock::new, nether(1.25F, 4.2F, SoundType.BASALT).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> CURSED_BRICKS = BLOCKS.registerBlock(
            "cursed_bricks", Block::new, nether(2.0F, 6.0F, SoundType.NETHER_BRICKS).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> CURSED_RUNE_STONE = BLOCKS.registerBlock(
            "cursed_rune_stone", Block::new,
            nether(2.5F, 8.0F, SoundType.NETHER_BRICKS).requiresCorrectToolForDrops().lightLevel(state -> 8));
    public static final DeferredBlock<Block> CURSED_ICE = BLOCKS.registerBlock(
            "cursed_ice", Block::new, nether(0.8F, 1.0F, SoundType.GLASS).friction(0.98F));
    public static final DeferredBlock<Block> CURSED_GLOWSTONE = BLOCKS.registerBlock(
            "cursed_glowstone", Block::new, nether(0.5F, 0.5F, SoundType.GLASS).lightLevel(state -> 15));
    public static final DeferredBlock<Block> CURSED_FOLIAGE = BLOCKS.registerBlock(
            "cursed_foliage", Block::new, nether(0.4F, 0.4F, SoundType.WART_BLOCK));

    // --- растения и кристаллы ---
    public static final DeferredBlock<CursedPlantBlock> CURSED_GRASS = BLOCKS.registerBlock(
            "cursed_grass", CursedPlantBlock::new, netherPlant(SoundType.NETHER_SPROUTS).replaceable());
    public static final DeferredBlock<CursedPlantBlock> CURSED_FLOWER = BLOCKS.registerBlock(
            "cursed_flower", CursedPlantBlock::new, netherPlant(SoundType.NETHER_SPROUTS).lightLevel(state -> 6));
    public static final DeferredBlock<CursedPlantBlock> CURSED_MUSHROOM = BLOCKS.registerBlock(
            "cursed_mushroom", CursedPlantBlock::new, netherPlant(SoundType.FUNGUS).lightLevel(state -> 6));
    public static final DeferredBlock<CursedPlantBlock> HELL_BUSH = BLOCKS.registerBlock(
            "hell_bush", CursedPlantBlock::new, netherPlant(SoundType.NETHER_SPROUTS));
    public static final DeferredBlock<CursedPlantBlock> BLAZING_FLOWER = BLOCKS.registerBlock(
            "blazing_flower", CursedPlantBlock::new, netherPlant(SoundType.NETHER_SPROUTS).lightLevel(state -> 10));
    public static final DeferredBlock<CelestialCrystalBlock> CURSED_CRYSTAL = BLOCKS.registerBlock(
            "cursed_crystal", CelestialCrystalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.5F).sound(SoundType.AMETHYST_CLUSTER)
                    .lightLevel(state -> 12).noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<CelestialCrystalBlock> CRYSTAL_SPROUTS = BLOCKS.registerBlock(
            "crystal_sprouts", CelestialCrystalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.3F).sound(SoundType.AMETHYST_CLUSTER)
                    .lightLevel(state -> 8).noOcclusion().pushReaction(PushReaction.DESTROY));

    // --- проклятое дерево ---
    public static final DeferredBlock<CursedLogBlock> CURSED_LOG = BLOCKS.registerBlock(
            "cursed_log", CursedLogBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0F).sound(SoundType.NETHER_WOOD));
    public static final DeferredBlock<RotatedPillarBlock> CURSED_STRIPPED_LOG = BLOCKS.registerBlock(
            "cursed_stripped_log", RotatedPillarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0F).sound(SoundType.NETHER_WOOD));
    public static final DeferredBlock<Block> CURSED_PLANKS = BLOCKS.registerBlock(
            "cursed_planks", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0F, 3.0F).sound(SoundType.NETHER_WOOD));
    public static final DeferredBlock<SlabBlock> CURSED_SLAB = BLOCKS.registerBlock(
            "cursed_slab", SlabBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0F, 3.0F).sound(SoundType.NETHER_WOOD));
    public static final DeferredBlock<StairBlock> CURSED_STAIRS = BLOCKS.registerBlock(
            "cursed_stairs", props -> new StairBlock(CURSED_PLANKS.get().defaultBlockState(), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0F, 3.0F).sound(SoundType.NETHER_WOOD));
    public static final DeferredBlock<FenceBlock> CURSED_FENCE = BLOCKS.registerBlock(
            "cursed_fence", FenceBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.0F, 3.0F).sound(SoundType.NETHER_WOOD));
    public static final DeferredBlock<LeavesBlock> CURSED_LEAVES = BLOCKS.registerBlock(
            "cursed_leaves", LeavesBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.2F).randomTicks()
                    .sound(SoundType.WART_BLOCK).noOcclusion().lightLevel(state -> 4)
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));
    public static final DeferredBlock<CursedHangingLeavesBlock> CURSED_HANGING_LEAVES = BLOCKS.registerBlock(
            "cursed_hanging_leaves", CursedHangingLeavesBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().instabreak()
                    .sound(SoundType.WEEPING_VINES).lightLevel(state -> 5).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<CursedSaplingBlock> CURSED_SAPLING = BLOCKS.registerBlock(
            "cursed_sapling", CursedSaplingBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollission().instabreak()
                    .sound(SoundType.NETHER_SPROUTS).pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<BlockItem> CURSED_STONE_ITEM = ITEMS.registerSimpleBlockItem(CURSED_STONE);
    public static final DeferredItem<BlockItem> CURSED_BASALT_ITEM = ITEMS.registerSimpleBlockItem(CURSED_BASALT);
    public static final DeferredItem<BlockItem> CURSED_BRICKS_ITEM = ITEMS.registerSimpleBlockItem(CURSED_BRICKS);
    public static final DeferredItem<BlockItem> CURSED_RUNE_STONE_ITEM = ITEMS.registerSimpleBlockItem(CURSED_RUNE_STONE);
    public static final DeferredItem<BlockItem> CURSED_ICE_ITEM = ITEMS.registerSimpleBlockItem(CURSED_ICE);
    public static final DeferredItem<BlockItem> CURSED_GLOWSTONE_ITEM = ITEMS.registerSimpleBlockItem(CURSED_GLOWSTONE);
    public static final DeferredItem<BlockItem> CURSED_FOLIAGE_ITEM = ITEMS.registerSimpleBlockItem(CURSED_FOLIAGE);
    public static final DeferredItem<BlockItem> CURSED_GRASS_ITEM = ITEMS.registerSimpleBlockItem(CURSED_GRASS);
    public static final DeferredItem<BlockItem> CURSED_FLOWER_ITEM = ITEMS.registerSimpleBlockItem(CURSED_FLOWER);
    public static final DeferredItem<BlockItem> CURSED_MUSHROOM_ITEM = ITEMS.registerSimpleBlockItem(CURSED_MUSHROOM);
    public static final DeferredItem<BlockItem> HELL_BUSH_ITEM = ITEMS.registerSimpleBlockItem(HELL_BUSH);
    public static final DeferredItem<BlockItem> BLAZING_FLOWER_ITEM = ITEMS.registerSimpleBlockItem(BLAZING_FLOWER);
    public static final DeferredItem<BlockItem> CURSED_CRYSTAL_ITEM = ITEMS.registerSimpleBlockItem(CURSED_CRYSTAL);
    public static final DeferredItem<BlockItem> CRYSTAL_SPROUTS_ITEM = ITEMS.registerSimpleBlockItem(CRYSTAL_SPROUTS);
    public static final DeferredItem<BlockItem> CURSED_LOG_ITEM = ITEMS.registerSimpleBlockItem(CURSED_LOG);
    public static final DeferredItem<BlockItem> CURSED_STRIPPED_LOG_ITEM = ITEMS.registerSimpleBlockItem(CURSED_STRIPPED_LOG);
    public static final DeferredItem<BlockItem> CURSED_PLANKS_ITEM = ITEMS.registerSimpleBlockItem(CURSED_PLANKS);
    public static final DeferredItem<BlockItem> CURSED_SLAB_ITEM = ITEMS.registerSimpleBlockItem(CURSED_SLAB);
    public static final DeferredItem<BlockItem> CURSED_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(CURSED_STAIRS);
    public static final DeferredItem<BlockItem> CURSED_FENCE_ITEM = ITEMS.registerSimpleBlockItem(CURSED_FENCE);
    public static final DeferredItem<BlockItem> CURSED_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(CURSED_LEAVES);
    public static final DeferredItem<BlockItem> CURSED_HANGING_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(CURSED_HANGING_LEAVES);
    public static final DeferredItem<BlockItem> CURSED_SAPLING_ITEM = ITEMS.registerSimpleBlockItem(CURSED_SAPLING);

    // --- селестийский кирпич, стекло, ступеньки и плиты (beta 1.8) ---
    public static final DeferredBlock<Block> CELESTIAL_BRICKS = BLOCKS.registerBlock(
            "celestial_bricks", Block::new, solid(1.5F, 6.0F, SoundType.STONE).requiresCorrectToolForDrops().lightLevel(s -> 4));
    public static final DeferredBlock<StairBlock> CELESTIAL_BRICK_STAIRS = BLOCKS.registerBlock(
            "celestial_brick_stairs", props -> new StairBlock(CELESTIAL_BRICKS.get().defaultBlockState(), props),
            solid(1.5F, 6.0F, SoundType.STONE).requiresCorrectToolForDrops().lightLevel(s -> 4));
    public static final DeferredBlock<SlabBlock> CELESTIAL_BRICK_SLAB = BLOCKS.registerBlock(
            "celestial_brick_slab", SlabBlock::new, solid(1.5F, 6.0F, SoundType.STONE).requiresCorrectToolForDrops().lightLevel(s -> 4));
    public static final DeferredBlock<CelestialGlassBlock> CELESTIAL_GLASS = BLOCKS.registerBlock(
            "celestial_glass", CelestialGlassBlock::new,
            solid(0.3F, 0.3F, SoundType.GLASS).noOcclusion().lightLevel(s -> 7)
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));
    public static final DeferredItem<BlockItem> CELESTIAL_BRICKS_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_BRICKS);
    public static final DeferredItem<BlockItem> CELESTIAL_BRICK_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> CELESTIAL_BRICK_SLAB_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_BRICK_SLAB);
    public static final DeferredItem<BlockItem> CELESTIAL_GLASS_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_GLASS);

    // --- проклятый незерский кирпич, стекло, ступеньки и плиты (beta 1.5) ---
    public static final DeferredBlock<Block> CURSED_NETHER_BRICKS = BLOCKS.registerBlock(
            "cursed_nether_bricks", Block::new, nether(2.0F, 6.0F, SoundType.NETHER_BRICKS).requiresCorrectToolForDrops());
    public static final DeferredBlock<StairBlock> CURSED_NETHER_BRICK_STAIRS = BLOCKS.registerBlock(
            "cursed_nether_brick_stairs", props -> new StairBlock(CURSED_NETHER_BRICKS.get().defaultBlockState(), props),
            nether(2.0F, 6.0F, SoundType.NETHER_BRICKS).requiresCorrectToolForDrops());
    public static final DeferredBlock<SlabBlock> CURSED_NETHER_BRICK_SLAB = BLOCKS.registerBlock(
            "cursed_nether_brick_slab", SlabBlock::new, nether(2.0F, 6.0F, SoundType.NETHER_BRICKS).requiresCorrectToolForDrops());
    public static final DeferredBlock<CursedGlassBlock> CURSED_GLASS = BLOCKS.registerBlock(
            "cursed_glass", CursedGlassBlock::new,
            nether(0.3F, 0.3F, SoundType.GLASS).noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));

    public static final DeferredItem<BlockItem> CURSED_NETHER_BRICKS_ITEM = ITEMS.registerSimpleBlockItem(CURSED_NETHER_BRICKS);
    public static final DeferredItem<BlockItem> CURSED_NETHER_BRICK_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(CURSED_NETHER_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> CURSED_NETHER_BRICK_SLAB_ITEM = ITEMS.registerSimpleBlockItem(CURSED_NETHER_BRICK_SLAB);
    public static final DeferredItem<BlockItem> CURSED_GLASS_ITEM = ITEMS.registerSimpleBlockItem(CURSED_GLASS);


    // --- проклятый камень и глубинный сланец (ступеньки, плиты, кирпичи) ---
    public static final DeferredBlock<Block> CURSED_STONE_BRICKS = BLOCKS.registerBlock(
            "cursed_stone_bricks", Block::new, nether(1.5F, 6.0F, SoundType.STONE_BRICKS).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> CURSED_STONE_BRICKS_ITEM = ITEMS.registerSimpleBlockItem(CURSED_STONE_BRICKS);
    public static final DeferredBlock<Block> CURSED_DEEPSLATE = BLOCKS.registerBlock(
            "cursed_deepslate", Block::new, nether(3.0F, 6.0F, SoundType.DEEPSLATE).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> CURSED_DEEPSLATE_ITEM = ITEMS.registerSimpleBlockItem(CURSED_DEEPSLATE);
    public static final DeferredBlock<Block> CURSED_DEEPSLATE_BRICKS = BLOCKS.registerBlock(
            "cursed_deepslate_bricks", Block::new, nether(3.5F, 6.0F, SoundType.DEEPSLATE_BRICKS).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> CURSED_DEEPSLATE_BRICKS_ITEM = ITEMS.registerSimpleBlockItem(CURSED_DEEPSLATE_BRICKS);
    public static final DeferredBlock<StairBlock> CURSED_STONE_STAIRS = BLOCKS.registerBlock(
            "cursed_stone_stairs", props -> new StairBlock(CURSED_STONE.get().defaultBlockState(), props),
            nether(1.5F, 6.0F, SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> CURSED_STONE_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(CURSED_STONE_STAIRS);
    public static final DeferredBlock<SlabBlock> CURSED_STONE_SLAB = BLOCKS.registerBlock(
            "cursed_stone_slab", SlabBlock::new, nether(1.5F, 6.0F, SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> CURSED_STONE_SLAB_ITEM = ITEMS.registerSimpleBlockItem(CURSED_STONE_SLAB);
    public static final DeferredBlock<StairBlock> CURSED_STONE_BRICK_STAIRS = BLOCKS.registerBlock(
            "cursed_stone_brick_stairs", props -> new StairBlock(CURSED_STONE_BRICKS.get().defaultBlockState(), props),
            nether(1.5F, 6.0F, SoundType.STONE_BRICKS).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> CURSED_STONE_BRICK_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(CURSED_STONE_BRICK_STAIRS);
    public static final DeferredBlock<StairBlock> CURSED_DEEPSLATE_STAIRS = BLOCKS.registerBlock(
            "cursed_deepslate_stairs", props -> new StairBlock(CURSED_DEEPSLATE.get().defaultBlockState(), props),
            nether(3.0F, 6.0F, SoundType.DEEPSLATE).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> CURSED_DEEPSLATE_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(CURSED_DEEPSLATE_STAIRS);
    public static final DeferredBlock<SlabBlock> CURSED_DEEPSLATE_SLAB = BLOCKS.registerBlock(
            "cursed_deepslate_slab", SlabBlock::new, nether(3.0F, 6.0F, SoundType.DEEPSLATE).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> CURSED_DEEPSLATE_SLAB_ITEM = ITEMS.registerSimpleBlockItem(CURSED_DEEPSLATE_SLAB);
    public static final DeferredBlock<StairBlock> CURSED_DEEPSLATE_BRICK_STAIRS = BLOCKS.registerBlock(
            "cursed_deepslate_brick_stairs", props -> new StairBlock(CURSED_DEEPSLATE_BRICKS.get().defaultBlockState(), props),
            nether(3.5F, 6.0F, SoundType.DEEPSLATE_BRICKS).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> CURSED_DEEPSLATE_BRICK_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(CURSED_DEEPSLATE_BRICK_STAIRS);

    private ModBlocks() {}
}
