package com.example.celestial.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/** Часы Фенрира: ПКМ останавливает течение дня и ночи, повторное нажатие возобновляет его. */
public class FenrirClockItem extends Item {
    public FenrirClockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            MinecraftServer server = level.getServer();
            GameRules.BooleanValue rule = server.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
            boolean nowRunning = !rule.get();
            rule.set(nowRunning, server);
            player.displayClientMessage(Component.translatable(nowRunning
                    ? "message.celestial.fenrir_clock.running" : "message.celestial.fenrir_clock.stopped"), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, nowRunning ? 1.4F : 0.6F);
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
