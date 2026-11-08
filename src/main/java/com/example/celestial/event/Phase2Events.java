package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.entity.FlyingSoul;
import com.example.celestial.registry.ModEntities;
import com.example.celestial.world.NiflheimSpread;
import com.example.celestial.world.Phase2Pending;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Переход во 2 фазу: после убийства Бога Селестии убийце задаётся вопрос в чате (красным, с кнопками Да / Нет).
 *  Да  - вылетает душа, начинаются её фразы и начинает распространяться Проклятый Нифельхельм (об этом пишется в чат).
 *  Нет - ничего не происходит, вопрос задаётся в чате снова.
 * Кнопки запускают команду /celestia_phase2 yes|no (права не нужны, работает только если вопрос задан).
 */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class Phase2Events {
    private Phase2Events() {}

    /** Вызывается, когда бог окончательно погиб: запоминаем вопрос и задаём его игроку. */
    public static void askAfterGodDeath(ServerPlayer player, ServerLevel level, double x, double y, double z) {
        Phase2Pending.get(player.server.overworld()).put(player.getUUID(),
                new Phase2Pending.Entry(level.dimension().location().toString(), x, y, z));
        sendWarning(player);
    }

    private static void sendWarning(ServerPlayer player) {
        MutableComponent text = Component.translatable("message.celestial.phase2.warning")
                .withStyle(ChatFormatting.RED);
        MutableComponent yes = Component.translatable("message.celestial.phase2.yes")
                .withStyle(s -> s.withColor(ChatFormatting.RED).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/celestia_phase2 yes")));
        MutableComponent no = Component.translatable("message.celestial.phase2.no")
                .withStyle(s -> s.withColor(ChatFormatting.DARK_RED).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/celestia_phase2 no")));
        player.sendSystemMessage(Component.literal(" "));
        player.sendSystemMessage(text);
        player.sendSystemMessage(yes.append(Component.literal("   ").withStyle(ChatFormatting.RED)).append(no));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("celestia_phase2")
                .then(Commands.literal("yes").executes(ctx -> answer(ctx.getSource(), true)))
                .then(Commands.literal("no").executes(ctx -> answer(ctx.getSource(), false))));
    }

    private static int answer(CommandSourceStack source, boolean yes) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MinecraftServer server = player.server;
        Phase2Pending data = Phase2Pending.get(server.overworld());
        Phase2Pending.Entry entry = data.get(player.getUUID());
        if (entry == null) {
            source.sendFailure(Component.translatable("message.celestial.phase2.none"));
            return 0;
        }
        if (!yes) {
            sendWarning(player); // отказ: ничего не начинается, вопрос повторяется
            return 0;
        }
        data.remove(player.getUUID());

        // душа вылетает из места гибели бога; если игрок в другом измерении, она появляется рядом с ним
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(entry.dimension())));
        double x = entry.x(), y = entry.y(), z = entry.z();
        if (level == null || level != player.serverLevel()) {
            level = player.serverLevel();
            x = player.getX();
            y = player.getY() + 2.5D;
            z = player.getZ();
        }
        FlyingSoul soul = ModEntities.FLYING_SOUL.get().create(level);
        if (soul != null) {
            soul.setPos(x, y, z);
            soul.setTarget(player.getUUID());
            level.addFreshEntity(soul);
        }

        NiflheimSpread.startAtWorldSpawn(server);
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("message.celestial.phase2.started").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                false);
        return 1;
    }

    /** Если игрок вышел, не ответив, вопрос повторяется при следующем входе. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (Phase2Pending.get(player.server.overworld()).has(player.getUUID())) sendWarning(player);
    }
}
