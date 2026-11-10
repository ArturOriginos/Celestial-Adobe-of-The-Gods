package com.example.celestial.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Визуальный эффект способностей Фенрира (рисуется на клиенте, урон наносит сам босс): дух воя (0),
 * след прыжка (1) и всплеск шипов (2). Эффекты воя и прыжка следуют за боссом.
 */
public class FenrirEffect extends Entity {
    public static final int KIND_HOWL = 0, KIND_LEAP = 1, KIND_SPIKES = 2;

    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(FenrirEffect.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(FenrirEffect.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(FenrirEffect.class, EntityDataSerializers.FLOAT);

    @Nullable
    private Entity follow;

    public FenrirEffect(EntityType<? extends FenrirEffect> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public void setup(int kind, int life, float scale, @Nullable Entity follow) {
        this.entityData.set(DATA_KIND, kind);
        this.entityData.set(DATA_LIFE, life);
        this.entityData.set(DATA_SCALE, scale);
        this.follow = follow;
    }

    public int getKind() { return this.entityData.get(DATA_KIND); }
    public int getLife() { return this.entityData.get(DATA_LIFE); }
    public float getScale() { return this.entityData.get(DATA_SCALE); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, KIND_HOWL);
        builder.define(DATA_LIFE, 40);
        builder.define(DATA_SCALE, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (follow != null && follow.isAlive()) {
            this.setPos(follow.getX(), follow.getY(), follow.getZ());
            this.setYRot(follow.getYRot());
        }
        if (this.tickCount > getLife() + 2) this.discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0D * 128.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
