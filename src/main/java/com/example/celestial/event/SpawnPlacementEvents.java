package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.registry.ModEntities;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * Правила появления проклятых мобов. Сами мобы прописаны в биоме celestial:cursed_niflheim (Незер):
 * монстры (зомби, скелет) появляются при любом освещении, животные - на любом прочном блоке
 * (в аду нет травы, поэтому ванильное правило "только трава" не подходит).
 */
@EventBusSubscriber(modid = CelestialMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class SpawnPlacementEvents {
    private SpawnPlacementEvents() {}

    @SubscribeEvent
    public static void register(RegisterSpawnPlacementsEvent event) {
        var op = RegisterSpawnPlacementsEvent.Operation.REPLACE;
        var heightmap = Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;

        event.register(ModEntities.CURSED_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, heightmap,
                Monster::checkAnyLightMonsterSpawnRules, op);
        event.register(ModEntities.CURSED_SKELETON.get(), SpawnPlacementTypes.ON_GROUND, heightmap,
                Monster::checkAnyLightMonsterSpawnRules, op);

        event.register(ModEntities.CURSED_PIG.get(), SpawnPlacementTypes.ON_GROUND, heightmap,
                Mob::checkMobSpawnRules, op);
        event.register(ModEntities.CURSED_COW.get(), SpawnPlacementTypes.ON_GROUND, heightmap,
                Mob::checkMobSpawnRules, op);
        event.register(ModEntities.CURSED_SHEEP.get(), SpawnPlacementTypes.ON_GROUND, heightmap,
                Mob::checkMobSpawnRules, op);
        event.register(ModEntities.CURSED_CHICKEN.get(), SpawnPlacementTypes.ON_GROUND, heightmap,
                Mob::checkMobSpawnRules, op);
    }
}
