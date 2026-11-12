package com.example.celestial.item;

import com.example.celestial.event.CerberusSummonEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Запретный плод: ПКМ спрашивает в чате, готов ли игрок к бою с Цербером; ответ «Да» призывает его. */
public class ForbiddenFruitItem extends Item {
    public ForbiddenFruitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp && !player.getCooldowns().isOnCooldown(this)) {
            player.getCooldowns().addCooldown(this, 20);
            CerberusSummonEvents.request(sp);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
