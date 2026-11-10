package com.example.celestial.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Тёмная сфера Всезнающего Ангела: летит за игроком, пока её не уничтожат (любой удар, стрела, снаряд).
 * При касании игрока наносит 37.5 урона и исчезает. Не сохраняется в мире; исчезает вместе с боссом.
 */
public class DarkOrb extends Entity {
    public static final float DAMAGE = 25.0F; // было 37.5, срезано в 1,5 раза
    private static final double SPEED = 0.90D; // 0.30 x 3

    @Nullable
    private UUID ownerId;
    @Nullable
    private UUID targetId;
    private int idleTicks;

    public DarkOrb(EntityType<? extends DarkOrb> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public void setup(LivingEntity owner, @Nullable LivingEntity target) {
        this.ownerId = owner.getUUID();
        this.targetId = target != null ? target.getUUID() : null;
        this.setDeltaMovement(
                (this.random.nextDouble() - 0.5D) * 0.2D,
                0.12D + this.random.nextDouble() * 0.08D,
                (this.random.nextDouble() - 0.5D) * 0.2D);
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved() || this.isInvulnerableTo(source)) return false;
        Entity attacker = source.getEntity();
        if (attacker != null && attacker.getUUID().equals(ownerId)) return false;
        this.pop();
        return true;
    }

    private void pop() {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.5D, this.getZ(),
                    24, 0.35D, 0.35D, 0.35D, 0.06D);
            server.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.5D, this.getZ(),
                    10, 0.3D, 0.3D, 0.3D, 0.08D);
            server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK,
                    SoundSource.HOSTILE, 1.4F, 0.7F);
        }
        this.discard();
    }

    @Nullable
    private LivingEntity findTarget(ServerLevel server) {
        if (targetId != null) {
            Entity e = server.getEntity(targetId);
            if (e instanceof Player p && p.isAlive() && !p.isSpectator() && !p.isCreative()) return p;
        }
        Player nearest = server.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 64.0D,
                e -> e instanceof Player p && !p.isSpectator() && !p.isCreative() && p.isAlive());
        this.targetId = nearest != null ? nearest.getUUID() : null;
        return nearest;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(0.5D), this.getRandomY(),
                        this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
            }
            return;
        }
        ServerLevel server = (ServerLevel) this.level();

        // исчезаем вместе с боссом
        Entity owner = ownerId != null ? server.getEntity(ownerId) : null;
        if (!(owner instanceof LivingEntity living) || !living.isAlive()) {
            this.pop();
            return;
        }

        LivingEntity target = findTarget(server);
        if (target == null) {
            idleTicks++;
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (idleTicks > 200) this.discard();
        } else {
            idleTicks = 0;
            Vec3 goal = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            Vec3 want = goal.subtract(this.position());
            double d = want.length();
            Vec3 desired = d > 1.0E-4D ? want.scale(SPEED / d) : Vec3.ZERO;
            this.setDeltaMovement(this.getDeltaMovement().scale(0.82D).add(desired.scale(0.18D)));

            if (this.tickCount > 15 && this.getBoundingBox().inflate(0.1D).intersects(target.getBoundingBox())) {
                target.invulnerableTime = 0;
                target.hurt(this.damageSources().indirectMagic(this, living), DAMAGE);
                this.pop();
                return;
            }
        }
        this.setPos(this.position().add(this.getDeltaMovement()));
    }
}
