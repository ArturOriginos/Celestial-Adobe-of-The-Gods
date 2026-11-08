package com.example.celestial.item;

import com.example.celestial.network.OpenTomePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/** Всезнающая книга: падает с Всезнающего Ангела. ПКМ открывает книгу с сюжетом. */
public class AllKnowingTomeItem extends Item {
    public AllKnowingTomeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new OpenTomePayload());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
