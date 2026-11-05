package com.example.celestial.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Визуальный эффект способностей Бога Селестии (рисуется на клиенте, урон наносит сам босс):
 * столп света (0), падающая звезда (1) и луч (2). Сначала идёт предупреждение (warmup), затем вспышка (life).
 */
public class CelestiaEffect extends Entity {
    public static final int KIND_PILLAR = 0, KIND_STAR = 1, KIND_BEAM = 2;

    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(CelestiaEffect.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(CelestiaEffect.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LENGTH =
            SynchedEntityData.defineId(CelestiaEffect.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_WARMUP =
            SynchedEntityData.defineId(CelestiaEffect.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(CelestiaEffect.class, EntityDataSerializers.INT);

    public CelestiaEffect(EntityType<? extends CelestiaEffect> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public void setup(int kind, float radius, float length, int warmup, int life) {
        this.entityData.set(DATA_KIND, kind);
        this.entityData.set(DATA_RADIUS, radius);
        this.entityData.set(DATA_LENGTH, length);
        this.entityData.set(DATA_WARMUP, warmup);
        this.entityData.set(DATA_LIFE, life);
    }

    public void setLength(float length) { this.entityData.set(DATA_LENGTH, length); }

    public int getKind() { return this.entityData.get(DATA_KIND); }
    public float getRadius() { return this.entityData.get(DATA_RADIUS); }
    public float getLength() { return this.entityData.get(DATA_LENGTH); }
    public int getWarmup() { return this.entityData.get(DATA_WARMUP); }
    public int getLife() { return this.entityData.get(DATA_LIFE); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, KIND_PILLAR);
        builder.define(DATA_RADIUS, 2.0F);
        builder.define(DATA_LENGTH, 40.0F);
        builder.define(DATA_WARMUP, 0);
        builder.define(DATA_LIFE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > getWarmup() + getLife() + 1) {
            this.discard();
        }
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
