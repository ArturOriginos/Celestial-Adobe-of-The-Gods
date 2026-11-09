package com.example.celestial.entity;

import com.example.celestial.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Всезнающий Ангел: босс с 7500 здоровья. Не двигается, стоит и смотрит на игрока.
 * Атаки: 5 тёмных сфер, которые летят за игроком, пока их не уничтожат (по 15 урона), и лазер из глаза
 * (120 урона в секунду) с задержкой: сначала тонкий луч-предупреждение следует за игроком, затем замирает,
 * и только после этого лазер выстреливает, так что от него можно убежать. Логики появления нет:
 * босса вызывают командой /summon celestial:all_knowing_angel или яйцом призыва.
 */
public class AllKnowingAngel extends Monster {
    public static final double MAX_HEALTH = 7500.0D;

    /** Сколько сфер за одну атаку и не больше скольких сфер одновременно. */
    private static final int ORBS_PER_CAST = 5;
    private static final int MAX_ORBS_ALIVE = 10;
    /** Лазер: заряд (тики), после какого тика прицел замирает, длительность, урон за удар каждые 5 тиков = 40 в секунду. */
    private static final int CHARGE_TICKS = 36;
    private static final int AIM_FREEZE_TICKS = 12;
    private static final int FIRE_TICKS = 40;
    private static final float LASER_DAMAGE_PER_HIT = 30.0F; // 120 урона в секунду (было 40, x3)
    private static final double LASER_RANGE = 64.0D;
    private static final int ORB_WINDUP_TICKS = 30;

    private static final EntityDataAccessor<Integer> LASER_STATE =
            SynchedEntityData.defineId(AllKnowingAngel.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LASER_YAW =
            SynchedEntityData.defineId(AllKnowingAngel.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LASER_PITCH =
            SynchedEntityData.defineId(AllKnowingAngel.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LASER_LENGTH =
            SynchedEntityData.defineId(AllKnowingAngel.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LASER_PROGRESS =
            SynchedEntityData.defineId(AllKnowingAngel.class, EntityDataSerializers.FLOAT);

    /** 0 - ожидание, 1 - накопление сфер, 2 - заряд лазера, 3 - выстрел лазера. */
    private int phase;
    private int phaseTick;
    private int cooldown = 60;
    private boolean lastWasOrbs;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.celestial.all_knowing_angel"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    public AllKnowingAngel(EntityType<? extends AllKnowingAngel> type, Level level) {
        super(type, level);
        this.xpReward = 3000;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LASER_STATE, 0);
        builder.define(LASER_YAW, 0.0F);
        builder.define(LASER_PITCH, 0.0F);
        builder.define(LASER_LENGTH, 0.0F);
        builder.define(LASER_PROGRESS, 0.0F);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // ---------- данные лазера для рендера ----------
    public int getLaserState() {
        return this.entityData.get(LASER_STATE);
    }

    public float getLaserYaw() {
        return this.entityData.get(LASER_YAW);
    }

    public float getLaserPitch() {
        return this.entityData.get(LASER_PITCH);
    }

    public float getLaserLength() {
        return this.entityData.get(LASER_LENGTH);
    }

    public float getLaserProgress() {
        return this.entityData.get(LASER_PROGRESS);
    }

    /** Глаз босса в мире: чуть впереди тела и на высоте зрачка модели. */
    public Vec3 getEyeOrigin() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
        return this.position().add(forward.x, 2.1D, forward.z);
    }

    // ---------- неподвижность ----------
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {}

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    /** Каждый удар чужой сущности (игрок, моб, стрела) наносит ровно 100 урона, броня и эффекты его не меняют. */
    public static final float FIXED_HIT_DAMAGE = 100.0F;

    private static boolean isFixedHit(DamageSource source) {
        return source.getEntity() != null && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (amount > 0.0F && isFixedHit(source)) amount = FIXED_HIT_DAMAGE;
        return super.hurt(source, amount);
    }

    @Override
    protected float getDamageAfterArmorAbsorb(DamageSource source, float amount) {
        return isFixedHit(source) ? amount : super.getDamageAfterArmorAbsorb(source, amount);
    }

    @Override
    protected float getDamageAfterMagicAbsorb(DamageSource source, float amount) {
        return isFixedHit(source) ? amount : super.getDamageAfterMagicAbsorb(source, amount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING) || super.isInvulnerableTo(source);
    }

    // ---------- ИИ ----------
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || this.distanceToSqr(target) > 96.0D * 96.0D)) target = null;

        boolean canTurn = phase == 0 || phase == 1 || (phase == 2 && phaseTick < CHARGE_TICKS - AIM_FREEZE_TICKS);
        if (target != null && canTurn) this.faceTarget(target);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();

        switch (phase) {
            case 0 -> tickIdle(target);
            case 1 -> tickOrbs(target);
            case 2 -> tickLaserCharge(target);
            default -> tickLaserFire();
        }
    }

    private void faceTarget(LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), want, 8.0F));
        this.setXRot(0.0F);
    }

    private void tickIdle(LivingEntity target) {
        if (target == null) {
            cooldown = Math.max(cooldown, 40);
            return;
        }
        if (--cooldown > 0) return;
        boolean orbsOk = countOrbs() <= MAX_ORBS_ALIVE - ORBS_PER_CAST;
        boolean orbs = orbsOk && (lastWasOrbs ? this.random.nextFloat() < 0.3F : this.random.nextFloat() < 0.6F);
        if (orbs) startOrbs();
        else startLaser();
    }

    private int countOrbs() {
        return this.level().getEntitiesOfClass(DarkOrb.class, this.getBoundingBox().inflate(96.0D),
                orb -> this.getUUID().equals(orb.getOwnerId())).size();
    }

    // ---------- сферы ----------
    private void startOrbs() {
        phase = 1;
        phaseTick = 0;
        lastWasOrbs = true;
        this.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 3.0F, 0.6F);
    }

    private void tickOrbs(LivingEntity target) {
        phaseTick++;
        if (this.level() instanceof ServerLevel server) {
            Vec3 eye = this.getEyeOrigin();
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, eye.x, eye.y + 0.8D, eye.z, 6, 1.6D, 1.0D, 1.6D, 0.06D);
        }
        if (phaseTick < ORB_WINDUP_TICKS) return;
        if (target != null) spawnOrbs(target);
        phase = 0;
        cooldown = 90;
    }

    private void spawnOrbs(LivingEntity target) {
        float start = this.random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < ORBS_PER_CAST; i++) {
            float ang = start + Mth.TWO_PI * i / ORBS_PER_CAST;
            DarkOrb orb = ModEntities.DARK_ORB.get().create(this.level());
            if (orb == null) continue;
            orb.setPos(this.getX() + Mth.cos(ang) * 2.8D, this.getY() + 3.0D + this.random.nextDouble() * 1.5D,
                    this.getZ() + Mth.sin(ang) * 2.8D);
            orb.setup(this, target);
            this.level().addFreshEntity(orb);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 3.0F, 0.5F);
    }

    // ---------- лазер ----------
    private void startLaser() {
        phase = 2;
        phaseTick = 0;
        lastWasOrbs = false;
        this.entityData.set(LASER_STATE, 1);
        this.entityData.set(LASER_PROGRESS, 0.0F);
        this.playSound(SoundEvents.BEACON_ACTIVATE, 4.0F, 0.5F);
    }

    private void updateAim(LivingEntity target) {
        Vec3 eye = this.getEyeOrigin();
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(eye);
        double horizontal = Math.sqrt(to.x * to.x + to.z * to.z);
        float yaw = (float) (Mth.atan2(-to.x, to.z) * Mth.RAD_TO_DEG);
        float pitch = (float) -(Mth.atan2(to.y, horizontal) * Mth.RAD_TO_DEG);
        this.entityData.set(LASER_YAW, yaw);
        this.entityData.set(LASER_PITCH, pitch);
    }

    /** Лазер проходит сквозь блоки: всегда на полную дальность, препятствия его не останавливают. */
    private double computeLength() {
        return LASER_RANGE;
    }

    private void tickLaserCharge(LivingEntity target) {
        if (target == null && phaseTick < CHARGE_TICKS - AIM_FREEZE_TICKS) {
            endLaser();
            return;
        }
        if (phaseTick < CHARGE_TICKS - AIM_FREEZE_TICKS && target != null) updateAim(target);
        this.entityData.set(LASER_LENGTH, (float) computeLength());
        this.entityData.set(LASER_PROGRESS, (float) phaseTick / CHARGE_TICKS);
        if (phaseTick % 10 == 0) this.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 3.0F, 0.6F + phaseTick / 80.0F);
        phaseTick++;
        if (phaseTick >= CHARGE_TICKS) {
            phase = 3;
            phaseTick = 0;
            this.entityData.set(LASER_STATE, 2);
            this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 4.0F, 0.6F);
        }
    }

    private void tickLaserFire() {
        double length = computeLength();
        this.entityData.set(LASER_LENGTH, (float) length);
        if (phaseTick % 5 == 0) damageAlongBeam(length);
        if (phaseTick % 8 == 0) this.playSound(SoundEvents.BEACON_AMBIENT, 4.0F, 0.5F);
        if (this.level() instanceof ServerLevel server) {
            Vec3 eye = this.getEyeOrigin();
            Vec3 dir = Vec3.directionFromRotation(this.getLaserPitch(), this.getLaserYaw());
            Vec3 end = eye.add(dir.scale(length));
            server.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 4, 0.5D, 0.5D, 0.5D, 0.1D);
        }
        phaseTick++;
        if (phaseTick >= FIRE_TICKS) endLaser();
    }

    /** 30 урона каждые 5 тиков = 120 в секунду; урон магический, броня его не уменьшает. */
    private void damageAlongBeam(double length) {
        Vec3 eye = this.getEyeOrigin();
        Vec3 dir = Vec3.directionFromRotation(this.getLaserPitch(), this.getLaserYaw());
        Vec3 end = eye.add(dir.scale(length));
        AABB area = new AABB(eye, end).inflate(1.5D);
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, area,
                x -> x != this && x.isAlive() && !(x instanceof AllKnowingAngel))) {
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            AABB box = e.getBoundingBox().inflate(0.45D);
            if (box.contains(eye) || box.clip(eye, end).isPresent()) {
                e.invulnerableTime = 0;
                e.hurt(this.damageSources().indirectMagic(this, this), LASER_DAMAGE_PER_HIT);
            }
        }
    }

    private void endLaser() {
        phase = 0;
        phaseTick = 0;
        cooldown = 100;
        this.entityData.set(LASER_STATE, 0);
        this.entityData.set(LASER_PROGRESS, 0.0F);
    }

    // ---------- полоска здоровья босса ----------
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

    // ---------- звуки ----------
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ELDER_GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ELDER_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ELDER_GUARDIAN_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }
}
