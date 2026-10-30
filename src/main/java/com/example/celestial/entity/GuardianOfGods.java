package com.example.celestial.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.phys.Vec3;

/** Хранитель Богов: огромный голем, 140 сердец, вырывается из-под земли, призывает шипы. Дроп: Душа Хранителя Богов. */
public class GuardianOfGods extends Monster {
    public static final double MAX_HEALTH = 280.0D; // 140 сердец
    private static final int RISE_TICKS = 70;

    private int riseTicks = 0;
    private double riseBaseY;
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.celestial.guardian_of_gods"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    public GuardianOfGods(EntityType<? extends GuardianOfGods> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SpikeAttackGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** При появлении Хранитель стартует под землёй и выбирается наружу. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.riseBaseY = this.getY();
        this.riseTicks = RISE_TICKS;
        this.setPos(this.getX(), this.riseBaseY - 4.0D, this.getZ());
        return data;
    }

    @Override
    public void aiStep() {
        if (riseTicks > 0) {
            rise();
            return;
        }
        super.aiStep();
    }

    private void rise() {
        riseTicks--;
        double progress = 1.0D - (double) riseTicks / RISE_TICKS;
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(this.getX(), riseBaseY - 4.0D + 4.0D * progress, this.getZ());
        if (this.level() instanceof ServerLevel server) {
            BlockState ground = server.getBlockState(BlockPos.containing(this.getX(), riseBaseY - 1.0D, this.getZ()));
            if (ground.isAir()) ground = Blocks.DIRT.defaultBlockState();
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    this.getX(), riseBaseY + 0.1D, this.getZ(), 12, 1.1D, 0.1D, 1.1D, 0.15D);
            if (riseTicks % 8 == 0) this.playSound(SoundEvents.GRAVEL_BREAK, 2.5F, 0.6F);
            if (riseTicks == 0) {
                this.noPhysics = false;
                this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 3.0F, 0.5F);
                server.sendParticles(ParticleTypes.EXPLOSION, this.getX(), riseBaseY + 0.5D, this.getZ(), 6, 1.5D, 0.2D, 1.5D, 0.0D);
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return riseTicks > 0 || super.isInvulnerableTo(source);
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

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }
}
