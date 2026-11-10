package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.entity.AllKnowingAngel;
import com.example.celestial.entity.Fenrir;
import com.example.celestial.network.SkyEyesPayload;
import com.example.celestial.world.WorldState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Последствия убийства боссов для всего мира:
 *  Всезнающий Ангел - в небе открываются глаза, а Аид пишет в чат непонятные символы;
 *  Фенрир - в мире наступает вечная ночь.
 * Команда /celestialworld (для операторов) включает и выключает оба состояния.
 */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class WorldStateEvents {
    /** Бессмысленные символы для сообщений Аида: руны, значки и блоки. */
    private static final String GLYPHS = "ᚠᚢᚦᚨᚱᚲᚷᚹᚺᚾᛁᛃᛇᛈᛉᛊᛏᛒᛖᛗᛚᛜᛞᛟ∴∵∷⊗⊘⋈⌬⍜▓▒░";
    private static final RandomSource RANDOM = RandomSource.create();

    private static final int NIGHT_TIME = 18000;
    private static final int HADES_MESSAGES = 8;

    private static int hadesLeft = 0;
    private static int hadesDelay = 0;

    private WorldStateEvents() {}

    // ------------------------------------------------------------ убийство боссов
    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();

        if (entity instanceof AllKnowingAngel) {
            WorldState state = WorldState.get(server);
            boolean first = !state.skyEyes();
            state.setSkyEyes(true);
            if (first) PacketDistributor.sendToAllPlayers(new SkyEyesPayload(true, true));
            hadesLeft = HADES_MESSAGES;
            hadesDelay = 60;
        } else if (entity instanceof Fenrir) {
            WorldState.get(server).setEternalNight(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (WorldState.get(player.server).skyEyes()) {
            PacketDistributor.sendToPlayer(player, new SkyEyesPayload(true, false));
        }
    }

    // ------------------------------------------------------------ тики: сообщения Аида и вечная ночь
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        if (hadesLeft > 0 && --hadesDelay <= 0) {
            hadesLeft--;
            hadesDelay = 40 + RANDOM.nextInt(80);
            server.getPlayerList().broadcastSystemMessage(hadesMessage(), false);
        }

        if (server.getTickCount() % 10 == 0 && WorldState.get(server).eternalNight()) {
            ServerLevel overworld = server.overworld();
            long time = overworld.getDayTime();
            long night = time - Math.floorMod(time, 24000L) + NIGHT_TIME; // всегда полночь текущих суток
            if (time != night) overworld.setDayTime(night);
        }
    }

    /** "<Аид> ᚠᛉ∴ ..." - набор символов, который ничего не значит. */
    private static Component hadesMessage() {
        StringBuilder sb = new StringBuilder();
        int words = 2 + RANDOM.nextInt(4);
        for (int w = 0; w < words; w++) {
            if (w > 0) sb.append(' ');
            int len = 2 + RANDOM.nextInt(6);
            for (int i = 0; i < len; i++) sb.append(GLYPHS.charAt(RANDOM.nextInt(GLYPHS.length())));
        }
        MutableComponent name = Component.translatable("celestial.hades").withStyle(ChatFormatting.DARK_RED);
        return Component.literal("<").append(name).append("> ")
                .append(Component.literal(sb.toString()).withStyle(ChatFormatting.RED));
    }

    // ------------------------------------------------------------ команда
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("celestialworld")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("eternal_night")
                        .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                            boolean on = BoolArgumentType.getBool(ctx, "enabled");
                            WorldState.get(ctx.getSource().getServer()).setEternalNight(on);
                            ctx.getSource().sendSuccess(() -> Component.translatable(on
                                    ? "command.celestial.world.eternal_night.on"
                                    : "command.celestial.world.eternal_night.off"), true);
                            return 1;
                        })))
                .then(Commands.literal("sky_eyes")
                        .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                            boolean on = BoolArgumentType.getBool(ctx, "enabled");
                            WorldState.get(ctx.getSource().getServer()).setSkyEyes(on);
                            PacketDistributor.sendToAllPlayers(new SkyEyesPayload(on, on));
                            ctx.getSource().sendSuccess(() -> Component.translatable(on
                                    ? "command.celestial.world.sky_eyes.on"
                                    : "command.celestial.world.sky_eyes.off"), true);
                            return 1;
                        }))));
    }
}
