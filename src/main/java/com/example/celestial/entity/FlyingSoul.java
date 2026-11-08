package com.example.celestial.entity;

import com.example.celestial.event.SoulDialogue;
import com.example.celestial.network.MiniSoulSync;
import com.example.celestial.registry.ModAttachments;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Маленькая душа, вылетающая из погибшего Бога Селестии. Летит к игроку по дуге и садится ему на правое плечо
 * (включает мини-душу), после чего рассказывает вступление.
 */
public class FlyingSoul extends Entity {
    private UUID targetId;

    public FlyingSoul(EntityType<? extends FlyingSoul> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setTarget(UUID id) {
        this.targetId = id;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    /** Правое плечо игрока (низ модели души). */
    private static Vec3 shoulder(ServerPlayer player) {
        double yaw = Math.toRadians(player.yBodyRot);
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
        double height = player.isCrouching() ? 1.15D : 1.4D;
        return player.position().add(right.scale(0.34D)).add(0.0D, height, 0.0D);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel server)) return;

        ServerPlayer target = this.targetId == null ? null : server.getServer().getPlayerList().getPlayer(this.targetId);
        if (target == null) {
            this.discard();
            return;
        }
        if (target.level() != server || this.tickCount > 400) { // игрок ушёл в другое измерение или полёт затянулся
            arrive(target);
            return;
        }

        Vec3 pos = this.position();
        Vec3 delta = shoulder(target).subtract(pos);
        double dist = delta.length();
        if (dist < 0.4D) {
            arrive(target);
            return;
        }
        double speed = Math.min(Math.min(0.55D, 0.06D + this.tickCount * 0.012D), dist);
        Vec3 dir = delta.scale(1.0D / dist);
        Vec3 perp = new Vec3(-dir.z, 0.0D, dir.x);
        double swirl = Math.sin(this.tickCount * 0.45D) * 0.07D * Math.min(1.0D, dist / 4.0D);
        double lift = Math.max(0.0D, 1.0D - this.tickCount / 20.0D) * 0.06D;
        Vec3 motion = dir.scale(speed).add(perp.scale(swirl)).add(0.0D, lift, 0.0D);
        this.setDeltaMovement(motion);
        this.setPos(pos.add(motion));
        this.setYRot((float) (Mth.atan2(-motion.x, motion.z) * (180.0D / Math.PI)));

        server.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.1D, this.getZ(), 1, 0.03D, 0.03D, 0.03D, 0.0D);
    }

    private void arrive(ServerPlayer target) {
        ServerLevel level = target.serverLevel();
        MiniSoulSync.set(target, true);
        level.playSound(null, target.getX(), target.getY() + 1.4D, target.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.5F);
        level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.6D, target.getZ(), 14, 0.25D, 0.25D, 0.25D, 0.03D);
        if (!target.getData(ModAttachments.MINI_SOUL_INTRO_DONE.get())) {
            target.setData(ModAttachments.MINI_SOUL_INTRO_DONE.get(), true);
            SoulDialogue.playIntro(target);
        }
        this.discard();
    }
}
