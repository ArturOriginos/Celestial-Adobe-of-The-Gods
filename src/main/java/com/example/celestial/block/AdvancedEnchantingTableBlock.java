package com.example.celestial.block;

import com.example.celestial.menu.AdvancedEnchantingMenu;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AdvancedEnchantingTableBlock extends Block {
    public static final MapCodec<AdvancedEnchantingTableBlock> CODEC = simpleCodec(AdvancedEnchantingTableBlock::new);
    private static final VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(0.0, 10.0, 0.0, 16.0, 14.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 4.0, 10.0, 4.0), Block.box(12.0, 0.0, 0.0, 16.0, 10.0, 4.0),
            Block.box(0.0, 0.0, 12.0, 4.0, 10.0, 16.0), Block.box(12.0, 0.0, 12.0, 16.0, 10.0, 16.0));
    private static final Component TITLE = Component.translatable("container.celestial.advanced_enchanting");

    public AdvancedEnchantingTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new AdvancedEnchantingMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                    TITLE));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 2.0, -0.5, (random.nextDouble() - 0.5) * 2.0);
        }
    }
}
