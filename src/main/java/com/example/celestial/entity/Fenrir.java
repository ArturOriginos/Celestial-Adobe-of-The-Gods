package com.example.celestial.entity;

import com.example.celestial.registry.ModEntities;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Фенрир: гигантский волк, слуга Аида. 8000 здоровья, но любой удар по нему снимает не больше 100.
 * Все его атаки (укус, прыжок, шипы, вой) бьют по 100 урона. Когда здоровье падает до 4000, он воет, на время
 * становится неуязвимым и один раз (только один!) восстанавливает 1000 здоровья, после чего бой продолжается
 * (вторая стадия: способности чаще, вой и шипы шире). Дроп: Часы Фенрира.
 *
 * <p>Потолок атрибута MAX_HEALTH (в ваниле 1024) поднят в {@code CelestialMod.raiseHealthLimit()} через access transformer,
 * поэтому у Фенрира настоящие 8000 здоровья. Если множитель {@link #HEALTH_SCALE} поставить больше 1, здоровье станет «виртуальным».
 */
public class Fenrir extends Monster {
    /** Здоровье с точки зрения игрока. */
    public static final double MAX_HEALTH = 8000.0D;
    public static final double PHASE2_HEALTH = 4000.0D;
    public static final float HEAL_AMOUNT = 1000.0F;
    /** Больше этого за один удар Фенрир не получает. */
    public static final float MAX_DAMAGE_TAKEN = 100.0F;
    /** Урон всех атак Фенрира. */
    public static final float ATTACK_DAMAGE = 100.0F / 1.5F; // урон срезан в 1,5 раза (было 100)
    /** 1 - здоровье настоящее. Во сколько раз «показываемое» здоровье больше реального (нужно только без снятия потолка). */
    public static final float HEALTH_SCALE = 1.0F;
    public static final double ATTRIBUTE_HEALTH = MAX_HEALTH / HEALTH_SCALE;

    private static final int RISE_TICKS = 80;
    private static final int TRANSITION_TICKS = 110;

    // ---------- способности ----------
    private static final int AB_NONE = 0, AB_LEAP = 1, AB_SPIKES = 2, AB_HOWL = 3;
    private static final int LEAP_WINDUP = 24, LEAP_DASH = 14, LEAP_RECOVER = 14;
    private static final int SPIKE_WINDUP = 32, SPIKE_TOTAL = 52;
    private static final int HOWL_WINDUP = 36, HOWL_TOTAL = 60;

    // ---------- стадии анимации (синхронизируются на клиент) ----------
    public static final int ST_IDLE = 0, ST_BITE = 1, ST_CROUCH = 2, ST_LEAP = 3, ST_SPIKES = 4, ST_HOWL = 5,
            ST_TRANSITION = 6;

    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(Fenrir.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(Fenrir.class, EntityDataSerializers.INT);

    private static final class Strike {
        final double x, y, z;
        int delay;
        final float radius;

        Strike(double x, double y, double z, int delay, float radius) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.delay = delay;
            this.radius = radius;
        }
    }

    private int riseTicks = 0;
    private double riseBaseY;
    private int transitionTicks = 0;
    private float healLeft = 0.0F;
    private int ability = AB_NONE;
    private int abilityTick = 0;
    private int cooldown = 80;
    private int lastAbility = AB_NONE;
    private int biteTimer = 0;
    // прыжок
    private int leapPhase = 0; // 0 - присел, 1 - рывок, 2 - восстановление
    private int dashTick = 0;
    private boolean dashing = false;
    private Vec3 leapDir = Vec3.ZERO;
    private double leapSpeed = 1.0D;
    private final Set<UUID> leapHit = new HashSet<>();
    // шипы
    private final List<Vec3> burstCenters = new ArrayList<>();
    private final List<Strike> strikes = new ArrayList<>();

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.celestial.fenrir"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    public Fenrir(EntityType<? extends Fenrir> type, Level level) {
        super(type, level);
        this.xpReward = 3000;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, ATTRIBUTE_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.STEP_HEIGHT, 1.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAGE, ST_IDLE);
        builder.define(DATA_PHASE, 1);
    }

    // =================================================================== состояние

    public int getPhase() { return this.entityData.get(DATA_PHASE); }
    public int getStage() { return this.entityData.get(DATA_STAGE); }

    private void setStage(int stage) {
        if (getStage() != stage) this.entityData.set(DATA_STAGE, stage);
    }

    /** Здоровье «как видит игрок» (из 8000). */
    public float getDisplayHealth() { return this.getHealth() * HEALTH_SCALE; }

    /** Идёт появление, превращение или каст: ходить нельзя (рывок прыжка - можно). */
    private boolean isBusy() {
        return riseTicks > 0 || transitionTicks > 0 || (ability != AB_NONE && !dashing);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (!this.level().isClientSide && isBusy()) {
            super.travel(Vec3.ZERO);
        } else {
            super.travel(travelVector);
        }
    }

    // =================================================================== урон

    private static boolean bypasses(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY); // /kill и бездна работают как обычно
    }

    /** Любой удар снимает не больше 100 виртуальных здоровья (то есть 12.5 реального). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (bypasses(source)) return super.hurt(source, amount);
        return super.hurt(source, Math.min(amount, MAX_DAMAGE_TAKEN) / HEALTH_SCALE);
    }

    /** Броня и защитные чары не уменьшают урон: ограничение в 100 должно быть предсказуемым. */
    @Override
    protected float getDamageAfterArmorAbsorb(DamageSource source, float amount) {
        return amount;
    }

    @Override
    protected float getDamageAfterMagicAbsorb(DamageSource source, float amount) {
        return amount;
    }

    /** Страховка: даже если чужой мод усилил урон уже после нашего ограничения, за раз уходит не больше 100. */
    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        float before = this.getHealth();
        super.actuallyHurt(source, amount);
        if (!bypasses(source)) {
            float floor = before - MAX_DAMAGE_TAKEN / HEALTH_SCALE;
            if (this.getHealth() < floor) this.setHealth(floor);
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (riseTicks > 0) return true;
        if (transitionTicks > 0 && !bypasses(source)) return true;
        return super.isInvulnerableTo(source);
    }

    /** Первую стадию нельзя пройти «насквозь»: смерть превращается в переход на вторую стадию. */
    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && getPhase() == 1 && !bypasses(source)) {
            this.setHealth((float) (PHASE2_HEALTH / HEALTH_SCALE));
            startTransition();
            return;
        }
        super.die(source);
    }

    // =================================================================== появление

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (spawnType == MobSpawnType.STRUCTURE) return data;
        this.riseBaseY = this.getY();
        this.riseTicks = RISE_TICKS;
        this.setPos(this.getX(), this.riseBaseY - 6.0D, this.getZ());
        return data;
    }

    @Override
    public void aiStep() {
        if (riseTicks > 0) {
            rise();
            return;
        }
        super.aiStep();
        if (this.level().isClientSide && this.random.nextInt(4) == 0) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL,
                    this.getRandomX(0.8D), this.getRandomY(), this.getRandomZ(0.8D), 0.0D, 0.05D, 0.0D);
        }
    }

    private void rise() {
        riseTicks--;
        double progress = 1.0D - (double) riseTicks / RISE_TICKS;
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(this.getX(), riseBaseY - 6.0D + 6.0D * progress, this.getZ());
        if (this.level() instanceof ServerLevel server) {
            BlockState ground = server.getBlockState(BlockPos.containing(this.getX(), riseBaseY - 1.0D, this.getZ()));
            if (ground.isAir()) ground = Blocks.STONE.defaultBlockState();
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    this.getX(), riseBaseY + 0.1D, this.getZ(), 16, 1.6D, 0.1D, 1.6D, 0.15D);
            if (riseTicks % 8 == 0) this.playSound(SoundEvents.GRAVEL_BREAK, 2.5F, 0.5F);
            if (riseTicks == 0) {
                this.noPhysics = false;
                this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 4.0F, 0.6F);
                server.sendParticles(ParticleTypes.EXPLOSION, this.getX(), riseBaseY + 0.5D, this.getZ(), 8, 2.0D, 0.2D, 2.0D, 0.0D);
            }
        }
    }

    // =================================================================== тик

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && isBusy()) {
            Vec3 d = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, d.y, 0.0D);
        }
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
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));
        if (riseTicks > 0) return;

        if (getPhase() == 1 && this.getDisplayHealth() <= PHASE2_HEALTH) startTransition();

        tickStrikes();

        if (transitionTicks > 0) {
            tickTransition();
            return;
        }
        if (biteTimer > 0 && --biteTimer == 0 && ability == AB_NONE) setStage(ST_IDLE);
        if (ability != AB_NONE) {
            tickAbility();
        } else if (cooldown > 0) {
            cooldown--;
        } else {
            chooseAbility();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (isBusy()) return false;
        boolean ok = super.doHurtTarget(target);
        if (ok) {
            biteTimer = 8;
            setStage(ST_BITE);
        }
        return ok;
    }

    // ---------- переход на 2 стадию: вой, неуязвимость и единственное лечение на 1000 ----------
    private void startTransition() {
        this.entityData.set(DATA_PHASE, 2);
        this.transitionTicks = TRANSITION_TICKS;
        this.healLeft = HEAL_AMOUNT; // ровно один раз: фаза 2 уже не вернётся в фазу 1
        this.ability = AB_NONE;
        this.dashing = false;
        this.strikes.clear();
        this.getNavigation().stop();
        setStage(ST_TRANSITION);
        this.bossEvent.setName(Component.translatable("entity.celestial.fenrir.howl"));
        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 5.0F, 0.5F);
        this.playSound(SoundEvents.WITHER_SPAWN, 3.0F, 0.6F);
        spawnEffect(FenrirEffect.KIND_HOWL, getX(), getY(), getZ(), TRANSITION_TICKS, 1.7F, true);
        hurtArea(getX(), getY(), getZ(), 12.0F, ATTACK_DAMAGE, 2.0D, 0.8D);
        announce(Component.translatable("celestial.fenrir.phase2"));
    }

    private void tickTransition() {
        transitionTicks--;
        this.getNavigation().stop();
        if (healLeft > 0.0F) {
            float step = Math.min(healLeft, HEAL_AMOUNT / (TRANSITION_TICKS - 10));
            healLeft -= step;
            this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + step / HEALTH_SCALE));
        }
        if (this.level() instanceof ServerLevel level) {
            if (transitionTicks % 6 == 0) {
                level.sendParticles(ParticleTypes.HEART, getRandomX(1.0D), getY() + 1.0D + random.nextDouble() * 4.0D,
                        getRandomZ(1.0D), 1, 0.2D, 0.2D, 0.2D, 0.0D);
            }
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 2.5D, getZ(), 6, 1.5D, 2.0D, 1.5D, 0.1D);
        }
        if (transitionTicks <= 0) {
            if (healLeft > 0.0F) {
                this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + healLeft / HEALTH_SCALE));
                healLeft = 0.0F;
            }
            this.bossEvent.setName(Component.translatable("entity.celestial.fenrir"));
            setStage(ST_IDLE);
            this.cooldown = 30;
        }
    }

    // ---------- выбор способности ----------
    private boolean phase2() { return getPhase() >= 2; }

    private void chooseAbility() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            cooldown = 20;
            return;
        }
        double dist = Math.sqrt(this.distanceToSqr(target));
        int wLeap = (dist > 7.0D && dist < 32.0D && this.hasLineOfSight(target)) ? (phase2() ? 6 : 5) : 0;
        int wSpikes = dist < 18.0D ? 4 : 2;
        int wHowl = dist < 14.0D ? 3 : 1;
        if (lastAbility == AB_LEAP) wLeap = 0;
        if (lastAbility == AB_SPIKES) wSpikes = 0;
        if (lastAbility == AB_HOWL) wHowl = 0;
        int total = wLeap + wSpikes + wHowl;
        if (total <= 0) {
            cooldown = 15;
            return;
        }
        int roll = this.random.nextInt(total);
        int pick = roll < wLeap ? AB_LEAP : (roll < wLeap + wSpikes ? AB_SPIKES : AB_HOWL);
        beginAbility(pick);
    }

    private void beginAbility(int a) {
        this.ability = a;
        this.abilityTick = 0;
        this.leapPhase = 0;
        this.dashing = false;
        this.getNavigation().stop();
    }

    private void endAbility(int baseCooldown) {
        this.lastAbility = ability;
        this.ability = AB_NONE;
        this.abilityTick = 0;
        this.dashing = false;
        this.biteTimer = 0;
        setStage(ST_IDLE);
        this.cooldown = phase2() ? baseCooldown / 2 + random.nextInt(20) : baseCooldown + random.nextInt(40);
    }

    private void tickAbility() {
        abilityTick++;
        LivingEntity target = this.getTarget();
        if (target != null && !target.isAlive()) target = null;
        switch (ability) {
            case AB_LEAP -> tickLeap(target);
            case AB_SPIKES -> tickSpikes(target);
            case AB_HOWL -> tickHowl();
            default -> endAbility(40);
        }
    }

    private void face(@Nullable LivingEntity target) {
        if (target == null) return;
        this.getLookControl().setLookAt(target, 40.0F, 40.0F);
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        this.setYRot(Mth.rotLerp(0.35F, this.getYRot(), yaw));
        this.yBodyRot = this.getYRot();
    }

    // ---------- прыжок-рывок ----------
    private void tickLeap(@Nullable LivingEntity target) {
        if (leapPhase == 0) {
            setStage(ST_CROUCH);
            face(target);
            if (this.level() instanceof ServerLevel level && abilityTick % 3 == 0) {
                level.sendParticles(ParticleTypes.CRIMSON_SPORE, getX(), getY() + 0.2D, getZ(), 8, 1.8D, 0.1D, 1.8D, 0.02D);
            }
            if (abilityTick == 1) this.playSound(SoundEvents.WOLF_GROWL, 4.0F, 0.5F);
            if (abilityTick >= LEAP_WINDUP) {
                if (target == null) {
                    endAbility(40);
                    return;
                }
                Vec3 d = new Vec3(target.getX() - getX(), 0.0D, target.getZ() - getZ());
                double dist = d.length();
                leapDir = dist < 1.0E-3D ? Vec3.directionFromRotation(0.0F, getYRot()).multiply(1, 0, 1).normalize() : d.scale(1.0D / dist);
                leapSpeed = Mth.clamp((dist + 3.0D) / LEAP_DASH, 0.8D, 2.0D);
                leapPhase = 1;
                dashTick = 0;
                dashing = true;
                leapHit.clear();
                setStage(ST_LEAP);
                this.setDeltaMovement(leapDir.x * leapSpeed, 0.5D + (dist > 15.0D ? 0.15D : 0.0D), leapDir.z * leapSpeed);
                this.hasImpulse = true;
                this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 3.0F, 1.1F);
                spawnEffect(FenrirEffect.KIND_LEAP, getX(), getY(), getZ(), LEAP_DASH + 8, 1.0F, true);
            }
            return;
        }
        if (leapPhase == 1) {
            dashTick++;
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(leapDir.x * leapSpeed, v.y, leapDir.z * leapSpeed);
            AABB box = this.getBoundingBox().inflate(1.2D, 0.4D, 1.2D);
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
                if (leapHit.add(e.getUUID())) strike(e, ATTACK_DAMAGE, 1.6D, 0.55D, leapDir.x, leapDir.z);
            }
            if (this.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5D, getZ(), 5, 1.2D, 1.0D, 1.2D, 0.05D);
            }
            boolean landed = this.onGround() && dashTick > 4;
            if (dashTick >= LEAP_DASH || landed || (this.horizontalCollision && dashTick > 3)) {
                dashing = false;
                leapPhase = 2;
                abilityTick = 0;
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.2D, 1.0D, 0.2D));
                setStage(ST_IDLE);
                if (this.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3D, getZ(), 4, 1.5D, 0.1D, 1.5D, 0.0D);
                }
                this.playSound(SoundEvents.DRAGON_FIREBALL_EXPLODE, 2.0F, 0.7F);
            }
            return;
        }
        if (abilityTick >= LEAP_RECOVER) endAbility(60);
    }

    // ---------- шипы ----------
    private void tickSpikes(@Nullable LivingEntity target) {
        setStage(ST_SPIKES);
        face(target);
        if (abilityTick == 1) {
            burstCenters.clear();
            burstCenters.add(new Vec3(getX(), getY(), getZ()));
            if (target != null) burstCenters.add(groundPos(target.getX(), target.getY(), target.getZ()));
            if (phase2()) {
                for (int i = 0; i < 6; i++) {
                    double a = Math.PI * 2.0D / 6.0D * i + random.nextDouble() * 0.4D;
                    burstCenters.add(groundPos(getX() + Math.cos(a) * 10.0D, getY(), getZ() + Math.sin(a) * 10.0D));
                }
            }
            this.playSound(SoundEvents.WOLF_GROWL, 4.0F, 0.4F);
        }
        if (target != null && abilityTick < SPIKE_WINDUP / 2 && burstCenters.size() > 1) {
            burstCenters.set(1, groundPos(target.getX(), target.getY(), target.getZ())); // метка следует за игроком, потом замирает
        }
        if (this.level() instanceof ServerLevel level && abilityTick < SPIKE_WINDUP) {
            if (abilityTick % 3 == 0) {
                for (Vec3 c : burstCenters) {
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 0.1D, c.z, 10, 3.0D, 0.05D, 3.0D, 0.03D);
                    level.sendParticles(ParticleTypes.CRIMSON_SPORE, c.x, c.y + 0.1D, c.z, 6, 3.2D, 0.05D, 3.2D, 0.02D);
                }
            }
        }
        if (abilityTick == SPIKE_WINDUP) {
            this.playSound(SoundEvents.EVOKER_CAST_SPELL, 4.0F, 0.5F);
            for (Vec3 c : burstCenters) {
                spawnEffect(FenrirEffect.KIND_SPIKES, c.x, c.y, c.z, 36, 1.0F, false);
                strikes.add(new Strike(c.x, c.y, c.z, 8, 4.6F));
            }
        }
        if (abilityTick >= SPIKE_TOTAL) endAbility(70);
    }

    // ---------- вой ----------
    private void tickHowl() {
        setStage(ST_HOWL);
        if (abilityTick == 1) {
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 5.0F, 0.6F);
            spawnEffect(FenrirEffect.KIND_HOWL, getX(), getY(), getZ(), HOWL_TOTAL - 4, 1.4F, true);
        }
        if (abilityTick == HOWL_WINDUP) {
            float radius = phase2() ? 13.0F : 10.0F;
            hurtArea(getX(), getY(), getZ(), radius, ATTACK_DAMAGE, 1.9D, 0.7D);
            this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 5.0F, 0.7F);
            if (this.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 2.5D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.0D, getZ(), 80, radius * 0.5D, 0.6D, radius * 0.5D, 0.3D);
            }
        }
        if (abilityTick >= HOWL_TOTAL) endAbility(70);
    }

    // =================================================================== урон по области

    private boolean canHit(LivingEntity e) {
        if (e == this || !e.isAlive()) return false;
        if (e instanceof Fenrir || e instanceof GuardianOfGods || e instanceof CursedBoss
                || e instanceof CelestiaGod || e instanceof AllKnowingAngel) return false;
        return !(e instanceof Player p && p.isSpectator());
    }

    private void strike(LivingEntity e, float damage, double kb, double kbUp, double dx, double dz) {
        if (e.hurt(this.damageSources().indirectMagic(this, this), damage)) {
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1.0E-3D) {
                dx = 0.0D;
                dz = 0.0D;
                len = 1.0D;
            }
            e.setDeltaMovement(e.getDeltaMovement().add(dx / len * kb, kbUp, dz / len * kb));
            e.hurtMarked = true;
        }
    }

    private void hurtArea(double x, double y, double z, float radius, float damage, double kb, double kbUp) {
        AABB box = new AABB(x - radius, y - 1.5D, z - radius, x + radius, y + 5.0D, z + radius);
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
            double dx = e.getX() - x, dz = e.getZ() - z;
            if (dx * dx + dz * dz > (double) radius * radius) continue;
            strike(e, damage, kb, kbUp, dx, dz);
        }
    }

    private void tickStrikes() {
        if (strikes.isEmpty()) return;
        Iterator<Strike> it = strikes.iterator();
        while (it.hasNext()) {
            Strike s = it.next();
            if (--s.delay > 0) continue;
            it.remove();
            hurtArea(s.x, s.y, s.z, s.radius, ATTACK_DAMAGE, 0.9D, 0.8D);
            if (this.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.EXPLOSION, s.x, s.y + 0.5D, s.z, 3, 2.0D, 0.2D, 2.0D, 0.0D);
                level.sendParticles(ParticleTypes.CRIMSON_SPORE, s.x, s.y + 0.5D, s.z, 20, 2.5D, 0.8D, 2.5D, 0.05D);
            }
        }
    }

    // =================================================================== вспомогательное

    private void spawnEffect(int kind, double x, double y, double z, int life, float scale, boolean follow) {
        if (!(this.level() instanceof ServerLevel level)) return;
        FenrirEffect fx = ModEntities.FENRIR_EFFECT.get().create(level);
        if (fx == null) return;
        fx.setup(kind, life, scale, follow ? this : null);
        fx.moveTo(x, y, z, this.getYRot(), 0.0F);
        level.addFreshEntity(fx);
    }

    private Vec3 groundPos(double x, double startY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(startY) + 2, Mth.floor(z));
        for (int i = 0; i < 10; i++) {
            BlockPos below = pos.below();
            boolean solidBelow = !this.level().getBlockState(below).getCollisionShape(this.level(), below).isEmpty();
            boolean freeHere = this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty();
            if (solidBelow && freeHere) return new Vec3(x, pos.getY(), z);
            pos.move(Direction.DOWN);
        }
        return new Vec3(x, startY, z);
    }

    private void announce(Component message) {
        if (!(this.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(this) < 96.0D * 96.0D) p.sendSystemMessage(message);
        }
    }

    // =================================================================== сохранение

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("FenrirPhase", getPhase());
        tag.putInt("FenrirTransition", transitionTicks);
        tag.putFloat("FenrirHealLeft", healLeft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FenrirPhase")) this.entityData.set(DATA_PHASE, Math.max(1, tag.getInt("FenrirPhase")));
        this.transitionTicks = tag.getInt("FenrirTransition");
        this.healLeft = tag.getFloat("FenrirHealLeft");
        if (transitionTicks > 0) {
            setStage(ST_TRANSITION);
            this.bossEvent.setName(Component.translatable("entity.celestial.fenrir.howl"));
        }
    }

    // =================================================================== звуки

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WOLF_GROWL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.45F + this.random.nextFloat() * 0.1F;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.RAVAGER_STEP, 1.2F, 0.6F);
    }
}
