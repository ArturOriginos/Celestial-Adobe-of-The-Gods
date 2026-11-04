package com.example.celestial.world;

import com.example.celestial.block.CelestiaPortalFieldBlock;
import com.example.celestial.registry.ModBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Рамка портала Селестии: вертикальный прямоугольник из блоков портала, внутри 2..21 в ширину и 3..21 в высоту. */
public record CelestiaPortalShape(Direction.Axis axis, BlockPos bottomLeft, int width, int height) {
    private static final int MIN_W = 2, MAX_W = 21, MIN_H = 3, MAX_H = 21;

    private static boolean isFrame(BlockState state) {
        return state.is(ModBlocks.CELESTIA_PORTAL.get());
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(ModBlocks.CELESTIA_PORTAL_FIELD.get());
    }

    /** Ищет рамку, к которой прилегает блок clicked (ключом нажали на блок рамки). */
    public static Optional<CelestiaPortalShape> find(Level level, BlockPos clicked) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            for (Direction dir : Direction.values()) {
                BlockPos start = clicked.relative(dir);
                if (!isEmpty(level.getBlockState(start))) continue;
                Optional<CelestiaPortalShape> shape = tryFrom(level, start, axis);
                if (shape.isPresent()) return shape;
            }
        }
        return Optional.empty();
    }

    private static Optional<CelestiaPortalShape> tryFrom(Level level, BlockPos start, Direction.Axis axis) {
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction left = right.getOpposite();

        // вниз до нижней перекладины
        BlockPos p = start;
        int n = 0;
        while (n <= MAX_H && isEmpty(level.getBlockState(p.below()))) {
            p = p.below();
            n++;
        }
        if (!isFrame(level.getBlockState(p.below()))) return Optional.empty();

        // влево до левой стойки
        int m = 0;
        while (m <= MAX_W && isEmpty(level.getBlockState(p.relative(left)))) {
            p = p.relative(left);
            m++;
        }
        if (!isFrame(level.getBlockState(p.relative(left)))) return Optional.empty();
        BlockPos bl = p;

        // ширина до правой стойки
        int w = 0;
        while (w <= MAX_W && isEmpty(level.getBlockState(bl.relative(right, w)))) w++;
        if (w < MIN_W || w > MAX_W || !isFrame(level.getBlockState(bl.relative(right, w)))) return Optional.empty();

        // высота до верхней перекладины, на каждом ряду по бокам стойки
        int h = 0;
        while (h <= MAX_H) {
            BlockPos row = bl.above(h);
            if (!isFrame(level.getBlockState(row.relative(left))) || !isFrame(level.getBlockState(row.relative(right, w)))) {
                return Optional.empty();
            }
            boolean allEmpty = true;
            for (int i = 0; i < w; i++) {
                if (!isEmpty(level.getBlockState(row.relative(right, i)))) {
                    allEmpty = false;
                    break;
                }
            }
            if (allEmpty) {
                h++;
                continue;
            }
            for (int i = 0; i < w; i++) {
                if (!isFrame(level.getBlockState(row.relative(right, i)))) return Optional.empty();
            }
            break;
        }
        if (h < MIN_H || h > MAX_H) return Optional.empty();
        return Optional.of(new CelestiaPortalShape(axis, bl, w, h));
    }

    /** Заполняет рамку полем портала. */
    public void createPortal(Level level) {
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockState field = ModBlocks.CELESTIA_PORTAL_FIELD.get().defaultBlockState()
                .setValue(CelestiaPortalFieldBlock.AXIS, axis);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                level.setBlock(bottomLeft.relative(right, i).above(j), field, 18);
            }
        }
    }
}
