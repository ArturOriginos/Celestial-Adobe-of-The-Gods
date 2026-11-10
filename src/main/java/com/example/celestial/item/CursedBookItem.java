package com.example.celestial.item;

import com.example.celestial.event.UnknownDimensionEvents;
import com.example.celestial.world.CelestiaPortalPlacer;
import com.example.celestial.world.UnknownDimension;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Проклятая книга. ПКМ переносит в пустое измерение «???» (в структуру с Всезнающим Ангелом),
 * повторное ПКМ по книге внутри измерения возвращает туда, откуда игрок пришёл.
 */
public class CursedBookItem extends Item {
    private static final String RET_DIM = "celestial_book_return_dim";
    private static final String RET_X = "celestial_book_return_x";
    private static final String RET_Y = "celestial_book_return_y";
    private static final String RET_Z = "celestial_book_return_z";

    public CursedBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        player.getCooldowns().addCooldown(this, 60);

        MinecraftServer server = sp.server;
        if (level.dimension().equals(UnknownDimension.LEVEL)) leave(sp, server);
        else enter(sp, server);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private void enter(ServerPlayer player, MinecraftServer server) {
        ServerLevel dest = server.getLevel(UnknownDimension.LEVEL);
        if (dest == null) {
            player.displayClientMessage(Component.translatable("message.celestial.cursed_book.no_dimension"), true);
            return;
        }
        CompoundTag data = player.getPersistentData();
        data.putString(RET_DIM, player.level().dimension().location().toString());
        data.putDouble(RET_X, player.getX());
        data.putDouble(RET_Y, player.getY());
        data.putDouble(RET_Z, player.getZ());

        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.5F);
        UnknownDimension.ensureBuilt(dest);
        UnknownDimensionEvents.scheduleAngel();
        player.changeDimension(new DimensionTransition(dest, UnknownDimension.PLAYER_SPAWN, Vec3.ZERO,
                180.0F, 0.0F, DimensionTransition.DO_NOTHING));
        player.displayClientMessage(Component.translatable("message.celestial.cursed_book.enter"), true);
    }

    private void leave(ServerPlayer player, MinecraftServer server) {
        CompoundTag data = player.getPersistentData();
        ServerLevel dest = null;
        if (data.contains(RET_DIM)) {
            ResourceLocation rl = ResourceLocation.tryParse(data.getString(RET_DIM));
            if (rl != null) dest = server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
        }
        if (dest == null || dest.dimension().equals(UnknownDimension.LEVEL)) dest = server.overworld();

        Vec3 target;
        if (data.contains(RET_X) && data.getString(RET_DIM).equals(dest.dimension().location().toString())) {
            target = new Vec3(data.getDouble(RET_X), data.getDouble(RET_Y), data.getDouble(RET_Z));
        } else {
            target = Vec3.atBottomCenterOf(dest.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, dest.getSharedSpawnPos()));
        }
        target = CelestiaPortalPlacer.findSafe(dest, target);

        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.5F);
        player.changeDimension(new DimensionTransition(dest, target, Vec3.ZERO,
                player.getYRot(), player.getXRot(), DimensionTransition.DO_NOTHING));
        player.displayClientMessage(Component.translatable("message.celestial.cursed_book.leave"), true);
    }
}
