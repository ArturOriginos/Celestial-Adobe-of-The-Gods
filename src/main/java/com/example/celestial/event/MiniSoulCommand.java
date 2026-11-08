package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.network.MiniSoulSync;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /minisoul on|off            - мини-душа на своём плече (для всех игроков)
 * /minisoul on|off <игроки>   - для других игроков (нужны права оператора, уровень 2)
 * /minisoul say <1-12>        - проверка: душа говорит выбранную фразу (оператор)
 * /minisoul intro             - проверка: вступление (фразы 1-5) (оператор)
 */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class MiniSoulCommand {
    private MiniSoulCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("minisoul")
                .then(Commands.literal("on")
                        .executes(ctx -> set(ctx.getSource(), java.util.List.of(ctx.getSource().getPlayerOrException()), true))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), true))))
                .then(Commands.literal("say")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("line", IntegerArgumentType.integer(1, SoulDialogue.LINE_COUNT))
                                .executes(ctx -> {
                                    SoulDialogue.say(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "line"));
                                    return 1;
                                })))
                .then(Commands.literal("intro")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            SoulDialogue.playIntro(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                .then(Commands.literal("off")
                        .executes(ctx -> set(ctx.getSource(), java.util.List.of(ctx.getSource().getPlayerOrException()), false))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), false)))));
    }

    private static int set(CommandSourceStack source, Collection<ServerPlayer> players, boolean value) {
        for (ServerPlayer player : players) {
            MiniSoulSync.set(player, value);
        }
        Component msg = Component.translatable(value ? "commands.celestial.minisoul.on" : "commands.celestial.minisoul.off",
                players.size());
        source.sendSuccess(() -> msg, true);
        return players.size();
    }
}
