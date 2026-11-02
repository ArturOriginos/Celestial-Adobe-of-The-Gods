package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.CursedBoss;
import com.example.celestial.entity.CursedSpike;
import com.example.celestial.entity.GodSpike;
import com.example.celestial.entity.GuardianOfGods;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GuardianOfGods>> GUARDIAN =
            ENTITIES.register("guardian_of_gods", () -> EntityType.Builder
                    .of(GuardianOfGods::new, MobCategory.MONSTER)
                    .sized(1.6F, 3.9F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("guardian_of_gods"));

    public static final DeferredHolder<EntityType<?>, EntityType<GodSpike>> GOD_SPIKE =
            ENTITIES.register("god_spike", () -> EntityType.Builder
                    .of(GodSpike::new, MobCategory.MISC)
                    .sized(0.5F, 0.8F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("god_spike"));

    public static final DeferredHolder<EntityType<?>, EntityType<CursedBoss>> CURSED_BOSS =
            ENTITIES.register("cursed_boss", () -> EntityType.Builder
                    .of(CursedBoss::new, MobCategory.MONSTER)
                    .sized(1.2F, 3.7F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("cursed_boss"));

    public static final DeferredHolder<EntityType<?>, EntityType<CursedSpike>> CURSED_SPIKE =
            ENTITIES.register("cursed_spike", () -> EntityType.Builder
                    .of(CursedSpike::new, MobCategory.MISC)
                    .sized(0.5F, 0.8F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("cursed_spike"));

    private ModEntities() {}
}
