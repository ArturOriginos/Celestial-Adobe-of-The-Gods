package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.block.AdvancedEnchantingTableBlock;
import com.example.celestial.block.CelestialLeavesBlock;
import com.example.celestial.block.CelestialSaplingBlock;
import com.example.celestial.block.SolarAnvilBlock;
import com.example.celestial.block.CelestialCrystalBlock;
import com.example.celestial.block.CelestialPlantBlock;
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
import net.minecraft.world.item.BlockItem;
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
                    .lightLevel(state -> 7));

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
    public static final DeferredItem<BlockItem> CELESTIAL_ORE_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_ORE);
    public static final DeferredItem<BlockItem> CELESTIAL_ICE_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_ICE);
    public static final DeferredItem<BlockItem> CELESTIAL_CLOUD_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_CLOUD);
    public static final DeferredItem<BlockItem> CELESTIAL_CLOUD_FLUFFY_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_CLOUD_FLUFFY);
    public static final DeferredItem<BlockItem> CELESTIAL_GLOWSTONE_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_GLOWSTONE);
    public static final DeferredItem<BlockItem> CELESTIAL_GRASS_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_GRASS);
    public static final DeferredItem<BlockItem> CELESTIAL_FLOWER_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_FLOWER);
    public static final DeferredItem<BlockItem> CELESTIAL_MUSHROOM_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_MUSHROOM);
    public static final DeferredItem<BlockItem> CELESTIAL_CRYSTAL_ITEM = ITEMS.registerSimpleBlockItem(CELESTIAL_CRYSTAL);

    private ModBlocks() {}
}
