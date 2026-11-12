package com.example.celestial.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.example.celestial.registry.ModEntities;

/**
 * Атаки и эффекты Аида. Один тип сущности на все модели из набора hades_boss:
 *  копьё пламени, тёмные колонны, извержение, кристальные шипы, кольцо цепей (ударные зоны с задержкой),
 *  череп-преследователь и комета (снаряды), ритуальный круг (только визуал).
 * Урон наносит сама сущность: зоны бьют один раз, когда проходит задержка (до этого идёт предупреждение:
 * зона видна и в ней вьются частицы), снаряды взрываются при попадании.
 */
public class HadesEffect extends Entity {
    public static final int KIND_SPEAR = 0, KIND_PILLARS = 1, KIND_ERUPTION = 2, KIND_SPIKES = 3, KIND_CHAINS = 4,
            KIND_CIRCLE = 5, KIND_SKULL = 6, KIND_COMET = 7;

    /** Время жизни, задержка до удара и радиус удара (в блоках при масштабе 1) по видам. */
    private static final int[] LIFE =   {36, 44, 44, 34, 58, 60, 140, 110};
    private static final int[] DELAY =  {24, 16, 16, 12, 40,  0,   0,   0};
    private static final float[] RADIUS = {1.8F, 3.5F, 2.6F, 2.8F, 2.4F, 0.0F, 3.0F, 3.5F};

    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(HadesEffect.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(HadesEffect.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(HadesEffect.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_DELAY =
            SynchedEntityData.defineId(HadesEffect.class, EntityDataSerializers.INT);

    private float damage = 0.0F;
    @Nullable
    private UUID ownerId;
    @Nullable
    private Entity follow;
    @Nullable
    private UUID targetId;
    private Vec3 direction = Vec3.ZERO;
    private boolean struck = false;
    private final Set<UUID> hit = new HashSet<>();

    public HadesEffect(EntityType<? extends HadesEffect> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    /** Создаёт эффект. lifeOverride == 0 - значение по виду; follow - ехать за владельцем (для ритуального круга). */
    @Nullable
    public static HadesEffect spawn(ServerLevel level, int kind, double x, double y, double z, float scale,
                                    @Nullable Entity owner, float damage, int lifeOverride, boolean follow) {
        HadesEffect e = ModEntities.HADES_EFFECT.get().create(level);
        if (e == null) return null;
        e.setPos(x, y, z);
        e.setYRot(level.random.nextFloat() * 360.0F);
        e.entityData.set(DATA_KIND, kind);
        e.entityData.set(DATA_LIFE, lifeOverride > 0 ? lifeOverride : LIFE[kind]);
        e.entityData.set(DATA_SCALE, scale);
        e.entityData.set(DATA_DELAY, DELAY[kind]);
        e.damage = damage;
        if (owner != null) {
            e.ownerId = owner.getUUID();
            if (follow) e.follow = owner;
        }
        level.addFreshEntity(e);
        return e;
    }

    public int getKind() { return this.entityData.get(DATA_KIND); }
    public int getLife() { return this.entityData.get(DATA_LIFE); }
    public float getScale() { return this.entityData.get(DATA_SCALE); }
    public int getDelay() { return this.entityData.get(DATA_DELAY); }

    public void launchHoming(LivingEntity target) {
        this.targetId = target.getUUID();
        Vec3 d = target.getBoundingBox().getCenter().subtract(this.position());
        this.direction = d.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, -1.0D, 0.0D) : d.normalize();
    }

    public void launchStraight(Vec3 dir) {
        this.direction = dir;
        orient(dir);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, KIND_SPEAR);
        builder.define(DATA_LIFE, 40);
        builder.define(DATA_SCALE, 1.0F);
        builder.define(DATA_DELAY, 0);
    }

    private boolean isProjectile() {
        int k = getKind();
        return k == KIND_SKULL || k == KIND_COMET;
    }

    private void orient(Vec3 v) {
        double horizontal = Math.sqrt(v.x * v.x + v.z * v.z);
        this.setYRot((float) (Mth.atan2(-v.x, v.z) * 180.0D / Math.PI));
        this.setXRot((float) -(Mth.atan2(v.y, horizontal) * 180.0D / Math.PI));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    // =================================================================== тик

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (!(this.level() instanceof ServerLevel server)) return;

        if (follow != null) {
            if (!follow.isAlive()) { this.discard(); return; }
            this.setPos(follow.getX(), follow.getY(), follow.getZ());
        }
        if (this.tickCount > getLife()) {
            this.discard();
            return;
        }
        if (isProjectile()) {
            tickProjectile(server);
        } else if (getKind() != KIND_CIRCLE) {
            tickZone(server);
        }
    }

    private LivingEntity owner(ServerLevel server) {
        if (ownerId != null && server.getEntity(ownerId) instanceof LivingEntity living) return living;
        return null;
    }

    private DamageSource source(ServerLevel server) {
        LivingEntity o = owner(server);
        return o != null ? this.damageSources().mobAttack(o) : this.damageSources().magic();
    }

    /** Зона: пока идёт задержка, показывает предупреждение, потом один раз бьёт всех внутри. */
    private void tickZone(ServerLevel server) {
        float radius = RADIUS[getKind()] * getScale();
        if (this.tickCount < getDelay()) {
            if (this.tickCount % 3 == 0) {
                for (int i = 0; i < 10; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2.0D;
                    server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(a) * radius, getY() + 0.1D,
                            getZ() + Math.sin(a) * radius, 1, 0.05D, 0.1D, 0.05D, 0.0D);
                }
            }
            return;
        }
        if (struck) return;
        struck = true;
        server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5D, getZ(), 40, radius * 0.5D, 0.6D, radius * 0.5D, 0.08D);
        this.playSound(getKind() == KIND_CHAINS ? SoundEvents.CHAIN_BREAK : SoundEvents.GENERIC_EXPLODE.value(),
                getKind() == KIND_CHAINS ? 2.0F : 3.0F, 0.6F);
        if (damage <= 0.0F) return;

        double height = getKind() == KIND_SPEAR ? 6.0D : 5.0D;
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(radius, height, radius), Hades::canHit)) {
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            if (dx * dx + dz * dz > radius * radius || e.getY() < getY() - 2.0D || e.getY() > getY() + height) continue;
            e.hurt(source(server), damage);
            Vec3 away = new Vec3(dx, 0.0D, dz);
            away = away.lengthSqr() < 1.0E-4D ? Vec3.ZERO : away.normalize();
            switch (getKind()) {
                case KIND_ERUPTION -> e.setDeltaMovement(e.getDeltaMovement().add(away.x * 1.4D, 0.9D, away.z * 1.4D));
                case KIND_CHAINS -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                default -> e.setDeltaMovement(e.getDeltaMovement().add(away.x * 0.4D, 0.6D, away.z * 0.4D));
            }
            e.hurtMarked = true;
        }
    }

    /** Череп преследует цель, комета летит по прямой; оба взрываются при попадании. */
    private void tickProjectile(ServerLevel server) {
        boolean skull = getKind() == KIND_SKULL;
        double speed = skull ? 0.55D : 1.5D;
        Vec3 dir = direction.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, -1.0D, 0.0D) : direction;
        if (skull && targetId != null && server.getEntity(targetId) instanceof LivingEntity target && target.isAlive()) {
            Vec3 want = target.getBoundingBox().getCenter().subtract(this.position()).normalize();
            dir = dir.lerp(want, 0.12D).normalize();
        }
        direction = dir;
        orient(dir);
        this.setPos(getX() + dir.x * speed, getY() + dir.y * speed, getZ() + dir.z * speed);
        if (this.tickCount % 2 == 0) {
            server.sendParticles(skull ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, getX(), getY(), getZ(), 2, 0.15D, 0.15D, 0.15D, 0.01D);
        }

        BlockPos pos = this.blockPosition();
        boolean blocked = !server.getBlockState(pos).getCollisionShape(server, pos).isEmpty();
        boolean near = !server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.9D), Hades::canHit).isEmpty();
        if (blocked || near || this.tickCount >= getLife() - 1) explode(server);
    }

    private void explode(ServerLevel server) {
        float radius = RADIUS[getKind()] * getScale();
        server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 4, radius * 0.3D, radius * 0.3D, radius * 0.3D, 0.0D);
        server.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 30, radius * 0.4D, radius * 0.4D, radius * 0.4D, 0.08D);
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 0.8F);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius), Hades::canHit)) {
            if (e.distanceToSqr(this) > radius * radius || !hit.add(e.getUUID())) continue;
            e.hurt(source(server), damage);
            Vec3 away = e.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            away = away.lengthSqr() < 1.0E-4D ? Vec3.ZERO : away.normalize();
            e.setDeltaMovement(e.getDeltaMovement().add(away.x * 0.8D, 0.5D, away.z * 0.8D));
            e.hurtMarked = true;
        }
        this.discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160.0D * 160.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
