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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Блок рамки портала Селестии (как рамка портала в Энд): 12 блоков кольцом 5x5 без углов.
 * ПКМ ключом Селестии вставляет ключ в рамку; когда во всех 12 блоках есть ключ, внутри загорается портал 3x3.
 */
public class CelestiaPortalBlock extends Block {
    public static final BooleanProperty KEY = BooleanProperty.create("key");
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 13.0, 16.0);

    public CelestiaPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KEY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KEY);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.CELESTIA_KEY.get()) || state.getValue(KEY)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(KEY, true), 2);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.updateNeighbourForOutputSignal(pos, this);

            Optional<CelestiaPortalShape> shape = CelestiaPortalShape.find(level, pos);
            if (shape.isPresent()) {
                shape.get().createPortal(level);
                level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Если сломать рамку с ключом, ключ выпадает обратно. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && state.getValue(KEY) && !level.isClientSide) {
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    new ItemStack(ModItems.CELESTIA_KEY.get()));
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
