package com.example.celestial.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Цербер: трёхголовый пёс Аида. 10000 здоровья, удары в среднем по 80 урона (укус 80, рывок 88, ударная волна 72,
 * огненное дыхание по 40 каждые полсекунды). Ломает блоки на своём пути (если в мире включён mobGriefing).
 * Когда здоровье падает до 5000, Цербер ревёт и 10 секунд бессмертен: за это время он полностью восстанавливает
 * здоровье до 10000, но только один раз. Дроп: Сердце Цербера. Призывается Запретным плодом.
 */
public class Cerberus extends Monster {
    public static final double MAX_HEALTH = 10000.0D;
    public static final double PHASE2_HEALTH = 5000.0D;
    public static final int TRANSITION_TICKS = 200; // 10 секунд бессмертия и лечения
    public static final int SPAWN_TICKS = 60;

    public static final float BITE_DAMAGE = 80.0F;
    public static final float DASH_DAMAGE = 88.0F;
    public static final float SLAM_DAMAGE = 72.0F;
    public static final float FIRE_DAMAGE = 40.0F;

    public static final int ST_IDLE = 0, ST_BITE = 1, ST_DASH_WINDUP = 2, ST_DASH = 3, ST_FIRE = 4,
            ST_SLAM_WINDUP = 5, ST_SLAM = 6, ST_ROAR = 7;

    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(Cerberus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STAGE_TICK =
            SynchedEntityData.defineId(Cerberus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(Cerberus.class, EntityDataSerializers.INT);

    private static final int DASH_WINDUP_TICKS = 20, DASH_TICKS = 16, FIRE_TICKS = 60,
            SLAM_WINDUP_TICKS = 25, SLAM_TICKS = 22;
    private static final double DASH_SPEED = 1.5D;
    private static final double SLAM_RADIUS = 16.0D;
    private static final double FIRE_RANGE = 19.0D;

    private int stageTick = 0;
    private int spawnTicks = 0;
    private int transitionTicks = 0;
    private float healPerTick = 0.0F;
    private int cooldown = 60;
    private int biteTimer = 0;
    private Vec3 dashDir = Vec3.ZERO;
    private final java.util.Set<java.util.UUID> hitOnce = new java.util.HashSet<>();

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.celestial.cerberus"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    public Cerberus(EntityType<? extends Cerberus> type, Level level) {
        super(type, level);
        this.xpReward = 5000;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, BITE_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
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
        builder.define(DATA_STAGE_TICK, 0);
        builder.define(DATA_PHASE, 1);
    }

    // =================================================================== состояние

    public int getStage() { return this.entityData.get(DATA_STAGE); }

    public int getStageTick() { return this.entityData.get(DATA_STAGE_TICK); }

    public int getPhase() { return this.entityData.get(DATA_PHASE); }

    private void setStage(int stage) {
        stageTick = 0;
        this.entityData.set(DATA_STAGE_TICK, 0);
        if (getStage() != stage) this.entityData.set(DATA_STAGE, stage);
    }

    private boolean isAbilityStage() {
        int s = getStage();
        return s == ST_DASH_WINDUP || s == ST_DASH || s == ST_FIRE || s == ST_SLAM_WINDUP || s == ST_SLAM;
    }

    /** Ревёт, колдует или готовит атаку: стоит на месте (рывок двигает себя сам). */
    private boolean isBusy() {
        return spawnTicks > 0 || transitionTicks > 0 || (isAbilityStage() && getStage() != ST_DASH);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (!this.level().isClientSide && isBusy()) super.travel(Vec3.ZERO);
        else super.travel(travelVector);
    }

    // =================================================================== урон и смерть

    private static boolean bypasses(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY); // /kill и бездна работают как обычно
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (spawnTicks > 0) return true;
        if (transitionTicks > 0 && !bypasses(source)) return true;
        return super.isInvulnerableTo(source);
    }

    /** Первая фаза не проходится «насквозь»: смертельный удар превращается в переход на вторую (один раз). */
    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && getPhase() == 1 && !bypasses(source)) {
            this.setHealth((float) PHASE2_HEALTH);
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
        if (spawnType != MobSpawnType.STRUCTURE) {
            this.spawnTicks = SPAWN_TICKS;
            setStage(ST_ROAR);
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 5.0F, 0.5F);
        }
        return data;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.FLAME, this.getRandomX(0.9D), this.getRandomY(), this.getRandomZ(0.9D),
                    0.0D, 0.04D, 0.0D);
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

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));

        if (spawnTicks > 0) {
            spawnTicks--;
            if (spawnTicks % 10 == 0) this.playSound(SoundEvents.WOLF_GROWL, 3.0F, 0.5F);
            if (spawnTicks == 0) setStage(ST_IDLE);
            return;
        }
        if (getPhase() == 1 && this.getHealth() <= PHASE2_HEALTH) startTransition();
        if (transitionTicks > 0) {
            tickTransition();
            return;
        }

        if (this.tickCount % 4 == 0) breakBlocks();

        this.entityData.set(DATA_STAGE_TICK, ++stageTick);
        if (biteTimer > 0 && --biteTimer == 0 && getStage() == ST_BITE) setStage(ST_IDLE);
        if (isAbilityStage()) {
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
        if (ok && getStage() == ST_IDLE) {
            biteTimer = 8;
            setStage(ST_BITE);
            this.playSound(SoundEvents.WOLF_GROWL, 2.0F, 0.6F);
        }
        return ok;
    }

    // =================================================================== разрушение блоков

    /** Цербер крушит всё, что попадает в его тело (кроме неразрушимых блоков и жидкостей), если mobGriefing включён. */
    private void breakBlocks() {
        if (!(this.level() instanceof ServerLevel server) || !server.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        AABB box = this.getBoundingBox().inflate(0.9D, 0.0D, 0.9D).move(0.0D, 0.55D, 0.0D);
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
            BlockState state = server.getBlockState(pos);
            if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.is(BlockTags.WITHER_IMMUNE)
                    || state.getDestroySpeed(server, pos) < 0.0F) continue;
            server.destroyBlock(pos, false, this);
        }
    }

    // =================================================================== вторая фаза

    /** Ровно один раз: рёв, 10 секунд бессмертия и полное восстановление здоровья. */
    private void startTransition() {
        this.entityData.set(DATA_PHASE, 2);
        this.transitionTicks = TRANSITION_TICKS;
        this.healPerTick = (float) (this.getMaxHealth() - this.getHealth()) / TRANSITION_TICKS;
        this.dashDir = Vec3.ZERO;
        setStage(ST_ROAR);
        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 6.0F, 0.45F);
    }

    private void tickTransition() {
        transitionTicks--;
        this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + healPerTick));
        if (this.level() instanceof ServerLevel server) {
            if (transitionTicks % 4 == 0) {
                server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 2.5D, getZ(), 12, 1.6D, 1.8D, 1.6D, 0.04D);
            }
            if (transitionTicks % 25 == 0) this.playSound(SoundEvents.WOLF_GROWL, 4.0F, 0.4F);
        }
        if (transitionTicks == 0) {
            this.setHealth(this.getMaxHealth());
            setStage(ST_IDLE);
            cooldown = 20;
        }
    }

    // =================================================================== способности

    private void chooseAbility() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;
        double dist = this.distanceTo(target);
        java.util.List<Integer> options = new java.util.ArrayList<>();
        if (dist > 7.0D && dist < 30.0D) options.add(ST_DASH_WINDUP);
        if (dist > 5.0D && dist < FIRE_RANGE) options.add(ST_FIRE);
        if (dist < 11.0D) options.add(ST_SLAM_WINDUP);
        if (options.isEmpty()) return; // вплотную работает обычный укус
        int pick = options.get(this.random.nextInt(options.size()));
        hitOnce.clear();
        setStage(pick);
        if (pick == ST_DASH_WINDUP) this.playSound(SoundEvents.WOLF_GROWL, 3.0F, 0.5F);
    }

    private void endAbility() {
        setStage(ST_IDLE);
        cooldown = getPhase() == 1 ? 60 + this.random.nextInt(40) : 30 + this.random.nextInt(30);
    }

    private void tickAbility() {
        LivingEntity target = this.getTarget();
        switch (getStage()) {
            case ST_DASH_WINDUP -> {
                faceTarget(target);
                if (stageTick >= DASH_WINDUP_TICKS) {
                    Vec3 dir = target != null ? target.position().subtract(this.position()) : this.getLookAngle();
                    dashDir = new Vec3(dir.x, 0.0D, dir.z).normalize();
                    hitOnce.clear();
                    setStage(ST_DASH);
                    this.playSound(SoundEvents.RAVAGER_ROAR, 4.0F, 0.6F);
                }
            }
            case ST_DASH -> {
                float yaw = (float) (Mth.atan2(dashDir.z, dashDir.x) * 180.0D / Math.PI) - 90.0F;
                this.setYRot(yaw);
                this.setYBodyRot(yaw);
                this.setYHeadRot(yaw);
                Vec3 d = this.getDeltaMovement();
                this.setDeltaMovement(dashDir.x * DASH_SPEED, d.y, dashDir.z * DASH_SPEED);
                hitArea(this.getBoundingBox().inflate(1.3D, 0.3D, 1.3D), DASH_DAMAGE, 1.8D, 0.5D);
                if (stageTick >= DASH_TICKS || (stageTick > 4 && this.horizontalCollision)) endAbility();
            }
            case ST_FIRE -> {
                faceTarget(target);
                if (stageTick % 5 == 0) fireBreath();
                if (stageTick >= FIRE_TICKS) endAbility();
            }
            case ST_SLAM_WINDUP -> {
                faceTarget(target);
                if (stageTick >= SLAM_WINDUP_TICKS) {
                    hitOnce.clear();
                    setStage(ST_SLAM);
                    this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 5.0F, 0.5F);
                }
            }
            case ST_SLAM -> {
                slamWave();
                if (stageTick >= SLAM_TICKS) endAbility();
            }
            default -> endAbility();
        }
    }

    private void faceTarget(@Nullable LivingEntity target) {
        if (target != null) this.getLookControl().setLookAt(target, 40.0F, 40.0F);
    }

    /** Огненное дыхание: конус перед мордой (около 19 блоков), урон и поджог. */
    private void fireBreath() {
        if (!(this.level() instanceof ServerLevel server)) return;
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        Vec3 mouth = this.position().add(forward.scale(2.4D)).add(0.0D, 3.0D, 0.0D);
        this.playSound(SoundEvents.BLAZE_SHOOT, 3.0F, 0.6F);
        for (int i = 1; i <= 8; i++) {
            Vec3 p = mouth.add(forward.scale(i * 2.2D));
            server.sendParticles(ParticleTypes.FLAME, p.x, p.y - i * 0.18D, p.z, 6, 0.4D + i * 0.12D, 0.3D + i * 0.1D, 0.4D + i * 0.12D, 0.02D);
        }
        AABB box = new AABB(mouth, mouth.add(forward.scale(FIRE_RANGE))).inflate(5.0D, 5.0D, 5.0D);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
            Vec3 to = e.position().add(0.0D, e.getBbHeight() * 0.5D, 0.0D).subtract(mouth);
            double along = to.dot(forward);
            if (along < 0.0D || along > FIRE_RANGE) continue;
            Vec3 flat = new Vec3(to.x, 0.0D, to.z);
            if (flat.lengthSqr() > 1.0E-4D && flat.normalize().dot(forward) < 0.88D) continue; // конус около 28 градусов
            e.hurt(this.damageSources().mobAttack(this), FIRE_DAMAGE);
            e.setRemainingFireTicks(100);
        }
    }

    /** Ударная волна: кольцо расходится от Цербера до 16 блоков, каждый получает урон один раз и подлетает. */
    private void slamWave() {
        if (!(this.level() instanceof ServerLevel server)) return;
        double radius = Math.min(SLAM_RADIUS, stageTick * (SLAM_RADIUS / 20.0D));
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(SLAM_RADIUS, 4.0D, SLAM_RADIUS), this::canHit)) {
            if (hitOnce.contains(e.getUUID())) continue;
            double dist = Math.sqrt((e.getX() - getX()) * (e.getX() - getX()) + (e.getZ() - getZ()) * (e.getZ() - getZ()));
            if (dist > radius + 0.8D || dist < radius - 2.4D) continue;
            hitOnce.add(e.getUUID());
            e.hurt(this.damageSources().mobAttack(this), SLAM_DAMAGE);
            Vec3 away = new Vec3(e.getX() - getX(), 0.0D, e.getZ() - getZ());
            away = away.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : away.normalize();
            knock(e, away, 1.2D, 0.7D);
        }
        if (stageTick % 3 == 0) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2D, getZ(), 18, radius * 0.5D, 0.1D, radius * 0.5D, 0.02D);
        }
    }

    private void hitArea(AABB box, float damage, double strength, double up) {
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, box, this::canHit)) {
            if (!hitOnce.add(e.getUUID())) continue;
            e.hurt(this.damageSources().mobAttack(this), damage);
            Vec3 dir = e.position().subtract(this.position());
            dir = new Vec3(dir.x, 0.0D, dir.z);
            knock(e, dir.lengthSqr() < 1.0E-4D ? dashDir : dir.normalize(), strength, up);
        }
    }

    /** Цели Цербера: всё живое, кроме монстров (чтобы не добивать своих) и игроков в творческом режиме. */
    private boolean canHit(LivingEntity e) {
        if (e == this || !e.isAlive() || e instanceof Cerberus || e instanceof Monster) return false;
        return !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    private static void knock(LivingEntity e, Vec3 dir, double strength, double up) {
        e.setDeltaMovement(e.getDeltaMovement().add(dir.x * strength, up, dir.z * strength));
        e.hurtMarked = true;
    }

    // =================================================================== звуки

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.WOLF_GROWL; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WOLF_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }

    // =================================================================== сохранение

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("CerberusPhase", getPhase());
        tag.putInt("TransitionTicks", transitionTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_PHASE, tag.contains("CerberusPhase") ? Math.max(1, tag.getInt("CerberusPhase")) : 1);
        this.transitionTicks = tag.getInt("TransitionTicks");
        if (transitionTicks > 0) {
            this.healPerTick = (float) (this.getMaxHealth() - this.getHealth()) / transitionTicks;
            this.entityData.set(DATA_STAGE, ST_ROAR);
        }
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }
}
