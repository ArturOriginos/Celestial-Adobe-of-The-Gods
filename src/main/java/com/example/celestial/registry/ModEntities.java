package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.entity.AllKnowingAngel;
import com.example.celestial.entity.DarkOrb;
import com.example.celestial.entity.CelestiaEffect;
import com.example.celestial.entity.CelestiaGod;
import com.example.celestial.entity.CelestiaSpike;
import com.example.celestial.entity.CursedBoss;
import com.example.celestial.entity.Fenrir;
import com.example.celestial.entity.FenrirEffect;
import com.example.celestial.entity.FlyingSoul;
import com.example.celestial.entity.CursedChicken;
import com.example.celestial.entity.CursedCow;
import com.example.celestial.entity.CursedPig;
import com.example.celestial.entity.CursedSheep;
import com.example.celestial.entity.CursedSkeleton;
import com.example.celestial.entity.CursedZombie;
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

    public static final DeferredHolder<EntityType<?>, EntityType<CelestiaGod>> CELESTIA_GOD =
            ENTITIES.register("celestia_god", () -> EntityType.Builder
                    .of(CelestiaGod::new, MobCategory.MONSTER)
                    .sized(1.9F, 5.3F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .build("celestia_god"));

    public static final DeferredHolder<EntityType<?>, EntityType<CelestiaSpike>> CELESTIA_SPIKE =
            ENTITIES.register("celestia_spike", () -> EntityType.Builder
                    .of(CelestiaSpike::new, MobCategory.MISC)
                    .sized(0.5F, 0.8F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("celestia_spike"));

    /** Визуальные эффекты способностей Бога Селестии (столпы, звёзды, лучи). Не сохраняются в мире. */
    public static final DeferredHolder<EntityType<?>, EntityType<CelestiaEffect>> CELESTIA_EFFECT =
            ENTITIES.register("celestia_effect", () -> EntityType.Builder
                    .of(CelestiaEffect::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .noSave()
                    .noSummon()
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("celestia_effect"));

    public static final DeferredHolder<EntityType<?>, EntityType<FlyingSoul>> FLYING_SOUL =
            ENTITIES.register("flying_soul", () -> EntityType.Builder
                    .of(FlyingSoul::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("flying_soul"));

    // ----------------------------------------------------------- Фенрир
    public static final DeferredHolder<EntityType<?>, EntityType<Fenrir>> FENRIR =
            ENTITIES.register("fenrir", () -> EntityType.Builder
                    .of(Fenrir::new, MobCategory.MONSTER)
                    .sized(3.2F, 5.0F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .build("fenrir"));

    // ----------------------------------------------------------- Аид
    public static final DeferredHolder<EntityType<?>, EntityType<com.example.celestial.entity.Hades>> HADES =
            ENTITIES.register("hades", () -> EntityType.Builder
                    .of(com.example.celestial.entity.Hades::new, MobCategory.MONSTER)
                    .sized(1.9F, 5.2F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build("hades"));

    /** Атаки Аида (зоны, снаряды, ритуальный круг). Не сохраняются в мире. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.example.celestial.entity.HadesEffect>> HADES_EFFECT =
            ENTITIES.register("hades_effect", () -> EntityType.Builder
                    .of(com.example.celestial.entity.HadesEffect::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .noSave()
                    .noSummon()
                    .fireImmune()
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("hades_effect"));

    // ----------------------------------------------------------- Цербер
    public static final DeferredHolder<EntityType<?>, EntityType<com.example.celestial.entity.Cerberus>> CERBERUS =
            ENTITIES.register("cerberus", () -> EntityType.Builder
                    .of(com.example.celestial.entity.Cerberus::new, MobCategory.MONSTER)
                    .sized(3.4F, 5.0F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .build("cerberus"));

    /** Визуальные эффекты способностей Фенрира (вой, след прыжка, шипы). Не сохраняются в мире. */
    public static final DeferredHolder<EntityType<?>, EntityType<FenrirEffect>> FENRIR_EFFECT =
            ENTITIES.register("fenrir_effect", () -> EntityType.Builder
                    .of(FenrirEffect::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .noSave()
                    .noSummon()
                    .fireImmune()
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("fenrir_effect"));

    // ----------------------------------------------------------- Проклятые мобы (без естественного спавна)
    public static final DeferredHolder<EntityType<?>, EntityType<CursedPig>> CURSED_PIG =
            ENTITIES.register("cursed_pig", () -> EntityType.Builder
                    .of(CursedPig::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.9F)
                    .clientTrackingRange(8)
                    .build("cursed_pig"));

    public static final DeferredHolder<EntityType<?>, EntityType<CursedCow>> CURSED_COW =
            ENTITIES.register("cursed_cow", () -> EntityType.Builder
                    .of(CursedCow::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.4F)
                    .clientTrackingRange(8)
                    .build("cursed_cow"));

    public static final DeferredHolder<EntityType<?>, EntityType<CursedSheep>> CURSED_SHEEP =
            ENTITIES.register("cursed_sheep", () -> EntityType.Builder
                    .of(CursedSheep::new, MobCategory.CREATURE)
                    .sized(0.9F, 1.3F)
                    .clientTrackingRange(8)
                    .build("cursed_sheep"));

    public static final DeferredHolder<EntityType<?>, EntityType<CursedChicken>> CURSED_CHICKEN =
            ENTITIES.register("cursed_chicken", () -> EntityType.Builder
                    .of(CursedChicken::new, MobCategory.CREATURE)
                    .sized(0.4F, 0.7F)
                    .clientTrackingRange(8)
                    .build("cursed_chicken"));

    public static final DeferredHolder<EntityType<?>, EntityType<CursedZombie>> CURSED_ZOMBIE =
            ENTITIES.register("cursed_zombie", () -> EntityType.Builder
                    .of(CursedZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("cursed_zombie"));

    public static final DeferredHolder<EntityType<?>, EntityType<CursedSkeleton>> CURSED_SKELETON =
            ENTITIES.register("cursed_skeleton", () -> EntityType.Builder
                    .of(CursedSkeleton::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.99F)
                    .clientTrackingRange(8)
                    .build("cursed_skeleton"));

    // ----------------------------------------------------------- Всезнающий Ангел (без логики появления)
    public static final DeferredHolder<EntityType<?>, EntityType<AllKnowingAngel>> ALL_KNOWING_ANGEL =
            ENTITIES.register("all_knowing_angel", () -> EntityType.Builder
                    .of(AllKnowingAngel::new, MobCategory.MONSTER)
                    .sized(3.0F, 5.1F)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .build("all_knowing_angel"));

    public static final DeferredHolder<EntityType<?>, EntityType<DarkOrb>> DARK_ORB =
            ENTITIES.register("dark_orb", () -> EntityType.Builder
                    .of(DarkOrb::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("dark_orb"));

    private ModEntities() {}
}
