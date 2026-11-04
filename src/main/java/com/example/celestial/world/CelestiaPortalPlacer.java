package com.example.celestial.world;

import com.example.celestial.block.CelestiaPortalFieldBlock;
import com.example.celestial.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Портал обратно на стартовом острове Селестии и подбор безопасной точки приземления. */
public final class CelestiaPortalPlacer {
    private static final int Y = CelestiaDimension.SPAWN_TOP_Y;

    /** Куда попадает игрок: перед порталом (портал в плоскости X на z=0, игрок стоит на z=3 и смотрит на него). */
    public static final Vec3 ARRIVAL = new Vec3(1.0, Y + 1, 3.0);

    private CelestiaPortalPlacer() {}

    /** Строит на стартовом острове рамку 4x5 (внутри 2x3) с полем, если её там ещё нет. */
    public static void ensureArrivalPortal(ServerLevel level) {
        BlockState frame = ModBlocks.CELESTIA_PORTAL.get().defaultBlockState();
        BlockState field = ModBlocks.CELESTIA_PORTAL_FIELD.get().defaultBlockState()
                .setValue(CelestiaPortalFieldBlock.AXIS, Direction.Axis.X);
        if (level.getBlockState(new BlockPos(0, Y + 1, 0)).is(field.getBlock())) return;

        // расчистить площадку и поставить платформу из земли под ногами
        BlockState earth = ModBlocks.CELESTIAL_EARTH.get().defaultBlockState();
        for (int x = -3; x <= 4; x++) {
            for (int z = -2; z <= 6; z++) {
                level.setBlock(new BlockPos(x, Y, z), earth, 3);
                for (int y = Y + 1; y <= Y + 6; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        // рамка: x от -1 до 2, y от Y до Y+4, внутри x 0..1, y Y+1..Y+3
        for (int x = -1; x <= 2; x++) {
            for (int y = Y; y <= Y + 4; y++) {
                boolean border = x == -1 || x == 2 || y == Y || y == Y + 4;
                if (!border) continue;
                if ((x == -1 || x == 2) && (y == Y || y == Y + 4)) continue; // углы не нужны
                level.setBlock(new BlockPos(x, y, 0), frame, 2);
            }
        }
        for (int x = 0; x <= 1; x++) {
            for (int y = Y + 1; y <= Y + 3; y++) {
                level.setBlock(new BlockPos(x, y, 0), field, 18);
            }
        }
    }

    private static boolean free(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.getCollisionShape(level, pos).isEmpty() && s.getFluidState().isEmpty();
    }

    /** Если точка внутри блока - поднимаем, если висит в воздухе - опускаем до земли (до 32 блоков). */
    public static Vec3 findSafe(ServerLevel level, Vec3 target) {
        BlockPos p = BlockPos.containing(target);
        level.getChunk(p); // прогрузить
        int y = p.getY();
        int max = level.getMaxBuildHeight() - 2;
        while (y < max && (!free(level, p.atY(y)) || !free(level, p.atY(y + 1)))) y++;
        int fall = 0;
        while (fall < 32 && y > level.getMinBuildHeight() + 1 && free(level, p.atY(y - 1))) {
            y--;
            fall++;
        }
        return new Vec3(target.x, y, target.z);
    }
}
