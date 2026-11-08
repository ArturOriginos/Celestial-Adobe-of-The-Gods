package com.example.celestial.block;

import com.example.celestial.registry.ModBlocks;
import com.example.celestial.world.CelestiaDimension;
import com.example.celestial.world.CelestiaPortalPlacer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Горизонтальное светящееся поле 3x3 внутри кольца портала Селестии (как в Энд). Телепортирует в Селестию и обратно. */
public class CelestiaPortalFieldBlock extends Block implements Portal {
    private static final VoxelShape SHAPE = Block.box(0.0, 6.0, 0.0, 16.0, 12.0, 16.0);

    /** Куда вернуться из Селестии (хранится в данных сущности). */
    private static final String RET_DIM = "celestial_return_dim";
    private static final String RET_POS = "celestial_return_pos";

    public CelestiaPortalFieldBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /** Поле гаснет, если рамка вокруг него разрушена. */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis().isHorizontal() && !neighborState.is(this)
                && !neighborState.is(ModBlocks.CELESTIA_PORTAL.get())) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return 0; // мгновенно, как портал в Энд
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        MinecraftServer server = level.getServer();
        CompoundTag data = entity.getPersistentData();

        if (level.dimension().equals(CelestiaDimension.LEVEL)) {
            // --- из Селестии обратно: туда, откуда пришли (или в обычный мир)
            ServerLevel dest = server.overworld();
            if (data.contains(RET_DIM)) {
                ResourceLocation rl = ResourceLocation.tryParse(data.getString(RET_DIM));
                if (rl != null) {
                    ServerLevel saved = server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
                    if (saved != null) dest = saved;
                }
            }
            Vec3 target;
            if (data.contains(RET_POS) && dest.dimension().location().toString().equals(data.getString(RET_DIM))) {
                target = Vec3.atBottomCenterOf(BlockPos.of(data.getLong(RET_POS)));
            } else {
                BlockPos spawn = dest.getSharedSpawnPos();
                BlockPos top = dest.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, spawn);
                target = Vec3.atBottomCenterOf(top);
            }
            target = CelestiaPortalPlacer.findSafe(dest, target);
            return new DimensionTransition(dest, target, Vec3.ZERO, entity.getYRot(), entity.getXRot(),
                    DimensionTransition.DO_NOTHING);
        }

        // --- в Селестию: запоминаем, откуда пришли, и ставим портал обратно на стартовом острове
        ServerLevel dest = server.getLevel(CelestiaDimension.LEVEL);
        if (dest == null) return null;
        data.putString(RET_DIM, level.dimension().location().toString());
        data.putLong(RET_POS, entity.blockPosition().asLong());
        CelestiaPortalPlacer.ensureArrivalPortal(dest);
        return new DimensionTransition(dest, CelestiaPortalPlacer.ARRIVAL, Vec3.ZERO, 180.0F, 0.0F,
                DimensionTransition.DO_NOTHING);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS,
                    0.5F, random.nextFloat() * 0.4F + 0.8F, false);
        }
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0, 0.02, 0.0);
        }
    }
}
