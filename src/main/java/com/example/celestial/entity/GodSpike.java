package com.example.celestial.entity;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** Золотой шип из-под земли (по механике похож на клыки вызывателя, но бьёт сильнее и подбрасывает). */
public class GodSpike extends Entity {
    private static final float DAMAGE = 16.0F; // 8 сердец

    private int warmup;
    private int lifeTicks = 22;
    private boolean sentEvent;
    private boolean clientStarted;
    @Nullable
    private LivingEntity owner;

    public GodSpike(EntityType<? extends GodSpike> type, Level level) {
        super(type, level);
    }

    public void setup(double x, double y, double z, @Nullable LivingEntity owner, int warmup) {
        this.setPos(x, y, z);
        this.owner = owner;
        this.warmup = warmup;
        this.setYRot(this.random.nextFloat() * 360.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (clientStarted) {
                lifeTicks--;
                if (lifeTicks == 14) {
                    for (int i = 0; i < 12; i++) {
                        this.level().addParticle(ParticleTypes.END_ROD,
                                this.getX() + (this.random.nextDouble() * 2.0 - 1.0) * 0.5,
                                this.getY() + 0.05 + this.random.nextDouble(),
                                this.getZ() + (this.random.nextDouble() * 2.0 - 1.0) * 0.5,
                                (this.random.nextDouble() * 2.0 - 1.0) * 0.3,
                                0.3 + this.random.nextDouble() * 0.3,
                                (this.random.nextDouble() * 2.0 - 1.0) * 0.3);
                    }
                }
            }
        } else if (--warmup < 0) {
            if (warmup == -8) {
                for (LivingEntity target : this.level().getEntitiesOfClass(
                        LivingEntity.class, this.getBoundingBox().inflate(0.6, 0.5, 0.6))) {
                    hit(target);
                }
            }
            if (!sentEvent) {
                this.level().broadcastEntityEvent(this, (byte) 4);
                sentEvent = true;
            }
            if (--lifeTicks < 0) {
                this.discard();
            }
        }
    }

    /** Кто призвал шип (нужен потомкам, чтобы масштабировать урон). */
    @Nullable
    protected LivingEntity getSpikeOwner() {
        return owner;
    }

    /** Урон шипа (у шипа Проклятого переопределяется). */
    protected float damage() {
        return DAMAGE;
    }

    private void hit(LivingEntity target) {
        if (!target.isAlive() || target.isInvulnerable() || target == owner || target instanceof GuardianOfGods || target instanceof CursedBoss
                || target instanceof CelestiaGod) return;
        if (target.hurt(this.damageSources().indirectMagic(this, owner), damage())) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.8, 0.0));
            target.hurtMarked = true;
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
        if (id == 4) {
            clientStarted = true;
            if (!this.isSilent()) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.EVOKER_FANGS_ATTACK,
                        this.getSoundSource(), 1.0F, this.random.nextFloat() * 0.2F + 0.85F, false);
            }
        }
    }

    /** Прогресс анимации 0..1 для рендера (0 - ещё под землёй). */
    public float getAnimationProgress(float partialTicks) {
        if (!clientStarted) return 0.0F;
        int i = lifeTicks - 2;
        return i <= 0 ? 1.0F : 1.0F - ((float) i - partialTicks) / 20.0F;
    }
}
