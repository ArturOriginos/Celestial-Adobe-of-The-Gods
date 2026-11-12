package com.example.celestial.entity;

import com.example.celestial.event.SoulDialogue;
import com.example.celestial.network.HadesCutscenePayload;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Аид, Бог Смерти: финальный босс. 20000 здоровья, урон 80-90.
 * <p>
 * Появление: катсцена на 22 секунды (камера у игрока наведена на Аида, он неуязвим и произносит три фразы,
 * после них Душа Селестии говорит свою). Игроки рядом на это время обездвижены и защищены.
 * <p>
 * Три фазы: 100-60% (копья, черепа, колонны), 60-30% (добавляются извержение, шипы, кометы, цепи, телепорт),
 * ниже 30% (всё это плюс ритуальный круг: огромный взрыв по площади). На границах фаз Аид бессмертен:
 * 6 секунд на 60%, 10 секунд на 30% (на второй границе он ещё и восстанавливает 3000 здоровья).
 */
public class Hades extends Monster {
    public static final double MAX_HEALTH = 20000.0D;
    public static final double PHASE2_HEALTH = 12000.0D; // 60%
    public static final double PHASE3_HEALTH = 6000.0D;  // 30%
    public static final int TRANSITION1_TICKS = 120;     // 6 секунд бессмертия
    public static final int TRANSITION2_TICKS = 200;     // 10 секунд бессмертия
    public static final float PHASE3_HEAL = 3000.0F;
    public static final int CUTSCENE_TICKS = 440;

    public static final float MELEE_DAMAGE = 85.0F;
    public static final float SPEAR_DAMAGE = 85.0F, SKULL_DAMAGE = 80.0F, PILLARS_DAMAGE = 90.0F, ERUPTION_DAMAGE = 85.0F,
            SPIKES_DAMAGE = 85.0F, COMET_DAMAGE = 90.0F, CHAINS_DAMAGE = 80.0F, ULTIMATE_DAMAGE = 90.0F;

    public static final int ST_IDLE = 0, ST_CAST = 1, ST_SLAM = 2, ST_CHARGE = 3, ST_ROAR = 4;

    private static final int A_SPEARS = 1, A_SKULLS = 2, A_PILLARS = 3, A_ERUPTION = 4, A_SPIKES = 5,
            A_COMET = 6, A_CHAINS = 7, A_ULT = 8;
    private static final double ULT_RADIUS = 14.0D;

    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(Hades.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(Hades.class, EntityDataSerializers.INT);

    private int cutsceneTicks = 0;
    private boolean cutsceneStarted = false;
    private int transitionTicks = 0;
    private float healPerTick = 0.0F;
    private int ability = 0;
    private int abilityTick = 0;
    private int cooldown = 60;
    private int ultCooldown = 240;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.celestial.hades"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

    public Hades(EntityType<? extends Hades> type, Level level) {
        super(type, level);
        this.xpReward = 20000;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, MELEE_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
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
        builder.define(DATA_PHASE, 1);
    }

    public int getStage() { return this.entityData.get(DATA_STAGE); }

    public int getPhase() { return this.entityData.get(DATA_PHASE); }

    private void setStage(int stage) {
        if (getStage() != stage) this.entityData.set(DATA_STAGE, stage);
    }

    /** Катсцена, переход между фазами или каст: стоит на месте. */
    private boolean isBusy() {
        return cutsceneTicks > 0 || transitionTicks > 0 || getStage() != ST_IDLE;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (!this.level().isClientSide && isBusy()) super.travel(Vec3.ZERO);
        else super.travel(travelVector);
    }

    // =================================================================== урон, фазы, смерть

    private static boolean bypasses(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if ((cutsceneTicks > 0 || transitionTicks > 0) && !bypasses(source)) return true;
        return super.isInvulnerableTo(source);
    }

    /** Граница фазы не перепрыгивается одним ударом: урон обрезается ровно до порога. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && !bypasses(source)) {
            double limit = getPhase() == 1 ? PHASE2_HEALTH : getPhase() == 2 ? PHASE3_HEALTH : 0.0D;
            if (limit > 0.0D) {
                float room = this.getHealth() - (float) limit;
                if (amount > room) amount = Math.max(room, 0.0F);
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && getPhase() < 3 && !bypasses(source)) { // запасной вариант
            this.setHealth((float) (getPhase() == 1 ? PHASE2_HEALTH : PHASE3_HEALTH));
            startTransition();
            return;
        }
        super.die(source);
    }

    private void startTransition() {
        int newPhase = getPhase() + 1;
        this.entityData.set(DATA_PHASE, newPhase);
        this.transitionTicks = newPhase == 2 ? TRANSITION1_TICKS : TRANSITION2_TICKS;
        this.healPerTick = newPhase == 3 ? PHASE3_HEAL / TRANSITION2_TICKS : 0.0F;
        this.ability = 0;
        this.abilityTick = 0;
        setStage(ST_ROAR);
        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 6.0F, 0.4F);
        if (this.level() instanceof ServerLevel server) {
            HadesEffect.spawn(server, HadesEffect.KIND_CIRCLE, getX(), getY(), getZ(), 3.0F, this, 0.0F, transitionTicks, true);
            // отбрасывает всех рядом (без урона), чтобы не стоять над ним
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(9.0D, 3.0D, 9.0D), Hades::canHit)) {
                Vec3 away = e.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
                away = away.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : away.normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(away.x * 1.6D, 0.8D, away.z * 1.6D));
                e.hurtMarked = true;
            }
        }
    }

    private void tickTransition() {
        transitionTicks--;
        if (healPerTick > 0.0F) this.setHealth(Math.min(this.getMaxHealth(), this.getHealth() + healPerTick));
        if (this.level() instanceof ServerLevel server) {
            if (transitionTicks % 4 == 0) {
                server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 2.8D, getZ(), 14, 1.4D, 2.0D, 1.4D, 0.05D);
            }
            if (transitionTicks % 30 == 0) this.playSound(SoundEvents.WITHER_AMBIENT, 4.0F, 0.5F);
        }
        if (transitionTicks == 0) {
            setStage(ST_IDLE);
            cooldown = 30;
            if (getPhase() == 3) ultCooldown = 160;
        }
    }

    // =================================================================== появление и катсцена

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (spawnType != MobSpawnType.STRUCTURE) {
            this.cutsceneTicks = CUTSCENE_TICKS;
            setStage(ST_ROAR);
        }
        return data;
    }

    private java.util.List<ServerPlayer> nearbyPlayers() {
        return this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64.0D)).stream().toList();
    }

    private static MutableComponent hadesLine(String key) {
        return Component.literal("<").append(Component.translatable("celestial.hades").withStyle(ChatFormatting.DARK_RED))
                .append("> ").append(Component.translatable(key).withStyle(s -> s.withColor(0xD9A0FF)));
    }

    private void tickCutscene() {
        int elapsed = CUTSCENE_TICKS - cutsceneTicks;
        java.util.List<ServerPlayer> players = nearbyPlayers();

        if (!cutsceneStarted) {
            cutsceneStarted = true;
            ServerPlayer nearest = null;
            for (ServerPlayer p : players) {
                if (nearest == null || p.distanceToSqr(this) < nearest.distanceToSqr(this)) nearest = p;
            }
            if (nearest != null) { // лицом к игроку
                double dx = nearest.getX() - getX(), dz = nearest.getZ() - getZ();
                float yaw = (float) (Mth.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
                this.setYRot(yaw);
                this.setYBodyRot(yaw);
                this.setYHeadRot(yaw);
            }
            for (ServerPlayer p : players) {
                PacketDistributor.sendToPlayer(p, new HadesCutscenePayload(HadesCutscenePayload.START, getId(), CUTSCENE_TICKS, "", ""));
            }
            if (this.level() instanceof ServerLevel server) {
                HadesEffect.spawn(server, HadesEffect.KIND_CIRCLE, getX(), getY(), getZ(), 3.8F, this, 0.0F, CUTSCENE_TICKS - 20, true);
            }
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 6.0F, 0.4F);
        }

        // игроки рядом: не двигаются и не получают урона, пока идёт сцена
        if (elapsed % 5 == 0) {
            for (ServerPlayer p : players) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 9, true, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12, 4, true, false, false));
            }
        }
        if (elapsed % 6 == 0 && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 2.5D, getZ(), 10, 1.3D, 2.0D, 1.3D, 0.04D);
        }

        switch (elapsed) {
            case 50 -> speak(players, "message.celestial.hades.intro.1");
            case 150 -> speak(players, "message.celestial.hades.intro.2");
            case 250 -> speak(players, "message.celestial.hades.intro.3");
            case 350 -> { // Душа Селестии говорит только у тех, у кого она есть
                for (ServerPlayer p : players) {
                    if (SoulDialogue.sayIfHasSoul(p, 10)) {
                        PacketDistributor.sendToPlayer(p, new HadesCutscenePayload(HadesCutscenePayload.LINE, getId(), 70,
                                "soul.celestial.name", "soul.celestial.line.10"));
                    }
                }
            }
            default -> {}
        }

        cutsceneTicks--;
        if (cutsceneTicks == 0) {
            setStage(ST_IDLE);
            cooldown = 40;
            for (ServerPlayer p : players) {
                PacketDistributor.sendToPlayer(p, new HadesCutscenePayload(HadesCutscenePayload.STOP, getId(), 0, "", ""));
            }
        }
    }

    private void speak(java.util.List<ServerPlayer> players, String key) {
        this.playSound(SoundEvents.WITHER_AMBIENT, 4.0F, 0.5F);
        for (ServerPlayer p : players) {
            p.sendSystemMessage(hadesLine(key));
            PacketDistributor.sendToPlayer(p, new HadesCutscenePayload(HadesCutscenePayload.LINE, getId(), 90, "celestial.hades", key));
        }
    }

    // =================================================================== тик

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
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && isBusy()) {
            Vec3 d = this.getDeltaMovement();
            this.setDeltaMovement(0.0D, d.y, 0.0D);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));

        if (cutsceneTicks > 0) {
            tickCutscene();
            return;
        }
        if (getPhase() == 1 && this.getHealth() <= PHASE2_HEALTH) startTransition();
        else if (getPhase() == 2 && this.getHealth() <= PHASE3_HEALTH) startTransition();
        if (transitionTicks > 0) {
            tickTransition();
            return;
        }

        if (ultCooldown > 0) ultCooldown--;
        if (ability != 0) {
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
        return super.doHurtTarget(target);
    }

    // =================================================================== способности

    private void chooseAbility() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;
        double dist = this.distanceTo(target);
        int phase = getPhase();

        if (phase >= 2 && dist > 20.0D && this.random.nextInt(2) == 0) { // догоняет рывком-телепортом
            blinkNear(target);
            cooldown = 15;
            return;
        }
        java.util.List<Integer> options = new java.util.ArrayList<>();
        options.add(A_SPEARS);
        options.add(A_SKULLS);
        options.add(A_PILLARS);
        if (phase >= 2) {
            if (dist < 10.0D) options.add(A_ERUPTION);
            options.add(A_SPIKES);
            options.add(A_COMET);
            options.add(A_CHAINS);
        }
        if (phase >= 3 && ultCooldown <= 0) {
            options.clear();
            options.add(A_ULT);
        }
        ability = options.get(this.random.nextInt(options.size()));
        abilityTick = 0;
        setStage(ability == A_ERUPTION ? ST_SLAM : ability == A_ULT ? ST_CHARGE : ST_CAST);
        if (ability == A_ULT) this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 6.0F, 0.5F);
    }

    private void endAbility() {
        ability = 0;
        setStage(ST_IDLE);
        int phase = getPhase();
        cooldown = phase == 1 ? 80 + this.random.nextInt(40) : phase == 2 ? 55 + this.random.nextInt(30) : 40 + this.random.nextInt(25);
    }

    private void blinkNear(LivingEntity target) {
        for (int i = 0; i < 8; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double dist = 9.0D + this.random.nextDouble() * 3.0D;
            if (this.randomTeleport(target.getX() + Math.cos(angle) * dist, target.getY(), target.getZ() + Math.sin(angle) * dist, true)) {
                this.playSound(SoundEvents.ENDERMAN_TELEPORT, 3.0F, 0.5F);
                return;
            }
        }
    }

    private void tickAbility() {
        abilityTick++;
        LivingEntity target = this.getTarget();
        if (target != null) this.getLookControl().setLookAt(target, 40.0F, 40.0F);
        if (!(this.level() instanceof ServerLevel server)) return;
        int phase = getPhase();

        switch (ability) {
            case A_SPEARS -> {
                int count = 3 + phase;
                if (abilityTick >= 20 && (abilityTick - 20) % 5 == 0 && (abilityTick - 20) / 5 < count && target != null) {
                    int i = (abilityTick - 20) / 5;
                    double ox = i == 0 ? 0.0D : (this.random.nextDouble() - 0.5D) * 10.0D;
                    double oz = i == 0 ? 0.0D : (this.random.nextDouble() - 0.5D) * 10.0D;
                    HadesEffect.spawn(server, HadesEffect.KIND_SPEAR, target.getX() + ox, target.getY(), target.getZ() + oz, 1.0F, this, SPEAR_DAMAGE, 0, false);
                }
                if (abilityTick >= 20 + count * 5 + 4) endAbility();
            }
            case A_SKULLS -> {
                int count = phase == 1 ? 2 : phase == 2 ? 3 : 4;
                if (abilityTick >= 20 && (abilityTick - 20) % 8 == 0 && (abilityTick - 20) / 8 < count && target != null) {
                    float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
                    HadesEffect skull = HadesEffect.spawn(server, HadesEffect.KIND_SKULL, getX() - Mth.sin(yaw) * 1.5D, getY() + 3.6D,
                            getZ() + Mth.cos(yaw) * 1.5D, 1.0F, this, SKULL_DAMAGE, 0, false);
                    if (skull != null) skull.launchHoming(target);
                    this.playSound(SoundEvents.BLAZE_SHOOT, 3.0F, 0.5F);
                }
                if (abilityTick >= 20 + count * 8 + 4) endAbility();
            }
            case A_PILLARS -> {
                if (abilityTick == 24 && target != null) {
                    HadesEffect.spawn(server, HadesEffect.KIND_PILLARS, target.getX(), target.getY(), target.getZ(), 1.4F, this, PILLARS_DAMAGE, 0, false);
                    if (phase >= 2) {
                        HadesEffect.spawn(server, HadesEffect.KIND_PILLARS, target.getX() + 7.0D, target.getY(), target.getZ(), 1.4F, this, PILLARS_DAMAGE, 0, false);
                        HadesEffect.spawn(server, HadesEffect.KIND_PILLARS, target.getX() - 7.0D, target.getY(), target.getZ(), 1.4F, this, PILLARS_DAMAGE, 0, false);
                    }
                }
                if (abilityTick >= 44) endAbility();
            }
            case A_ERUPTION -> {
                if (abilityTick == 22) {
                    HadesEffect.spawn(server, HadesEffect.KIND_ERUPTION, getX(), getY(), getZ(), 2.4F, this, ERUPTION_DAMAGE, 0, false);
                    this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 0.5F);
                }
                if (abilityTick >= 44) endAbility();
            }
            case A_SPIKES -> {
                if (abilityTick >= 18 && (abilityTick - 18) % 5 == 0 && (abilityTick - 18) / 5 < 6 && target != null) {
                    int i = (abilityTick - 18) / 5;
                    Vec3 dir = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
                    dir = dir.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : dir.normalize();
                    double d = 4.0D + i * 3.4D;
                    HadesEffect.spawn(server, HadesEffect.KIND_SPIKES, getX() + dir.x * d, getY(), getZ() + dir.z * d, 1.1F, this, SPIKES_DAMAGE, 0, false);
                }
                if (abilityTick >= 18 + 6 * 5 + 4) endAbility();
            }
            case A_COMET -> {
                int count = phase >= 3 ? 5 : 3;
                if (abilityTick >= 24 && (abilityTick - 24) % 8 == 0 && (abilityTick - 24) / 8 < count && target != null) {
                    Vec3 start = target.position().add((this.random.nextDouble() - 0.5D) * 14.0D, 28.0D, (this.random.nextDouble() - 0.5D) * 14.0D);
                    Vec3 aim = target.position().add(target.getDeltaMovement().scale(8.0D)).add(0.0D, 1.0D, 0.0D);
                    HadesEffect comet = HadesEffect.spawn(server, HadesEffect.KIND_COMET, start.x, start.y, start.z, 1.0F, this, COMET_DAMAGE, 0, false);
                    if (comet != null) comet.launchStraight(aim.subtract(start).normalize());
                    this.playSound(SoundEvents.FIRECHARGE_USE, 3.0F, 0.5F);
                }
                if (abilityTick >= 24 + count * 8 + 24) endAbility();
            }
            case A_CHAINS -> {
                if (abilityTick == 20 && target != null) {
                    HadesEffect.spawn(server, HadesEffect.KIND_CHAINS, target.getX(), target.getY(), target.getZ(), 1.3F, this, CHAINS_DAMAGE, 0, false);
                    if (phase >= 3) {
                        HadesEffect.spawn(server, HadesEffect.KIND_CHAINS, target.getX() + 6.0D, target.getY(), target.getZ() - 4.0D, 1.3F, this, CHAINS_DAMAGE, 0, false);
                    }
                }
                if (abilityTick >= 36) endAbility();
            }
            case A_ULT -> ultimate(server);
            default -> endAbility();
        }
    }

    /** Ритуальный круг: 4 секунды Аид заряжает удар (круг радиусом около 14 блоков), затем взрыв по всей площади круга. */
    private void ultimate(ServerLevel server) {
        if (abilityTick == 1) {
            HadesEffect.spawn(server, HadesEffect.KIND_CIRCLE, getX(), getY(), getZ(), 3.9F, this, 0.0F, 84, true);
        }
        if (abilityTick % 6 == 0) {
            for (int i = 0; i < 14; i++) {
                double a = i * (Math.PI * 2.0D / 14.0D);
                server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX() + Math.cos(a) * ULT_RADIUS, getY() + 0.2D,
                        getZ() + Math.sin(a) * ULT_RADIUS, 1, 0.1D, 0.4D, 0.1D, 0.01D);
            }
        }
        if (abilityTick == 80) {
            HadesEffect.spawn(server, HadesEffect.KIND_ERUPTION, getX(), getY(), getZ(), 3.2F, this, 0.0F, 0, false);
            this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 6.0F, 0.4F);
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(ULT_RADIUS, 6.0D, ULT_RADIUS), Hades::canHit)) {
                double dx = e.getX() - getX(), dz = e.getZ() - getZ();
                if (dx * dx + dz * dz > ULT_RADIUS * ULT_RADIUS) continue;
                e.hurt(this.damageSources().mobAttack(this), ULTIMATE_DAMAGE);
                Vec3 away = new Vec3(dx, 0.0D, dz);
                away = away.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : away.normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(away.x * 1.8D, 0.9D, away.z * 1.8D));
                e.hurtMarked = true;
            }
        }
        if (abilityTick >= 100) {
            ultCooldown = 700;
            endAbility();
        }
    }

    /** Цели Аида: всё живое, кроме монстров и игроков в творческом режиме/наблюдателей. */
    public static boolean canHit(LivingEntity e) {
        if (!e.isAlive() || e instanceof Hades || e instanceof Monster) return false;
        return !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    // =================================================================== звуки и сохранение

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_AMBIENT; }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.WITHER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HadesPhase", getPhase());
        tag.putInt("TransitionTicks", transitionTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_PHASE, tag.contains("HadesPhase") ? Math.max(1, tag.getInt("HadesPhase")) : 1);
        this.transitionTicks = tag.getInt("TransitionTicks");
        if (transitionTicks > 0) {
            this.healPerTick = getPhase() == 3 ? PHASE3_HEAL / TRANSITION2_TICKS : 0.0F;
            this.entityData.set(DATA_STAGE, ST_ROAR);
        }
        if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
    }
}
