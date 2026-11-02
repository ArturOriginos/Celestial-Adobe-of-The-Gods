package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.EnchantLogic;
import com.example.celestial.advancement.ModAdvancements;
import com.example.celestial.entity.GuardianOfGods;
import com.example.celestial.registry.ModBlocks;
import com.example.celestial.registry.ModEnchantments;
import com.example.celestial.registry.ModItems;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CelestialMod.MODID)
public final class CelestiaEvents {
    private CelestiaEvents() {}

    /** Идёт валка дерева (чтобы другие обработчики не срабатывали рекурсивно). */
    public static boolean felling = false;

    private static boolean pickaxeBlock(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    /** Кирко-лопата Селестии мгновенно ломает землю, камень и руды. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getEntity().getMainHandItem().is(ModItems.CELESTIA_PICKAXE.get())) return;
        if (pickaxeBlock(event.getState())) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), 100000.0F));
        }
    }

    /** ... и корректно собирает дроп со всего, что относится к кирке и лопате. */
    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (!event.getEntity().getMainHandItem().is(ModItems.CELESTIA_PICKAXE.get())) return;
        if (pickaxeBlock(event.getTargetBlock())) {
            event.setCanHarvest(true);
        }
    }

    /** Блок Селестии, спавнеры (Шёлковое касание II) и валка деревьев (Гнев Природы). */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        ItemStack tool = player.getMainHandItem();

        // 1. Блок Селестии можно сломать только кирко-лопатой Селестии
        if (state.is(ModBlocks.CELESTIA_BLOCK.get()) && !tool.is(ModItems.CELESTIA_PICKAXE.get())
                && !tool.is(ModItems.CURSED_PICKAXE.get())) {
            event.setCanceled(true);
            return;
        }

        // 2. Шёлковое касание II добывает спавнеры
        if (state.is(Blocks.SPAWNER) && !player.isCreative()
                && ModEnchantments.getLevel(level, tool, Enchantments.SILK_TOUCH) >= 2) {
            ItemStack drop = new ItemStack(Items.SPAWNER);
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                CompoundTag tag = be.saveWithoutMetadata(level.registryAccess());
                BlockItem.setBlockEntityData(drop, be.getType(), tag);
            }
            Block.popResource(level, pos, drop);
            return;
        }

        // 3. Гнев Природы на топоре Селестии: мгновенно срубает всё дерево
        if (!felling && state.is(BlockTags.LOGS) && tool.is(ModItems.CELESTIA_AXE.get())
                && ModEnchantments.getLevel(level, tool, ModEnchantments.NATURES_WRATH) > 0) {
            felling = true;
            try {
                Set<BlockPos> seen = new HashSet<>();
                Deque<BlockPos> queue = new ArrayDeque<>();
                seen.add(pos);
                queue.add(pos);
                while (!queue.isEmpty() && seen.size() < 2500) {
                    BlockPos p = queue.poll();
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                BlockPos n = p.offset(dx, dy, dz);
                                if (Math.abs(n.getX() - pos.getX()) > 24 || Math.abs(n.getZ() - pos.getZ()) > 24
                                        || n.getY() < pos.getY() - 2 || n.getY() > pos.getY() + 60) continue;
                                if (seen.contains(n) || !level.getBlockState(n).is(BlockTags.LOGS)) continue;
                                seen.add(n);
                                queue.add(n);
                            }
                        }
                    }
                }
                for (BlockPos p : seen) {
                    if (!p.equals(pos)) player.gameMode.destroyBlock(p);
                }
            } finally {
                felling = false;
            }
        }
    }

    /** Ножницы Селестии: 1 урон, но 2% шанс мгновенно убить любого моба. */
    @SubscribeEvent
    public static void onShearsHit(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return;
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        if (!player.getMainHandItem().is(ModItems.CELESTIA_SHEARS.get())) return;
        LivingEntity target = event.getEntity();
        if (target instanceof Player || target instanceof GuardianOfGods || !target.isAlive()) return;
        int fortune = ModEnchantments.getLevel(player.level(), player.getMainHandItem(), Enchantments.FORTUNE);
        float chance = 0.02F + fortune * (0.08F / 6.0F); // Удача VI = 10%
        if (player.getRandom().nextFloat() < chance) {
            target.hurt(player.damageSources().playerAttack(player), Float.MAX_VALUE);
        }
    }

    /** Обычная наковальня: ножницы + книга с Гневом Природы => Ножницы Селестии. */
    @SubscribeEvent
    public static void onAnvilShears(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!EnchantLogic.isShears(left) || !EnchantLogic.isAdvancedBook(right)) return;
        ItemEnchantments stored = right.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
            if (EnchantLogic.isNaturesWrath(entry.getKey())) {
                event.setOutput(new ItemStack(ModItems.CELESTIA_SHEARS.get()));
                event.setCost(10);
                event.setMaterialCost(1);
                return;
            }
        }
    }

    /** Достижения за убийство Хранителя Богов: первое и последующие. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof GuardianOfGods)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (ModAdvancements.isDone(player, "kill_guardian")) {
            ModAdvancements.grant(player, "kill_guardian_twice");
        } else {
            ModAdvancements.grant(player, "kill_guardian");
        }
    }

    private static final String WITHER_KILLS = "celestial_wither_kills";

    /** Эндер Дракон: душа выпадает всегда. Визер: шанс 25%, а каждое 5-е убийство без дропа - гарантированная душа. */
    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        boolean dragon = entity instanceof EnderDragon;
        boolean wither = entity instanceof WitherBoss;
        if (!dragon && !wither) return;

        String key = dragon ? "item.celestial.guardian_soul.from_ender_dragon" : "item.celestial.guardian_soul.from_wither";
        boolean drop = dragon || level.random.nextFloat() < 0.25F;
        if (wither && event.getSource().getEntity() instanceof ServerPlayer killer) {
            CompoundTag data = killer.getPersistentData();
            int kills = data.getInt(WITHER_KILLS) + 1;
            if (kills >= 5) drop = true;
            data.putInt(WITHER_KILLS, drop ? 0 : kills);
        }
        if (!drop) return;

        ItemStack soul = new ItemStack(ModItems.GUARDIAN_SOUL.get());
        soul.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable(key).withStyle(ChatFormatting.GOLD))));

        Vec3 pos = entity.position();
        if (entity instanceof EnderDragon && level.dimension() == Level.END) {
            // дракон умирает в воздухе - кладём душу у портала, чтобы она не упала в бездну
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.ZERO);
            pos = Vec3.atBottomCenterOf(top.above());
        }
        ItemEntity item = new ItemEntity(level, pos.x, pos.y, pos.z, soul);
        item.setDefaultPickUpDelay();
        item.setGlowingTag(true);
        level.addFreshEntity(item);
    }

    /** Счётчик убийств Визера переживает смерть игрока. */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getEntity().getPersistentData().putInt(WITHER_KILLS,
                    event.getOriginal().getPersistentData().getInt(WITHER_KILLS));
        }
    }

    /** Поглотитель Душ (меч): шанс 2% за уровень, что с убитого моба выпадет его яйцо призыва. */
    @SubscribeEvent
    public static void onSoulDevourer(LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getSource().getDirectEntity() != player) return;
        if (target instanceof Player || target instanceof GuardianOfGods) return;
        int lvl = ModEnchantments.getLevel(player.level(), player.getMainHandItem(), ModEnchantments.SOUL_DEVOURER);
        if (lvl <= 0) return;
        SpawnEggItem egg = SpawnEggItem.byId(target.getType());
        if (egg == null || player.getRandom().nextFloat() >= lvl * 0.02F) return;
        target.spawnAtLocation(new ItemStack(egg));
    }
}
