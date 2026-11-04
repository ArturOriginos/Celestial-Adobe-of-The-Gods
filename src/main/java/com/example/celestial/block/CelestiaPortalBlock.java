package com.example.celestial.block;

import com.example.celestial.registry.ModItems;
import com.example.celestial.world.CelestiaPortalShape;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Блок рамки портала Селестии. Ключ Селестии (ПКМ по рамке) зажигает портал. */
public class CelestiaPortalBlock extends Block {
    public CelestiaPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.CELESTIA_KEY.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        Optional<CelestiaPortalShape> shape = CelestiaPortalShape.find(level, pos);
        if (shape.isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.celestial.portal_invalid"), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            shape.get().createPortal(level);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
