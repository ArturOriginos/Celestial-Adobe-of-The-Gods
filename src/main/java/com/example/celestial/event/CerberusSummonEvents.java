package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.entity.Cerberus;
import com.example.celestial.registry.ModEntities;
import com.example.celestial.registry.ModItems;
import com.example.celestial.world.UnknownDimension;
import com.mojang.brigadier.CommandDispatcher;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

/**
 * Призыв Цербера Запретным плодом. ПКМ по плоду задаёт в чате вопрос с кнопками Да / Нет (можно и написать «да» / «нет»):
 *  Да  - плод расходуется, Цербер появляется перед игроком;
 *  Нет - плод остаётся в инвентаре, в чат пишется «Битва состоится, но когда?».
 * Кнопки запускают /cerberus_summon yes|no (права не нужны, работает только если вопрос задан).
 */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class CerberusSummonEvents {
    private static final int ANSWER_TICKS = 1200; // на ответ даётся минута

    private record Pending(ResourceKey<Level> dimension, long expires) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private CerberusSummonEvents() {}

    /** Вызывается предметом: запоминает вопрос и задаёт его в чате. */
    public static void request(ServerPlayer player) {
        if (player.level().dimension().equals(UnknownDimension.LEVEL)) {
            player.displayClientMessage(Component.translatable("message.celestial.cerberus.not_here"), true);
            return;
        }
        PENDING.put(player.getUUID(), new Pending(player.level().dimension(), player.server.getTickCount() + ANSWER_TICKS));
        sendWarning(player);
    }

    private static void sendWarning(ServerPlayer player) {
        MutableComponent text = Component.translatable("message.celestial.cerberus.warning").withStyle(ChatFormatting.RED);
        MutableComponent yes = Component.translatable("message.celestial.cerberus.yes")
                .withStyle(s -> s.withColor(ChatFormatting.RED).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cerberus_summon yes")));
        MutableComponent no = Component.translatable("message.celestial.cerberus.no_button")
                .withStyle(s -> s.withColor(ChatFormatting.DARK_RED).withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cerberus_summon no")));
        player.sendSystemMessage(Component.literal(" "));
        player.sendSystemMessage(text);
        player.sendSystemMessage(yes.append(Component.literal("   ").withStyle(ChatFormatting.RED)).append(no));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("cerberus_summon")
                .then(Commands.literal("yes").executes(ctx -> answer(ctx.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("no").executes(ctx -> answer(ctx.getSource().getPlayerOrException(), false))));
    }

    /** Ответ можно просто написать в чат: «да» / «нет» (или yes / no), пока вопрос задан. */
    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (!PENDING.containsKey(player.getUUID())) return;
        String text = event.getRawText().trim().toLowerCase(java.util.Locale.ROOT);
        if (text.equals("да") || text.equals("yes")) answer(player, true);
        else if (text.equals("нет") || text.equals("no")) answer(player, false);
    }

    private static int answer(ServerPlayer player, boolean yes) {
        Pending pending = PENDING.remove(player.getUUID());
        if (pending == null || pending.expires() < player.server.getTickCount()
                || !pending.dimension().equals(player.level().dimension())) {
            player.sendSystemMessage(Component.translatable("message.celestial.cerberus.none").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        if (!yes) { // плод не тратился, он остаётся в инвентаре
            player.sendSystemMessage(Component.translatable("message.celestial.cerberus.no").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        if (!player.isCreative() && !consumeFruit(player)) {
            player.sendSystemMessage(Component.translatable("message.celestial.cerberus.lost").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        spawn(player);
        return 1;
    }

    private static boolean consumeFruit(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.FORBIDDEN_FRUIT.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** Цербер появляется в нескольких блоках перед игроком (ближайшее свободное место) и сразу видит его как цель. */
    private static void spawn(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Cerberus cerberus = ModEntities.CERBERUS.get().create(level);
        if (cerberus == null) return;

        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0D, look.z);
        forward = forward.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : forward.normalize();
        boolean placed = false;
        search:
        for (double dist : new double[] {9.0D, 7.0D, 5.0D, 3.0D}) {
            for (double dy : new double[] {0.0D, 1.0D, 2.0D, -1.0D}) {
                Vec3 pos = player.position().add(forward.scale(dist)).add(0.0D, dy, 0.0D);
                cerberus.setPos(pos.x, pos.y, pos.z);
                if (level.noCollision(cerberus)) {
                    placed = true;
                    break search;
                }
            }
        }
        if (!placed) cerberus.setPos(player.getX() + forward.x * 5.0D, player.getY() + 1.0D, player.getZ() + forward.z * 5.0D);

        cerberus.setYRot(player.getYRot() + 180.0F);
        cerberus.finalizeSpawn(level, level.getCurrentDifficultyAt(cerberus.blockPosition()), MobSpawnType.EVENT, null);
        cerberus.setTarget(player);
        level.addFreshEntity(cerberus);
    }
}
