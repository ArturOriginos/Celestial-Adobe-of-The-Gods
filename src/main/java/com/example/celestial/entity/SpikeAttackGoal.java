package com.example.celestial.entity;

import com.example.celestial.registry.ModEntities;
import java.util.EnumSet;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Хранитель замирает, концентрируется и призывает шипы: либо кольцами вокруг себя, либо линией к цели. */
public class SpikeAttackGoal extends Goal {
    private final Monster mob;
    private final Supplier<EntityType<? extends GodSpike>> spikeType;
    private final ParticleOptions chargeParticle;
    private int windup;
    private int nextCastTick = 60;

    public SpikeAttackGoal(GuardianOfGods mob) {
        this(mob, ModEntities.GOD_SPIKE::get, ParticleTypes.FLAME);
    }

    public SpikeAttackGoal(Monster mob, Supplier<EntityType<? extends GodSpike>> spikeType, ParticleOptions chargeParticle) {
        this.mob = mob;
        this.spikeType = spikeType;
        this.chargeParticle = chargeParticle;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || mob.tickCount < nextCastTick) return false;
        double dist = mob.distanceToSqr(target);
        return dist > 9.0D && dist < 24.0D * 24.0D && mob.hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        return windup > 0 && target != null && target.isAlive();
    }

    @Override
    public void start() {
        windup = 30;
        mob.getNavigation().stop();
        mob.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 3.0F, 0.7F);
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        windup--;

        if (mob.level() instanceof ServerLevel level) {
            // подготовка: золотые искры вокруг ног
            level.sendParticles(chargeParticle, mob.getX(), mob.getY() + 0.1, mob.getZ(),
                    6, 1.2, 0.05, 1.2, 0.02);
            if (windup == 0) cast(level, target);
        }
    }

    @Override
    public void stop() {
        windup = 0;
        nextCastTick = mob.tickCount + 70 + mob.getRandom().nextInt(50);
    }

    private void cast(ServerLevel level, LivingEntity target) {
        mob.playSound(SoundEvents.EVOKER_CAST_SPELL, 3.0F, 0.6F);
        if (mob.getRandom().nextBoolean()) {
            // кольца вокруг Хранителя
            boolean enraged = mob.getHealth() < mob.getMaxHealth() * 0.5F;
            int[] radii = enraged ? new int[]{2, 4, 6, 8} : new int[]{2, 4, 6};
            int[] counts = enraged ? new int[]{8, 14, 20, 26} : new int[]{8, 14, 20};
            for (int r = 0; r < radii.length; r++) {
                for (int i = 0; i < counts[r]; i++) {
                    double angle = (Math.PI * 2.0 / counts[r]) * i;
                    spawn(level, mob.getX() + Math.cos(angle) * radii[r], mob.getZ() + Math.sin(angle) * radii[r], r * 4);
                }
            }
        } else {
            // линия шипов в сторону цели + удар в самой цели
            Vec3 dir = new Vec3(target.getX() - mob.getX(), 0, target.getZ() - mob.getZ()).normalize();
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            double length = Math.min(18.0, Math.sqrt(mob.distanceToSqr(target)) + 2.0);
            for (int i = 2; i <= length; i++) {
                double cx = mob.getX() + dir.x * i;
                double cz = mob.getZ() + dir.z * i;
                spawn(level, cx, cz, i);
                if (i % 3 == 0) {
                    spawn(level, cx + side.x, cz + side.z, i + 1);
                    spawn(level, cx - side.x, cz - side.z, i + 1);
                }
            }
            spawn(level, target.getX(), target.getZ(), 6);
        }
    }

    private void spawn(ServerLevel level, double x, double z, int delay) {
        GodSpike spike = spikeType.get().create(level);
        if (spike == null) return;
        spike.setup(x, groundY(level, x, mob.getY(), z), z, mob, delay);
        level.addFreshEntity(spike);
    }

    private static double groundY(Level level, double x, double startY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(startY) + 2, Mth.floor(z));
        for (int i = 0; i < 8; i++) {
            BlockPos below = pos.below();
            boolean solidBelow = !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
            boolean freeHere = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
            if (solidBelow && freeHere) return pos.getY();
            pos.move(Direction.DOWN);
        }
        return startY;
    }
}
