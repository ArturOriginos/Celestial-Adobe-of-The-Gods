package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.registry.ModItems;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Поведение инструментов проклятых: кирко-лопата, топор и щит (меч - обычный атрибут урона 40). */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class CursedEvents {
    private CursedEvents() {}

    /** Идёт валка дерева (чтобы не запускать её рекурсивно для каждого сломанного бревна). */
    private static boolean felling = false;
    /** Идёт возврат урона (защита от бесконечной цепочки, если щиты у обоих). */
    private static boolean reflecting = false;

    /** Предел валки: хватает даже на гигантское Селестийское дерево. */
    private static final int FELL_MAX_BLOCKS = 40000;
    private static final int FELL_RADIUS = 64;
    private static final int FELL_HEIGHT = 200;

    /** Кирко-лопата ломает всё, кроме неразрушимых блоков (бедрок и т.п.) и деревьев (брёвен). */
    private static boolean pickBreakable(BlockState state) {
        return state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) >= 0.0F
                && !state.is(BlockTags.LOGS);
    }

    /** Топор: дерево (брёвна) и листва. */
    private static boolean axeBreakable(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }

    // ------------------------------------------------------------ Мгновенная добыча
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        if (tool.is(ModItems.CURSED_PICKAXE.get())) {
            if (pickBreakable(event.getState())) {
                event.setNewSpeed(Math.max(event.getNewSpeed(), 1.0E7F));
            }
        } else if (tool.is(ModItems.CURSED_AXE.get())) {
            if (axeBreakable(event.getState())) {
                event.setNewSpeed(Math.max(event.getNewSpeed(), 1.0E7F));
            }
        }
    }

    /** Дроп собирается с любого блока, который инструмент ломает мгновенно. */
    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        if (tool.is(ModItems.CURSED_PICKAXE.get()) && pickBreakable(event.getTargetBlock())) {
            event.setCanHarvest(true);
        } else if (tool.is(ModItems.CURSED_AXE.get()) && axeBreakable(event.getTargetBlock())) {
            event.setCanHarvest(true);
        }
    }

    // ------------------------------------------------------------ Топор: валка дерева целиком
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (felling) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!player.getMainHandItem().is(ModItems.CURSED_AXE.get())) return;
        if (!event.getState().is(BlockTags.LOGS)) return;

        BlockPos origin = event.getPos();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        seen.add(origin);
        queue.add(origin);
        while (!queue.isEmpty() && seen.size() < FELL_MAX_BLOCKS) {
            BlockPos p = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos n = p.offset(dx, dy, dz);
                        if (Math.abs(n.getX() - origin.getX()) > FELL_RADIUS
                                || Math.abs(n.getZ() - origin.getZ()) > FELL_RADIUS
                                || n.getY() < origin.getY() - 3
                                || n.getY() > origin.getY() + FELL_HEIGHT) continue;
                        if (seen.contains(n) || !level.getBlockState(n).is(BlockTags.LOGS)) continue;
                        seen.add(n);
                        queue.add(n);
                    }
                }
            }
        }

        felling = true;
        try {
            for (BlockPos p : seen) {
                if (!p.equals(origin)) player.gameMode.destroyBlock(p);
            }
        } finally {
            felling = false;
        }
    }

    // ------------------------------------------------------------ Щит: возврат урона
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (reflecting) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.isBlocking() || !player.getUseItem().is(ModItems.CURSED_SHIELD.get())) return;

        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_SHIELD)) return;
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == player) return;

        // как у ванильного щита: закрывает только удары спереди
        Vec3 from = source.getSourcePosition();
        if (from != null) {
            Vec3 view = player.getViewVector(1.0F);
            Vec3 dir = from.vectorTo(player.position()).normalize();
            dir = new Vec3(dir.x, 0.0, dir.z);
            if (dir.dot(view) >= 0.0) return;
        }

        float amount = event.getAmount();
        if (amount <= 0.0F) return;

        reflecting = true;
        try {
            // магический урон от имени игрока: игнорирует броню, так что возвращается весь урон
            attacker.hurt(player.damageSources().indirectMagic(player, player), amount);
        } finally {
            reflecting = false;
        }
    }
}
