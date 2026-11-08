package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.registry.ModAttachments;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Мини-душа разговаривает с игроком в чате (тексты: soul.celestial.line.1 .. 12).
 *  1-5   вступление: сразу при получении души, дальше через 3 секунды между фразами
 *  6     случайно, пока игрок в аду (Незер)
 *  7, 8, 11, 12 в бою ("Сзади!" - если удар пришёл со спины)
 *  9     когда у игрока остаётся одно сердце
 *  10    пока не используется (на будущее: встреча с Аидом)
 * Говорит только пока душа сидит на плече игрока.
 */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class SoulDialogue {
    public static final int LINE_COUNT = 12;
    private static final int INTRO_LINES = 5;
    private static final int INTRO_GAP_TICKS = 60;        // 3 секунды между фразами вступления
    private static final int COMBAT_COOLDOWN_TICKS = 240; // не чаще раза в 12 секунд
    private static final int LOW_HEALTH_COOLDOWN_TICKS = 600;

    private record Pending(int line, int dueTick) {}

    private static final Map<UUID, List<Pending>> SCHEDULE = new HashMap<>();
    private static final Map<UUID, Integer> COMBAT_NEXT = new HashMap<>();
    private static final Map<UUID, Integer> LOW_HEALTH_NEXT = new HashMap<>();
    private static final Map<UUID, Integer> HELL_NEXT = new HashMap<>();

    private SoulDialogue() {}

    // ------------------------------------------------------------------ публичный API

    /** Сказать фразу сейчас. */
    public static void say(ServerPlayer player, int line) {
        MutableComponent name = Component.translatable("soul.celestial.name")
                .withStyle(s -> s.withColor(0xB9A6FF).withBold(true));
        MutableComponent text = Component.translatable("soul.celestial.line." + line)
                .withStyle(s -> s.withColor(0xFFE9A8));
        player.sendSystemMessage(Component.literal("[").withStyle(s -> s.withColor(0x8F8BD8))
                .append(name).append(Component.literal("] ").withStyle(s -> s.withColor(0x8F8BD8))).append(text));
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY() + 1.4D, player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F + player.getRandom().nextFloat() * 0.4F);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.75D, player.getZ(), 4, 0.2D, 0.1D, 0.2D, 0.01D);
    }

    /** Вступление: фразы 1-5, первая сразу, остальные с интервалом в 3 секунды. */
    public static void playIntro(ServerPlayer player) {
        int now = player.server.getTickCount();
        List<Pending> list = SCHEDULE.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        for (int i = 0; i < INTRO_LINES; i++) {
            list.add(new Pending(i + 1, now + i * INTRO_GAP_TICKS));
        }
    }

    private static boolean hasSoul(ServerPlayer player) {
        return player.getData(ModAttachments.MINI_SOUL.get());
    }

    // ------------------------------------------------------------------ события

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (SCHEDULE.isEmpty()) return;
        MinecraftServer server = event.getServer();
        int now = server.getTickCount();
        Iterator<Map.Entry<UUID, List<Pending>>> it = SCHEDULE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<Pending>> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) { // игрок вышел: вступление не теряем, но и не говорим в пустоту
                continue;
            }
            List<Pending> list = entry.getValue();
            list.removeIf(p -> {
                if (p.dueTick() > now) return false;
                say(player, p.line());
                return true;
            });
            if (list.isEmpty()) it.remove();
        }
    }

    /** Фраза 6: случайно, пока игрок в аду. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        UUID id = player.getUUID();
        if (player.level().dimension() != Level.NETHER || !hasSoul(player)) {
            HELL_NEXT.remove(id);
            return;
        }
        int now = player.server.getTickCount();
        Integer next = HELL_NEXT.get(id);
        if (next == null) { // только что попал в ад: первая фраза через 30-90 секунд
            HELL_NEXT.put(id, now + 600 + player.getRandom().nextInt(1200));
        } else if (now >= next) {
            say(player, 6);
            HELL_NEXT.put(id, now + 3600 + player.getRandom().nextInt(5400)); // дальше раз в 3-7,5 минут
        }
    }

    /** Фразы 9 (одно сердце), 7, 8, 11, 12 (бой). */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();

        // игрок получил урон
        if (victim instanceof ServerPlayer player && hasSoul(player)) {
            int now = player.server.getTickCount();
            if (player.isAlive() && player.getHealth() <= 2.0F
                    && now >= LOW_HEALTH_NEXT.getOrDefault(player.getUUID(), 0)) {
                LOW_HEALTH_NEXT.put(player.getUUID(), now + LOW_HEALTH_COOLDOWN_TICKS);
                say(player, 9);
                return;
            }
            if (attacker instanceof LivingEntity && !(attacker instanceof Player)) {
                combatLine(player, attacker, true);
            }
            return;
        }
        // игрок бьёт моба
        if (attacker instanceof ServerPlayer player && victim instanceof Mob && hasSoul(player)) {
            combatLine(player, victim, false);
        }
    }

    private static void combatLine(ServerPlayer player, Entity enemy, boolean tookDamage) {
        int now = player.server.getTickCount();
        if (now < COMBAT_NEXT.getOrDefault(player.getUUID(), 0)) return;
        COMBAT_NEXT.put(player.getUUID(), now + COMBAT_COOLDOWN_TICKS);

        int line;
        Vec3 toEnemy = enemy.position().subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
        boolean behind = tookDamage && toEnemy.lengthSqr() > 1.0E-4D
                && toEnemy.normalize().dot(player.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize()) < -0.3D;
        if (behind) {
            line = 12; // "Сзади!"
        } else {
            int[] pool = {7, 8, 11}; // "Осторожно!", "Беги!", "Берегись!"
            line = pool[player.getRandom().nextInt(pool.length)];
        }
        say(player, line);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        COMBAT_NEXT.remove(id);
        LOW_HEALTH_NEXT.remove(id);
        HELL_NEXT.remove(id);
    }
}
