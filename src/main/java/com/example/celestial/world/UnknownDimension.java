package com.example.celestial.world;

import com.example.celestial.CelestialMod;
import com.example.celestial.entity.AllKnowingAngel;
import com.example.celestial.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Пустое измерение «???» / «Неизвестно»: сюда телепортирует Проклятая книга.
 * В нём стоит одна структура (чертёж eye_structure). Игрок появляется на полу большого зала,
 * а Всезнающий Ангел - на вершине пьедестала в центре зала.
 */
public final class UnknownDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "unknown"));

    public static final String BLUEPRINT = "eye_structure";
    /** Юго-западный нижний угол чертежа в мире: центр зала (x48, z48 в чертеже) попадает в (0, 0). */
    public static final BlockPos ORIGIN = new BlockPos(-48, 40, -48);
    private static final int SIZE_X = 97, SIZE_Y = 76, SIZE_Z = 96;

    /** Пол зала в чертеже: y54, значит игрок стоит на y55; к северу от пьедестала Ангел. */
    public static final Vec3 PLAYER_SPAWN = Vec3.atBottomCenterOf(ORIGIN.offset(48, 55, 64));
    /** Вершина пьедестала: y58 (светящийся камень в центре), Ангел стоит на y59. */
    public static final Vec3 ANGEL_SPAWN = Vec3.atBottomCenterOf(ORIGIN.offset(48, 59, 48));

    private UnknownDimension() {}

    /** Ставит структуру один раз; признак готовности - блок на вершине пьедестала. */
    public static void ensureBuilt(ServerLevel level) {
        if (!level.getBlockState(ORIGIN.offset(48, 58, 48)).isAir()) return;
        CelestiaBlueprint bp = CelestiaBlueprint.getNoAir(BLUEPRINT);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
        for (CelestiaBlueprint.Entry e : bp.blocks) {
            pos.set(ORIGIN.getX() + e.x(), ORIGIN.getY() + e.y(), ORIGIN.getZ() + e.z());
            level.setBlock(pos, e.state(), flags);
        }
    }

    /** Если в структуре нет живого Ангела, ставит нового на пьедестал. */
    public static void ensureAngel(ServerLevel level) {
        AABB box = new AABB(ORIGIN.getX(), ORIGIN.getY(), ORIGIN.getZ(),
                ORIGIN.getX() + SIZE_X, ORIGIN.getY() + SIZE_Y, ORIGIN.getZ() + SIZE_Z);
        if (!level.getEntitiesOfClass(AllKnowingAngel.class, box, a -> a.isAlive()).isEmpty()) return;
        AllKnowingAngel angel = ModEntities.ALL_KNOWING_ANGEL.get().create(level);
        if (angel == null) return;
        angel.moveTo(ANGEL_SPAWN.x, ANGEL_SPAWN.y, ANGEL_SPAWN.z, 0.0F, 0.0F); // лицом на юг, к игроку
        angel.finalizeSpawn(level, level.getCurrentDifficultyAt(angel.blockPosition()), MobSpawnType.EVENT, null);
        level.addFreshEntity(angel);
    }
}
