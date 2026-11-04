package com.example.celestial;

import com.example.celestial.menu.AdvancedEnchantingMenu;
import com.example.celestial.network.EnchantRequestPayload;
import com.example.celestial.registry.ModBlocks;
import com.example.celestial.entity.CursedBoss;
import com.example.celestial.entity.GuardianOfGods;
import com.example.celestial.registry.ModChunkGenerators;
import com.example.celestial.registry.ModCreativeTabs;
import com.example.celestial.registry.ModEffects;
import com.example.celestial.registry.ModEntities;
import com.example.celestial.registry.ModFeatures;
import com.example.celestial.registry.ModStructures;
import com.example.celestial.registry.ModItems;
import com.example.celestial.registry.ModMenus;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(CelestialMod.MODID)
public class CelestialMod {
    public static final String MODID = "celestial";

    public CelestialMod(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.ITEMS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        ModChunkGenerators.CHUNK_GENERATORS.register(modBus);
        ModStructures.STRUCTURE_TYPES.register(modBus);
        ModStructures.PIECE_TYPES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModMenus.MENUS.register(modBus);

        modBus.addListener(this::registerPayloads);
        modBus.addListener(this::addCreativeTab);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::commonSetup);

        // SERVER-конфиг синхронизируется с клиентом автоматически
        container.registerConfig(ModConfig.Type.SERVER, AEConfig.SPEC);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                EnchantRequestPayload.TYPE,
                EnchantRequestPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof net.minecraft.server.level.ServerPlayer sp
                            && sp.containerMenu instanceof AdvancedEnchantingMenu menu) {
                        menu.tryEnchant(sp, payload.enchants());
                    }
                }));
    }

    /** Дерево Селестии горит как обычное. */
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            fire.setFlammable(ModBlocks.CELESTIAL_LOG.get(), 5, 5);
            fire.setFlammable(ModBlocks.CELESTIAL_PLANKS.get(), 5, 20);
            fire.setFlammable(ModBlocks.CELESTIAL_SLAB.get(), 5, 20);
            fire.setFlammable(ModBlocks.CELESTIAL_STAIRS.get(), 5, 20);
            fire.setFlammable(ModBlocks.CELESTIAL_LEAVES.get(), 30, 60);
            fire.setFlammable(ModBlocks.CELESTIAL_GRASS.get(), 60, 100);
            fire.setFlammable(ModBlocks.CELESTIAL_FLOWER.get(), 60, 100);
        });
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GUARDIAN.get(), GuardianOfGods.createAttributes().build());
        event.put(ModEntities.CURSED_BOSS.get(), CursedBoss.createAttributes().build());
    }

    private void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.ADVANCED_ENCHANTING_TABLE_ITEM);
            event.accept(ModBlocks.SOLAR_ANVIL_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.ADVANCED_BOOK);
            event.accept(ModItems.GUARDIAN_SOUL);
            event.accept(ModItems.CURSED_SOUL);
            event.accept(ModItems.UNITED_SOUL);
            event.accept(ModItems.CELESTIA_RUNE);
            event.accept(ModItems.CELESTIA_KEY);
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModBlocks.CELESTIAL_LOG_ITEM);
            event.accept(ModBlocks.CELESTIAL_LEAVES_ITEM);
            event.accept(ModBlocks.CELESTIAL_SAPLING_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModBlocks.CELESTIAL_EARTH_ITEM);
            event.accept(ModBlocks.CELESTIAL_STONE_ITEM);
            event.accept(ModBlocks.CELESTIAL_SAND_ITEM);
            event.accept(ModBlocks.CELESTIAL_ORE_ITEM);
            event.accept(ModBlocks.CELESTIAL_ICE_ITEM);
            event.accept(ModBlocks.CELESTIAL_CLOUD_ITEM);
            event.accept(ModBlocks.CELESTIAL_CLOUD_FLUFFY_ITEM);
            event.accept(ModBlocks.CELESTIAL_GLOWSTONE_ITEM);
            event.accept(ModBlocks.CELESTIAL_GRASS_ITEM);
            event.accept(ModBlocks.CELESTIAL_FLOWER_ITEM);
            event.accept(ModBlocks.CELESTIAL_MUSHROOM_ITEM);
            event.accept(ModBlocks.CELESTIAL_CRYSTAL_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.CELESTIAL_LOG_ITEM);
            event.accept(ModBlocks.CELESTIAL_PLANKS_ITEM);
            event.accept(ModBlocks.CELESTIAL_SLAB_ITEM);
            event.accept(ModBlocks.CELESTIAL_STAIRS_ITEM);
            event.accept(ModBlocks.CELESTIA_BLOCK_ITEM);
            event.accept(ModBlocks.CELESTIA_PORTAL_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModBlocks.CURSED_LOG_ITEM);
            event.accept(ModBlocks.CURSED_LEAVES_ITEM);
            event.accept(ModBlocks.CURSED_SAPLING_ITEM);
            event.accept(ModBlocks.CURSED_HANGING_LEAVES_ITEM);
            event.accept(ModBlocks.CURSED_STONE_ITEM);
            event.accept(ModBlocks.CURSED_BASALT_ITEM);
            event.accept(ModBlocks.CURSED_ICE_ITEM);
            event.accept(ModBlocks.CURSED_FOLIAGE_ITEM);
            event.accept(ModBlocks.CURSED_GRASS_ITEM);
            event.accept(ModBlocks.CURSED_FLOWER_ITEM);
            event.accept(ModBlocks.CURSED_MUSHROOM_ITEM);
            event.accept(ModBlocks.HELL_BUSH_ITEM);
            event.accept(ModBlocks.BLAZING_FLOWER_ITEM);
            event.accept(ModBlocks.CURSED_CRYSTAL_ITEM);
            event.accept(ModBlocks.CRYSTAL_SPROUTS_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.CURSED_LOG_ITEM);
            event.accept(ModBlocks.CURSED_STRIPPED_LOG_ITEM);
            event.accept(ModBlocks.CURSED_PLANKS_ITEM);
            event.accept(ModBlocks.CURSED_SLAB_ITEM);
            event.accept(ModBlocks.CURSED_STAIRS_ITEM);
            event.accept(ModBlocks.CURSED_FENCE_ITEM);
            event.accept(ModBlocks.CURSED_BRICKS_ITEM);
            event.accept(ModBlocks.CURSED_RUNE_STONE_ITEM);
            event.accept(ModBlocks.CURSED_NETHER_BRICKS_ITEM);
            event.accept(ModBlocks.CURSED_NETHER_BRICK_STAIRS_ITEM);
            event.accept(ModBlocks.CURSED_NETHER_BRICK_SLAB_ITEM);
            event.accept(ModBlocks.CURSED_GLASS_ITEM);
            event.accept(ModBlocks.CURSED_GLOWSTONE_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.CELESTIA_SWORD);
            event.accept(ModItems.CELESTIA_AXE);
            event.accept(ModItems.CELESTIA_SHIELD);
            event.accept(ModItems.CURSED_SWORD);
            event.accept(ModItems.CURSED_AXE);
            event.accept(ModItems.CURSED_SHIELD);
            event.accept(ModItems.PAXEL);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.CELESTIA_PICKAXE);
            event.accept(ModItems.CELESTIA_SHEARS);
            event.accept(ModItems.CURSED_PICKAXE);
            event.accept(ModItems.PAXEL);
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.GUARDIAN_EGG);
            event.accept(ModItems.CURSED_BOSS_EGG);
        }
    }
}
