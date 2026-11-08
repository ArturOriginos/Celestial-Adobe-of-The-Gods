package com.example.celestial.entity;

import com.example.celestial.registry.ModEntities;
import com.example.celestial.registry.ModItems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Бог Селестии: финальный босс. 1000 здоровья, две стадии.
 * <p>
 * Стадия 1 - обычное поведение: бой вплотную Паксилем Богов и Проклятых и набор способностей.
 * На 500 здоровья босс замирает и на 30 секунд становится бессмертным: не двигается и по сценарию
 * применяет все свои способности. После этого начинается стадия 2 - те же способности, но быстрее и сильнее.
 * Паксель в руке чисто декоративный и никогда не выпадает.
 */
public class CelestiaGod extends Monster {
    public static final double MAX_HEALTH = 1000.0D;
    public static final double PHASE2_HEALTH = 500.0D;
    public static final int IMMORTAL_TICKS = 30 * 20;
    private static final int RISE_TICKS = 70;

    // ---------- урон способностей (до множителя стадии 2) ----------
    /** Ни один удар босса (и ближний, и способности) не бьёт сильнее этого значения: 20 здоровья = 10 сердец. */
    public static final float MAX_HIT_DAMAGE = 20.0F;
    public static final float DMG_STRIKE = 20.0F;  // удар столбом света (Власть Селестии)
    public static final float DMG_PILLAR = 20.0F;  // столп кары
    public static final float DMG_STAR = 12.0F;    // падающая звезда
    public static final float DMG_HALO = 14.0F;    // расходящееся кольцо
    public static final float DMG_BEAM = 20.0F;    // луч
    public static final float DMG_STEP = 16.0F;    // всплеск после телепорта
    public static final float PHASE2_MULT = 1.3F;

    // ---------- способности ----------
    private static final int AB_NONE = 0, AB_DOMINION = 1, AB_STAR_RAIN = 2, AB_PILLARS = 3, AB_HALO = 4, AB_BEAM = 5, AB_STEP = 6;
    private static final int[] DURATION = {0, 130, 100, 100, 75, 75, 30};
    private static final int[] TRANSITION_SEQUENCE = {AB_DOMINION, AB_STAR_RAIN, AB_PILLARS, AB_HALO, AB_BEAM, AB_DOMINION};

    // ---------- стадии анимации (синхронизируются на клиент) ----------
    public static final int ST_IDLE = 0, ST_CHARGE = 1, ST_SUMMON = 2, ST_ERUPT = 3, ST_STRIKE = 4, ST_FINISH = 5,
            ST_STAR = 10, ST_PILLAR = 11, ST_HALO = 12, ST_BEAM = 13, ST_STEP = 14, ST_IMMORTAL = 15;

    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(CelestiaGod.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(CelestiaGod.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IMMORTAL =
            SynchedEntityData.defineId(CelestiaGod.class, EntityDataSerializers.BOOLEAN);

    private static final class Strike {
        final double x, y, z;
        int delay;
        final float radius, damage;
        final int kind; // 0 - столп, 1 - звезда
        Strike(double x, double y, double z, int delay, float radius, float damage, int kind) {
            this.x = x; this.y = y; this.z = z; this.delay = delay; this.radius = radius; this.damage = damage; this.kind = kind;
        }
    }

    // ---------- серверное состояние ----------
    private int riseTicks = 0;
    private double riseBaseY;
    private int transitionTicks = 0;
    private int ability = AB_NONE;
    private int abilityTick = 0;
    private int cooldown = 80;
    private int lastAbility = AB_NONE;
    private int seqIndex = 0;
    private final List<Strike> strikes = new ArrayList<>();
    private final Set<UUID> haloHit1 = new HashSet<>();
    private final Set<UUID> haloHit2 = new HashSet<>();
    private Vec3 beamDir = Vec3.ZERO;

    // ---------- клиентская анимация ----------
    private int stageTicks;
    private float lift, lift0, armX, armX0, armZ, armZ0, ringScale = 1.0F, ringScale0 = 1.0F;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.celestial.celestia_god"),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

    public CelestiaGod(EntityType<? extends CelestiaGod> type, Level level) {
        super(type, level);
        this.xpReward = 2000;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 56.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAGE, ST_IDLE);
        builder.define(DATA_PHASE, 1);
        builder.define(DATA_IMMORTAL, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false) {
            @Override
            public boolean canUse() {
                return !isBusy() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !isBusy() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // =================================================================== состояние

    public int getPhase() { return this.entityData.get(DATA_PHASE); }
    public int getStage() { return this.entityData.get(DATA_STAGE); }
    public boolean isImmortalPhase() { return this.entityData.get(DATA_IMMORTAL); }
    public float damageMultiplier() { return getPhase() >= 2 ? PHASE2_MULT : 1.0F; }

    private void setStage(int stage) { this.entityData.set(DATA_STAGE, stage); }

    /** Идёт превращение, каст или появление: перемещение запрещено. */
    private boolean isBusy() {
        return riseTicks > 0 || transitionTicks > 0 || (ability != AB_NONE && ability != AB_STEP);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (!this.level().isClientSide && isBusy()) {
            super.travel(Vec3.ZERO);
        } else {
            super.travel(travelVector);
        }
    }

    // =================================================================== появление и паксель

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        equipPaxel();
        if (spawnType == MobSpawnType.STRUCTURE) return data;
        this.riseBaseY = this.getY();
        this.riseTicks = RISE_TICKS;
        this.setPos(this.getX(), this.riseBaseY - 5.0D, this.getZ());
        return data;
    }

    /** Паксель Богов и Проклятых в руке: только для вида, шанс выпадения 0. */
    private void equipPaxel() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.PAXEL.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        // экипировка (паксель) не выпадает никогда, зато Кольцо полёта богов выпадает всегда (100%)
        this.spawnAtLocation(new ItemStack(ModItems.FLIGHT_RING.get()));
    }

    // =================================================================== тик

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            clientTick();
        } else {
            if (this.getMainHandItem().isEmpty()) equipPaxel();
            if (isBusy()) {
                Vec3 d = this.getDeltaMovement();
                this.setDeltaMovement(0.0D, d.y, 0.0D);
            }
        }
    }

    @Override
    public void aiStep() {
        if (riseTicks > 0) {
            rise();
            return;
        }
        super.aiStep();
    }

    private void rise() {
        riseTicks--;
        double progress = 1.0D - (double) riseTicks / RISE_TICKS;
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(this.getX(), riseBaseY - 5.0D + 5.0D * progress, this.getZ());
        if (this.level() instanceof ServerLevel server) {
            BlockState ground = server.getBlockState(BlockPos.containing(this.getX(), riseBaseY - 1.0D, this.getZ()));
            if (ground.isAir()) ground = Blocks.STONE.defaultBlockState();
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    this.getX(), riseBaseY + 0.1D, this.getZ(), 14, 1.3D, 0.1D, 1.3D, 0.15D);
            server.sendParticles(ParticleTypes.END_ROD, this.getX(), riseBaseY + progress * 4.0D, this.getZ(),
                    4, 1.0D, 0.6D, 1.0D, 0.03D);
            if (riseTicks % 8 == 0) this.playSound(SoundEvents.GRAVEL_BREAK, 2.5F, 0.5F);
            if (riseTicks == 0) {
                this.noPhysics = false;
                this.playSound(SoundEvents.BEACON_ACTIVATE, 3.0F, 0.6F);
                this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 3.0F, 1.2F);
                server.sendParticles(ParticleTypes.EXPLOSION, this.getX(), riseBaseY + 0.5D, this.getZ(), 8, 2.0D, 0.2D, 2.0D, 0.0D);
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (riseTicks > 0) return true;
        if (transitionTicks > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        return super.isInvulnerableTo(source);
    }

    /** Первую стадию нельзя пройти «насквозь»: смерть превращается в переход на вторую стадию. */
    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && getPhase() == 1 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.setHealth((float) PHASE2_HEALTH);
            startTransition();
            return;
        }
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player killer) {
            this.killerId = killer.getUUID();
        } else if (this.getKillCredit() instanceof net.minecraft.world.entity.player.Player credit) {
            this.killerId = credit.getUUID();
        }
        super.die(source);
    }

    // =================================================================== полоска здоровья

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // =================================================================== ИИ

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (riseTicks > 0) return;

        if (getPhase() == 1 && this.getHealth() <= PHASE2_HEALTH) {
            this.setHealth((float) PHASE2_HEALTH);
            startTransition();
        }

        tickStrikes();

        if (transitionTicks > 0) tickTransition();
        if (ability != AB_NONE) {
            tickAbility();
        } else if (transitionTicks == 0) {
            if (cooldown > 0) cooldown--;
            else chooseAbility();
        }
    }

    // ---------- переход на 2 стадию ----------
    private void startTransition() {
        this.entityData.set(DATA_PHASE, 2);
        this.entityData.set(DATA_IMMORTAL, true);
        this.transitionTicks = IMMORTAL_TICKS;
        this.seqIndex = 0;
        this.cooldown = 30;
        this.ability = AB_NONE;
        this.strikes.clear();
        this.getNavigation().stop();
        setStage(ST_IMMORTAL);
        this.bossEvent.setName(Component.translatable("entity.celestial.celestia_god.immortal"));
        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 4.0F, 0.7F);
        this.playSound(SoundEvents.BEACON_POWER_SELECT, 4.0F, 0.5F);
        if (this.level() instanceof ServerLevel level) {
            hurtArea(getX(), getY(), getZ(), 12.0F, 16.0F, true);
            level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2.5D, getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.5D, getZ(), 120, 0.5D, 0.5D, 0.5D, 0.9D);
        }
        announce(Component.translatable("celestial.god.phase2"));
    }

    private void tickTransition() {
        transitionTicks--;
        if (transitionTicks <= 0) {
            endTransition();
            return;
        }
        // защитное сияние вокруг бессмертного босса
        if (this.level() instanceof ServerLevel level && this.tickCount % 3 == 0) {
            double a = this.tickCount * 0.25D;
            for (int i = 0; i < 3; i++) {
                double ang = a + i * (Math.PI * 2.0 / 3.0);
                level.sendParticles(ParticleTypes.END_ROD, getX() + Math.cos(ang) * 2.2D,
                        getY() + 0.5D + (this.tickCount % 60) / 12.0D, getZ() + Math.sin(ang) * 2.2D, 1, 0, 0, 0, 0.0D);
            }
        }
        // сценарий способностей
        if (ability == AB_NONE) {
            if (cooldown > 0) {
                cooldown--;
            } else if (transitionTicks < IMMORTAL_TICKS - 30) {
                for (int tries = 0; tries < TRANSITION_SEQUENCE.length; tries++) {
                    int next = TRANSITION_SEQUENCE[seqIndex % TRANSITION_SEQUENCE.length];
                    seqIndex++;
                    if (DURATION[next] + 5 < transitionTicks) {
                        startAbility(next);
                        break;
                    }
                }
            }
        }
    }

    private void endTransition() {
        this.transitionTicks = 0;
        this.entityData.set(DATA_IMMORTAL, false);
        this.bossEvent.setName(Component.translatable("entity.celestial.celestia_god"));
        if (ability != AB_NONE) endAbility();
        this.cooldown = 40;
        setStage(ST_IDLE);
        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 4.0F, 1.1F);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.5D, getZ(), 80, 1.0D, 1.5D, 1.0D, 0.4D);
        }
        announce(Component.translatable("celestial.god.vulnerable"));
    }

    // ---------- выбор способности ----------
    private void chooseAbility() {
        LivingEntity t = this.getTarget();
        if (t == null || !t.isAlive()) return;
        double dist = Math.sqrt(this.distanceToSqr(t));
        int[] w = new int[7];
        w[AB_DOMINION] = dist < 32 ? 3 : 0;
        w[AB_STAR_RAIN] = dist < 32 ? 2 : 0;
        w[AB_PILLARS] = dist < 40 ? 2 : 0;
        w[AB_HALO] = dist < 16 ? 3 : 0;
        w[AB_BEAM] = (dist > 4 && dist < 40 && this.hasLineOfSight(t)) ? 3 : 0;
        w[AB_STEP] = dist > 20 ? 9 : (dist > 12 ? (getPhase() == 2 ? 4 : 3) : 0);
        if (lastAbility > 0 && lastAbility < w.length) w[lastAbility] = 0;
        int total = 0;
        for (int x : w) total += x;
        if (total <= 0) return;
        int roll = this.random.nextInt(total);
        for (int i = 1; i < w.length; i++) {
            roll -= w[i];
            if (roll < 0) {
                startAbility(i);
                return;
            }
        }
    }

    private void startAbility(int id) {
        this.ability = id;
        this.abilityTick = 0;
        this.lastAbility = id;
        this.getNavigation().stop();
        this.haloHit1.clear();
        this.haloHit2.clear();
        switch (id) {
            case AB_DOMINION -> announce(Component.translatable("celestial.god.ability.dominion"));
            case AB_STAR_RAIN -> announce(Component.translatable("celestial.god.ability.star_rain"));
            case AB_PILLARS -> announce(Component.translatable("celestial.god.ability.pillars"));
            case AB_HALO -> announce(Component.translatable("celestial.god.ability.halo"));
            case AB_BEAM -> announce(Component.translatable("celestial.god.ability.beam"));
            default -> { }
        }
    }

    private void endAbility() {
        this.ability = AB_NONE;
        this.abilityTick = 0;
        if (transitionTicks > 0) {
            this.cooldown = 10;
            setStage(ST_IMMORTAL);
        } else {
            this.cooldown = getPhase() == 1 ? 60 + this.random.nextInt(40) : 30 + this.random.nextInt(30);
            setStage(ST_IDLE);
        }
    }

    private void tickAbility() {
        LivingEntity focus = focus();
        if (focus != null) this.getLookControl().setLookAt(focus, 40.0F, 40.0F);
        switch (ability) {
            case AB_DOMINION -> tickDominion();
            case AB_STAR_RAIN -> tickStarRain();
            case AB_PILLARS -> tickPillars();
            case AB_HALO -> tickHalo();
            case AB_BEAM -> tickBeam(focus);
            case AB_STEP -> tickStep(focus);
            default -> { }
        }
        abilityTick++;
        if (abilityTick >= DURATION[ability]) endAbility();
    }

    // =================================================================== способность 1: Власть Селестии
    private static final float DOMINION_RADIUS = 9.0F;

    private void tickDominion() {
        if (!(this.level() instanceof ServerLevel level)) return;
        int t = abilityTick;
        double x = getX(), y = getY(), z = getZ();
        boolean p2 = getPhase() >= 2;

        if (t == 0) {
            setStage(ST_CHARGE);
            this.playSound(SoundEvents.BEACON_ACTIVATE, 3.5F, 0.8F);
        }
        if (t < 40) { // 1. накопление света: частицы стягиваются к боссу, на земле растёт золотое кольцо
            for (int i = 0; i < 6; i++) {
                double ang = this.random.nextDouble() * Math.PI * 2;
                double r = 7.0D * (1.0D - t / 40.0D) + 2.0D;
                double h = this.random.nextDouble() * 4.0D;
                level.sendParticles(ParticleTypes.END_ROD, x + Math.cos(ang) * r, y + h, z + Math.sin(ang) * r,
                        0, -Math.cos(ang) * 0.5, 0.05, -Math.sin(ang) * 0.5, 0.35);
            }
            ring(level, ParticleTypes.END_ROD, x, y + 0.15D, z, DOMINION_RADIUS * t / 40.0D, 20);
        }
        if (t == 40) { // 2. призыв света
            setStage(ST_SUMMON);
            this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 3.5F, 0.6F);
        }
        if (t >= 40 && t < 60) { // предупреждение: зона удара и кольца будущих кристаллов
            if (t % 2 == 0) {
                ring(level, ParticleTypes.FLAME, x, y + 0.15D, z, DOMINION_RADIUS, 36);
                ring(level, ParticleTypes.END_ROD, x, y + 0.15D, z, 3.0D, 12);
                ring(level, ParticleTypes.END_ROD, x, y + 0.15D, z, 6.0D, 18);
            }
        }
        if (t == 60) { // 3. выброс света: кристаллы кольцами
            setStage(ST_ERUPT);
            // огромный столб света: предупреждение 30 тиков, затем удар
            spawnEffect(CelestiaEffect.KIND_PILLAR, x, y, z, DOMINION_RADIUS, 0.0F, 30, 20, 0.0F, 0.0F);
            this.playSound(SoundEvents.EVOKER_CAST_SPELL, 4.0F, 0.5F);
            int[] radii = p2 ? new int[]{3, 6, 9, 12, 15} : new int[]{3, 6, 9, 12};
            int[] counts = {6, 12, 18, 24, 30};
            for (int r = 0; r < radii.length; r++) {
                for (int i = 0; i < counts[r]; i++) {
                    double ang = (Math.PI * 2.0 / counts[r]) * i;
                    spawnSpike(x + Math.cos(ang) * radii[r], z + Math.sin(ang) * radii[r], r * 4);
                }
            }
        }
        if (p2 && t == 74) { // вторая волна по сдвинутым кольцам
            this.playSound(SoundEvents.EVOKER_CAST_SPELL, 4.0F, 0.7F);
            int[] radii = {4, 7, 10, 13};
            int[] counts = {8, 14, 20, 26};
            for (int r = 0; r < radii.length; r++) {
                for (int i = 0; i < counts[r]; i++) {
                    double ang = (Math.PI * 2.0 / counts[r]) * (i + 0.5);
                    spawnSpike(x + Math.cos(ang) * radii[r], z + Math.sin(ang) * radii[r], r * 3);
                }
            }
        }
        if (t >= 60 && t < 90 && t % 2 == 0) { // жар в центре перед ударом
            level.sendParticles(ParticleTypes.END_ROD, x, y + 0.2D, z, 8, 2.0D, 0.1D, 2.0D, 0.05D);
            ring(level, ParticleTypes.FLAME, x, y + 0.15D, z, DOMINION_RADIUS, 36);
        }
        if (t == 90) { // 4. удар: столб света
            setStage(ST_STRIKE);
            hurtArea(x, y, z, DOMINION_RADIUS, DMG_STRIKE, true);
            level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 6.0F, 0.7F);
            level.playSound(null, x, y, z, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 5.0F, 0.6F);
            level.sendParticles(ParticleTypes.FLASH, x, y + 1.5D, z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 0.5D, z, 1, 0, 0, 0, 0);
        }
        if (t >= 90 && t < 100) { // столб света держится
            for (int h = 0; h < 32; h += 2) {
                level.sendParticles(ParticleTypes.END_ROD, x, y + h, z, 4, 1.6D, 0.2D, 1.6D, 0.02D);
            }
            ring(level, ParticleTypes.END_ROD, x, y + 0.2D, z, DOMINION_RADIUS * (t - 89) / 10.0D, 30);
        }
        if (t == 100) setStage(ST_FINISH); // 5. завершение
    }

    // =================================================================== способность 2: Звёздопад
    private void tickStarRain() {
        int t = abilityTick;
        if (t == 0) {
            setStage(ST_STAR);
            this.playSound(SoundEvents.BEACON_POWER_SELECT, 3.5F, 1.2F);
        }
        if (this.level() instanceof ServerLevel level && t < 20) {
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 6.0D, getZ(), 5, 1.5D, 1.5D, 1.5D, 0.1D);
        }
        int interval = getPhase() >= 2 ? 2 : 3;
        if (t >= 20 && t < 80 && t % interval == 0) {
            List<Player> players = nearbyPlayers();
            if (players.isEmpty()) return;
            Player p = players.get(this.random.nextInt(players.size()));
            double ang = this.random.nextDouble() * Math.PI * 2;
            double r = this.random.nextDouble() * 8.0D;
            boolean direct = getPhase() >= 2 && this.random.nextInt(5) < 2;
            double sx = direct ? p.getX() : p.getX() + Math.cos(ang) * r;
            double sz = direct ? p.getZ() : p.getZ() + Math.sin(ang) * r;
            addStrike(sx, sz, p.getY(), 22, 2.6F, DMG_STAR, 1);
        }
    }

    // =================================================================== способность 3: Столпы кары
    private void tickPillars() {
        int t = abilityTick;
        if (t == 0) {
            setStage(ST_PILLAR);
            this.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 3.5F, 0.6F);
        }
        List<Player> players = nearbyPlayers();
        if (t == 10) {
            for (Player p : players) addStrike(p.getX(), p.getZ(), p.getY(), 35, 3.0F, DMG_PILLAR, 0);
        }
        if (t == 40) { // упреждающая волна: туда, куда игрок бежит
            for (Player p : players) {
                Vec3 m = p.getDeltaMovement();
                double mx = Mth.clamp(m.x * 25.0D, -7.0D, 7.0D), mz = Mth.clamp(m.z * 25.0D, -7.0D, 7.0D);
                addStrike(p.getX() + mx, p.getZ() + mz, p.getY(), 28, 3.0F, DMG_PILLAR, 0);
            }
        }
        if (t == 65 && getPhase() >= 2) { // стадия 2: крест из столпов вокруг игрока
            for (Player p : players) {
                addStrike(p.getX(), p.getZ(), p.getY(), 28, 3.0F, DMG_PILLAR, 0);
                for (int i = 0; i < 4; i++) {
                    double ang = i * Math.PI / 2.0;
                    addStrike(p.getX() + Math.cos(ang) * 4.5D, p.getZ() + Math.sin(ang) * 4.5D, p.getY(), 28, 3.0F, DMG_PILLAR, 0);
                }
            }
        }
    }

    // =================================================================== способность 4: Кольца Селестии
    private void tickHalo() {
        int t = abilityTick;
        if (t == 0) {
            setStage(ST_HALO);
            this.playSound(SoundEvents.BEACON_ACTIVATE, 3.5F, 1.4F);
        }
        if (t >= 10) haloRing(t - 10, haloHit1);
        if (t >= 25 && getPhase() >= 2) haloRing(t - 25, haloHit2);
    }

    private void haloRing(int age, Set<UUID> hit) {
        double r = 2.0D + age * 0.65D;
        if (r > 28.0D || !(this.level() instanceof ServerLevel level)) return;
        ring(level, ParticleTypes.END_ROD, getX(), getY() + 0.2D, getZ(), r, (int) Math.max(24, r * 5));
        double reach = r + 1.5D;
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(getX() - reach, getY() - 1.0D, getZ() - reach, getX() + reach, getY() + 3.0D, getZ() + reach),
                this::isValidVictim);
        for (LivingEntity e : list) {
            if (hit.contains(e.getUUID())) continue;
            double d = Math.hypot(e.getX() - getX(), e.getZ() - getZ());
            if (Math.abs(d - r) > 1.4D) continue;
            if (e.getY() - getY() > 1.0D) continue; // можно перепрыгнуть
            hit.add(e.getUUID());
            hit(e, DMG_HALO * damageMultiplier(), true);
        }
    }

    // =================================================================== способность 5: Луч
    @Nullable
    private CelestiaEffect beamFx;

    private static float yawOf(Vec3 d) { return (float) (Mth.atan2(d.x, d.z) * 57.29577951D); }
    private static float pitchOf(Vec3 d) { return (float) (Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 57.29577951D); }

    private void tickBeam(@Nullable LivingEntity focus) {
        int t = abilityTick;
        if (t == 0) {
            setStage(ST_BEAM);
            beamFx = null;
            this.playSound(SoundEvents.BEACON_POWER_SELECT, 3.5F, 0.6F);
        }
        boolean aiming = t < 20 || (t >= 40 && t < 48);
        if (focus != null && aiming) {
            Vec3 to = focus.getBoundingBox().getCenter().subtract(this.getEyePosition());
            if (to.lengthSqr() > 1.0E-4D) beamDir = to.normalize();
        }
        if (!(this.level() instanceof ServerLevel level) || beamDir.equals(Vec3.ZERO)) return;
        Vec3 origin = this.getEyePosition();
        // луч-предупреждение следует за целью, пока босс целится
        if (t == 0 || t == 40) beamFx = spawnEffect(CelestiaEffect.KIND_BEAM, origin.x, origin.y, origin.z, 0.5F, 40.0F,
                t == 0 ? 25 : 10, 10, yawOf(beamDir), pitchOf(beamDir));
        if (aiming && beamFx != null && beamFx.isAlive()) {
            beamFx.moveTo(origin.x, origin.y, origin.z, yawOf(beamDir), pitchOf(beamDir));
        }
        if (aiming && t % 2 == 0) { // дополнительно - линия из частиц
            for (int i = 2; i < 40; i += 2) {
                Vec3 p = origin.add(beamDir.scale(i));
                level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
            }
        }
        if (t == 25 || t == 50) {
            float len = fireBeam(level, beamDir);
            if (beamFx != null && beamFx.isAlive()) beamFx.setLength(len);
            if (getPhase() >= 2) {
                for (double ang : new double[]{0.22D, -0.22D}) {
                    Vec3 d = beamDir.yRot((float) ang);
                    float l2 = fireBeam(level, d);
                    spawnEffect(CelestiaEffect.KIND_BEAM, origin.x, origin.y, origin.z, 0.5F, l2, 0, 10, yawOf(d), pitchOf(d));
                }
            }
        }
    }

    /** Наносит урон по лучу и возвращает его длину (до первого блока). */
    private float fireBeam(ServerLevel level, Vec3 dir) {
        Vec3 origin = this.getEyePosition();
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 4.0F, 1.4F);
        Set<LivingEntity> victims = new HashSet<>();
        float length = 40.0F;
        for (int i = 1; i <= 80; i++) {
            Vec3 p = origin.add(dir.scale(i * 0.5D));
            BlockPos bp = BlockPos.containing(p);
            if (!level.getBlockState(bp).getCollisionShape(level, bp).isEmpty()) {
                level.sendParticles(ParticleTypes.FLASH, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                length = i * 0.5F;
                break;
            }
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 2, 0.12D, 0.12D, 0.12D, 0.01D);
            if (i % 3 == 0) {
                victims.addAll(level.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(1.0D), this::isValidVictim));
            }
        }
        for (LivingEntity e : victims) hit(e, DMG_BEAM * damageMultiplier(), false);
        return length;
    }

    // =================================================================== способность 6: Шаг бога
    private void tickStep(@Nullable LivingEntity focus) {
        if (!(this.level() instanceof ServerLevel level)) return;
        int t = abilityTick;
        if (t == 0) {
            setStage(ST_STEP);
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 3.0F, 0.6F);
            level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.5D, getZ(), 60, 0.8D, 1.8D, 0.8D, 0.2D);
        }
        if (t == 8 && focus != null) {
            for (int i = 0; i < 10; i++) {
                double ang = this.random.nextDouble() * Math.PI * 2;
                double tx = focus.getX() + Math.cos(ang) * 4.5D, tz = focus.getZ() + Math.sin(ang) * 4.5D;
                double ty = focus.getY();
                AABB moved = this.getBoundingBox().move(tx - getX(), ty - getY(), tz - getZ());
                if (level.noCollision(this, moved)) {
                    this.moveTo(tx, ty, tz, this.getYRot(), this.getXRot());
                    this.getNavigation().stop();
                    level.playSound(null, tx, ty, tz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 3.0F, 0.8F);
                    level.sendParticles(ParticleTypes.END_ROD, tx, ty + 2.5D, tz, 60, 0.8D, 1.8D, 0.8D, 0.2D);
                    break;
                }
            }
        }
        if (t == 18) {
            hurtArea(getX(), getY(), getZ(), 6.0F, DMG_STEP, true);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 4.0F, 0.8F);
            level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5D, getZ(), 6, 2.0D, 0.2D, 2.0D, 0.0D);
            for (int i = 0; i < 10; i++) {
                double ang = Math.PI * 2.0 / 10 * i;
                spawnSpike(getX() + Math.cos(ang) * 4.0D, getZ() + Math.sin(ang) * 4.0D, 0);
            }
            for (int i = 0; i < 16; i++) {
                double ang = Math.PI * 2.0 / 16 * (i + 0.5);
                spawnSpike(getX() + Math.cos(ang) * 7.0D, getZ() + Math.sin(ang) * 7.0D, 4);
            }
        }
    }

    // =================================================================== отложенные удары
    private void addStrike(double x, double z, double nearY, int delay, float radius, float damage, int kind) {
        double gy = groundY(x, nearY, z);
        strikes.add(new Strike(x, gy, z, delay, radius, damage, kind));
        // визуал: предупреждающее кольцо + столп света / падающая звезда
        spawnEffect(kind == 0 ? CelestiaEffect.KIND_PILLAR : CelestiaEffect.KIND_STAR, x, gy, z, radius, 0.0F, delay,
                kind == 0 ? 16 : 10, 0.0F, 0.0F);
    }

    @Nullable
    private CelestiaEffect spawnEffect(int kind, double x, double y, double z, float radius, float length,
                                       int warmup, int life, float yaw, float pitch) {
        if (!(this.level() instanceof ServerLevel level)) return null;
        CelestiaEffect fx = ModEntities.CELESTIA_EFFECT.get().create(level);
        if (fx == null) return null;
        fx.setup(kind, radius, length, warmup, life);
        fx.moveTo(x, y, z, yaw, pitch);
        level.addFreshEntity(fx);
        return fx;
    }

    private void tickStrikes() {
        if (strikes.isEmpty() || !(this.level() instanceof ServerLevel level)) return;
        Iterator<Strike> it = strikes.iterator();
        while (it.hasNext()) {
            Strike s = it.next();
            s.delay--;
            if (s.delay > 0) {
                if (s.delay % 2 == 0) ring(level, ParticleTypes.FLAME, s.x, s.y + 0.15D, s.z, s.radius, 14);
                if (s.kind == 1) { // звезда падает сверху
                    level.sendParticles(ParticleTypes.END_ROD, s.x, s.y + s.delay * 1.4D, s.z, 0, 0, -1.0, 0, 0.6);
                    level.sendParticles(ParticleTypes.END_ROD, s.x, s.y + s.delay * 1.4D + 0.7D, s.z, 0, 0, -1.0, 0, 0.6);
                }
                continue;
            }
            it.remove();
            hurtArea(s.x, s.y, s.z, s.radius, s.damage, true);
            level.playSound(null, s.x, s.y, s.z, s.kind == 0 ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.HOSTILE, s.kind == 0 ? 2.5F : 3.0F, s.kind == 0 ? 1.2F : 0.6F);
            if (s.kind == 0) {
                for (int h = 0; h < 30; h += 2) {
                    level.sendParticles(ParticleTypes.END_ROD, s.x, s.y + h, s.z, 3, s.radius * 0.25D, 0.1D, s.radius * 0.25D, 0.03D);
                }
                level.sendParticles(ParticleTypes.FLASH, s.x, s.y + 1.0D, s.z, 1, 0, 0, 0, 0);
            } else {
                level.sendParticles(ParticleTypes.END_ROD, s.x, s.y + 0.5D, s.z, 24, s.radius * 0.4D, 0.3D, s.radius * 0.4D, 0.25D);
                level.sendParticles(ParticleTypes.EXPLOSION, s.x, s.y + 0.3D, s.z, 1, 0, 0, 0, 0);
            }
        }
    }

    // =================================================================== утилиты
    private void spawnSpike(double x, double z, int delay) {
        if (!(this.level() instanceof ServerLevel level)) return;
        CelestiaSpike spike = ModEntities.CELESTIA_SPIKE.get().create(level);
        if (spike == null) return;
        spike.setup(x, groundY(x, this.getY(), z), z, this, delay);
        level.addFreshEntity(spike);
    }

    private double groundY(double x, double startY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(startY) + 2, Mth.floor(z));
        for (int i = 0; i < 8; i++) {
            BlockPos below = pos.below();
            boolean solidBelow = !this.level().getBlockState(below).getCollisionShape(this.level(), below).isEmpty();
            boolean freeHere = this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty();
            if (solidBelow && freeHere) return pos.getY();
            pos.move(Direction.DOWN);
        }
        return startY;
    }

    private static void ring(ServerLevel level, net.minecraft.core.particles.ParticleOptions type,
                             double x, double y, double z, double radius, int points) {
        if (radius < 0.3D) return;
        for (int i = 0; i < points; i++) {
            double ang = Math.PI * 2.0 / points * i;
            level.sendParticles(type, x + Math.cos(ang) * radius, y, z + Math.sin(ang) * radius, 1, 0, 0, 0, 0.0D);
        }
    }

    private boolean isValidVictim(LivingEntity e) {
        if (e == this || !e.isAlive()) return false;
        if (e instanceof CelestiaGod || e instanceof GuardianOfGods || e instanceof CursedBoss) return false;
        return !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    private void hit(LivingEntity e, float damage, boolean knockUp) {
        if (e.hurt(this.damageSources().indirectMagic(this, this), Math.min(damage, MAX_HIT_DAMAGE)) && knockUp) {
            e.setDeltaMovement(e.getDeltaMovement().add(0.0D, 0.9D, 0.0D));
            e.hurtMarked = true;
        }
    }

    private void hurtArea(double x, double y, double z, float radius, float baseDamage, boolean knockUp) {
        float damage = baseDamage * damageMultiplier();
        List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(x - radius, y - 1.0D, z - radius, x + radius, y + 6.0D, z + radius), this::isValidVictim);
        for (LivingEntity e : list) {
            double dx = e.getX() - x, dz = e.getZ() - z;
            if (dx * dx + dz * dz > (double) radius * radius) continue;
            hit(e, damage, knockUp);
        }
    }

    private List<Player> nearbyPlayers() {
        return this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(48.0D),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator());
    }

    @Nullable
    private LivingEntity focus() {
        LivingEntity t = this.getTarget();
        if (t != null && t.isAlive()) return t;
        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Player p : nearbyPlayers()) {
            double d = this.distanceToSqr(p);
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }

    private void announce(Component message) {
        if (!(this.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (this.distanceToSqr(p) < 64.0D * 64.0D) p.displayClientMessage(message, true);
        }
    }

    // =================================================================== NBT
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GodPhase", getPhase());
        tag.putInt("GodImmortal", transitionTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("GodPhase")) this.entityData.set(DATA_PHASE, Math.max(1, tag.getInt("GodPhase")));
        this.transitionTicks = tag.getInt("GodImmortal");
        if (transitionTicks > 0) {
            this.entityData.set(DATA_IMMORTAL, true);
            this.bossEvent.setName(Component.translatable("entity.celestial.celestia_god.immortal"));
        }
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }

    // =================================================================== клиентская анимация
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_STAGE.equals(key)) stageTicks = 0;
    }

    private void clientTick() {
        stageTicks++;
        int st = getStage();
        float tx, tz, tr = 1.0F;
        switch (st) {
            case ST_CHARGE -> { tx = -0.9F; tz = 0.35F; tr = 0.8F; }
            case ST_SUMMON -> { tx = -0.15F; tz = 1.25F; tr = 1.6F; }
            case ST_ERUPT -> { tx = -0.5F; tz = 1.0F; tr = 1.8F; }
            case ST_STRIKE -> { tx = -0.3F; tz = 0.25F; tr = 2.2F; }
            case ST_FINISH -> { tx = -0.05F; tz = 0.4F; tr = 1.0F; }
            case ST_STAR -> { tx = -2.9F; tz = 0.3F; tr = 1.4F; }
            case ST_PILLAR -> { tx = -1.6F; tz = 0.2F; tr = 1.3F; }
            case ST_HALO -> { tx = -0.1F; tz = 1.5F; tr = 2.0F; }
            case ST_BEAM -> { tx = -1.55F; tz = 0.05F; tr = 1.2F; }
            case ST_STEP -> { tx = -0.5F; tz = 0.5F; tr = 1.3F; }
            case ST_IMMORTAL -> { tx = -0.2F; tz = 1.1F; tr = 1.7F; }
            default -> { tx = -0.05F; tz = 0.18F; }
        }
        armX0 = armX; armZ0 = armZ; ringScale0 = ringScale; lift0 = lift;
        armX += (tx - armX) * 0.2F;
        armZ += (tz - armZ) * 0.2F;
        ringScale += (tr - ringScale) * 0.1F;
        lift += ((isImmortalPhase() ? 1.6F : 0.0F) - lift) * 0.06F;

        if (this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.END_ROD, this.getRandomX(0.9D), this.getRandomY(), this.getRandomZ(0.9D),
                    0.0D, 0.03D, 0.0D);
        }
    }

    public float getArmX(float pt) { return Mth.lerp(pt, armX0, armX); }
    public float getArmZ(float pt) { return Mth.lerp(pt, armZ0, armZ); }
    public float getRingScale(float pt) { return Mth.lerp(pt, ringScale0, ringScale); }
    public float getLift(float pt) { return Mth.lerp(pt, lift0, lift); }

    // =================================================================== смерть

    /** Длительность анимации смерти: 3 секунды. */
    public static final int DEATH_TICKS = 60;

    /** Кто нанёс смертельный удар: ему достанется мини-душа. */
    private java.util.UUID killerId;

    /**
     * Анимация смерти (~3 с): бог не падает, а медленно поднимается, дрожит и светится,
     * вокруг расходятся кольца частиц, затем яркая вспышка и исчезновение.
     */
    @Override
    protected void tickDeath() {
        this.deathTime++;
        this.setNoGravity(true);
        this.setDeltaMovement(0.0D, 0.035D, 0.0D);
        if (!(this.level() instanceof ServerLevel server)) return;

        this.bossEvent.setProgress(0.0F);
        float t = (float) this.deathTime / DEATH_TICKS;
        double cx = this.getX(), cy = this.getY() + this.getBbHeight() * 0.55D, cz = this.getZ();

        if (this.deathTime == 1) {
            this.playSound(SoundEvents.BEACON_DEACTIVATE, 4.0F, 0.6F);
        }
        // лучи света, поднимающиеся от тела
        server.sendParticles(ParticleTypes.END_ROD, this.getRandomX(0.8D), this.getY() + this.random.nextDouble() * this.getBbHeight(),
                this.getRandomZ(0.8D), 2 + (int) (t * 6), 0.0D, 0.12D, 0.0D, 0.02D);
        // расширяющееся кольцо каждые 10 тиков
        if (this.deathTime % 10 == 0) {
            double radius = 0.8D + 3.2D * t;
            for (int i = 0; i < 28; i++) {
                double ang = Math.PI * 2.0D * i / 28.0D;
                server.sendParticles(ParticleTypes.END_ROD, cx + Math.cos(ang) * radius, cy, cz + Math.sin(ang) * radius,
                        1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 3.5F, 0.6F + 1.2F * t);
        }
        if (this.deathTime % 15 == 0) {
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 3.0F, 0.5F + 0.6F * t);
        }
        // финал: вспышка и исчезновение
        if (this.deathTime >= DEATH_TICKS && !this.isRemoved()) {
            server.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            server.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 160, 1.2D, 1.4D, 1.2D, 0.25D);
            server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, cx, cy, cz, 90, 1.0D, 1.2D, 1.0D, 0.4D);
            server.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 5.0F, 0.7F);
            server.playSound(null, cx, cy, cz, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 5.0F, 1.2F);
            // из погибшего бога вылетает маленькая душа и летит к убийце (или к ближайшему игроку)
            net.minecraft.server.level.ServerPlayer receiver = this.killerId == null ? null
                    : server.getServer().getPlayerList().getPlayer(this.killerId);
            if (receiver == null && server.getNearestPlayer(this, 64.0D) instanceof net.minecraft.server.level.ServerPlayer near) {
                receiver = near;
            }
            if (receiver != null) {
                com.example.celestial.entity.FlyingSoul soul = com.example.celestial.registry.ModEntities.FLYING_SOUL.get().create(server);
                if (soul != null) {
                    soul.setPos(cx, cy, cz);
                    soul.setTarget(receiver.getUUID());
                    server.addFreshEntity(soul);
                }
            }
            this.bossEvent.removeAllPlayers();
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    // =================================================================== звуки
    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.AMETHYST_BLOCK_RESONATE; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.AMETHYST_BLOCK_BREAK; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.ENDER_DRAGON_DEATH; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.AMETHYST_BLOCK_STEP, 1.2F, 0.6F);
    }
}
