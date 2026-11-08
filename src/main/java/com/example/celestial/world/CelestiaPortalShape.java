package com.example.celestial.world;

import com.example.celestial.block.CelestiaPortalBlock;
import com.example.celestial.registry.ModBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Портал Селестии как портал в Энд: кольцо из 12 блоков портала (квадрат 5x5 без углов) в одной плоскости,
 * в каждом из них вставлен ключ. Внутри кольца поле 3x3. center - центральный блок поля.
 */
public record CelestiaPortalShape(BlockPos center) {
    private static boolean isRing(int i, int j) {
        int a = Math.max(Math.abs(i), Math.abs(j));
        return a == 2 && !(Math.abs(i) == 2 && Math.abs(j) == 2);
    }

    private static boolean isKeyedFrame(BlockState state) {
        return state.is(ModBlocks.CELESTIA_PORTAL.get()) && state.getValue(CelestiaPortalBlock.KEY);
    }

    /** Ищет полностью заполненное ключами кольцо, в которое входит блок clicked. */
    public static Optional<CelestiaPortalShape> find(Level level, BlockPos clicked) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos c = clicked.offset(dx, 0, dz);
                if (!isRing(-dx, -dz)) continue; // clicked должен лежать на кольце вокруг c
                if (complete(level, c)) return Optional.of(new CelestiaPortalShape(c));
            }
        }
        return Optional.empty();
    }

    private static boolean complete(Level level, BlockPos c) {
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (!isRing(i, j)) continue;
                if (!isKeyedFrame(level.getBlockState(c.offset(i, 0, j)))) return false;
            }
        }
        return true;
    }

    /** Заполняет внутреннее поле 3x3 порталом. */
    public void createPortal(Level level) {
        BlockState field = ModBlocks.CELESTIA_PORTAL_FIELD.get().defaultBlockState();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                level.setBlock(center.offset(i, 0, j), field, 18);
            }
        }
    }
}
