package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.EnchantLogic;
import com.example.celestial.registry.ModBlocks;
import com.example.celestial.registry.ModEffects;
import com.example.celestial.registry.ModEnchantments;
import com.example.celestial.registry.ModItems;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Поведение кастомных зачарований + перенос их с улучшенной книги на предмет в наковальне. */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class EnchantEvents {
    private EnchantEvents() {}

    // защита от рекурсии (события срабатывают из самих же обработчиков)
    private static boolean mining = false;
    private static boolean cleaving = false;

    // ---------------------------------------------------------------- Синдром Майнера (3x3)
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (mining || CelestiaEvents.felling) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isShiftKeyDown()) return; // на Shift ломаем один блок

        ItemStack tool = player.getMainHandItem();
        Level level = player.level();
        if (ModEnchantments.getLevel(level, tool, ModEnchantments.MINER_SYNDROME) <= 0) return;

        BlockPos origin = event.getPos();
        float originHardness = event.getState().getDestroySpeed(level, origin);

        // Плоскость 3x3 перпендикулярна грани, по которой бьёт игрок
        Direction.Axis axis;
        HitResult hit = player.pick(player.blockInteractionRange() + 1.0, 1.0F, false);
        if (hit instanceof BlockHitResult bhr && bhr.getBlockPos().equals(origin)) {
            axis = bhr.getDirection().getAxis();
        } else {
            axis = Direction.orderedByNearest(player)[0].getAxis();
        }

        mining = true;
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    if (tool.isEmpty()) return; // инструмент сломался
                    if (tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage() - 1) return;

                    BlockPos pos = switch (axis) {
                        case X -> origin.offset(0, a, b);
                        case Y -> origin.offset(a, 0, b);
                        case Z -> origin.offset(a, b, 0);
                    };
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    float hardness = state.getDestroySpeed(level, pos);
                    if (hardness < 0) continue;                       // бедрок и т.п.
                    if (hardness > originHardness * 2.5F + 1.0F) continue; // не трогаем куда более твёрдое
                    boolean correct = tool.isCorrectToolForDrops(state)
                            || ((tool.is(com.example.celestial.registry.ModItems.CELESTIA_PICKAXE.get())
                            || tool.is(com.example.celestial.registry.ModItems.CURSED_PICKAXE.get()))
                            && (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                            || state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)))
                            || (tool.is(com.example.celestial.registry.ModItems.PAXEL.get())
                            && (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)
                            || state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)
                            || state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)));
                    if (!correct) continue;  // только то, что инструмент реально добывает

                    player.gameMode.destroyBlock(pos);
                }
            }
        } finally {
            mining = false;
        }
    }

    // ------------------------------------------------ Убийца (3x3 / 5x5) + Кровотечение
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (cleaving) return;
        DamageSource source = event.getSource();
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return;
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        float damage = event.getNewDamage();
        if (damage <= 0) return;

        ItemStack weapon = player.getMainHandItem();
        Level level = player.level();
        int slayer = ModEnchantments.getLevel(level, weapon, ModEnchantments.SLAYER);
        int bleeding = ModEnchantments.getLevel(level, weapon, ModEnchantments.BLEEDING);
        if (slayer <= 0 && bleeding <= 0) return;

        LivingEntity target = event.getEntity();
        List<LivingEntity> hitEntities = new ArrayList<>();
        hitEntities.add(target);

        if (slayer > 0) {
            int radius = slayer; // 1 -> 3x3, 2 -> 5x5
            AABB area = new AABB(target.blockPosition()).inflate(radius, 1.0, radius);
            cleaving = true;
            try {
                for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, area,
                        e -> canCleave(player, target, e))) {
                    other.hurt(source, damage);
                    hitEntities.add(other);
                }
            } finally {
                cleaving = false;
            }
        }

        if (bleeding > 0) {
            int duration = 80 + 40 * bleeding; // 6 / 8 / 10 секунд
            for (LivingEntity e : hitEntities) {
                if (e.isAlive()) {
                    e.addEffect(new MobEffectInstance(ModEffects.BLEEDING, duration, bleeding - 1), player);
                }
            }
        }
    }

    private static boolean canCleave(Player player, LivingEntity primary, LivingEntity e) {
        if (e == primary || e == player || !e.isAlive()) return false;
        if (e instanceof Player || e instanceof ArmorStand || e instanceof AbstractVillager) return false;
        if (e instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID())) return false;
        return !e.isAlliedTo(player);
    }

    // -------------------------------------------------------------- Увеличение опыта
    @SubscribeEvent
    public static void onXp(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || event.getAmount() <= 0) return;
        int lvl = ModEnchantments.getLevel(player.level(), player.getMainHandItem(), ModEnchantments.EXPERIENCE_BOOST);
        if (lvl <= 0) return;
        event.setAmount(event.getAmount() * (lvl >= 2 ? 4 : 2));
    }

    // ------------------------------------------------------------------------- Магнит
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.isSpectator() || player.isShiftKeyDown()) return; // на Shift магнит выключается

        boolean magnet = ModEnchantments.getLevel(level, player.getMainHandItem(), ModEnchantments.MAGNET) > 0
                || ModEnchantments.getLevel(level, player.getOffhandItem(), ModEnchantments.MAGNET) > 0;
        if (!magnet) return;

        AABB area = new AABB(player.blockPosition()).inflate(2.0); // 5x5x5
        Vec3 center = player.position().add(0, player.getBbHeight() * 0.5, 0);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area,
                i -> i.isAlive() && !i.hasPickUpDelay())) {
            Vec3 dir = center.subtract(item.position());
            double dist = dir.length();
            if (dist < 0.3) continue;
            double speed = Math.min(0.35, 0.05 + dist * 0.2);
            item.setDeltaMovement(dir.scale(speed / dist));
            item.hasImpulse = true;
        }
    }

    // ------- Обычная наковальня: наковальня + книга с Благословением Гелиоса => Солнечная наковальня
    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!EnchantLogic.isAnvil(left) || !EnchantLogic.isAdvancedBook(right)) return;

        ItemEnchantments stored = right.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
            if (EnchantLogic.isHelios(entry.getKey())) {
                event.setOutput(new ItemStack(ModBlocks.SOLAR_ANVIL_ITEM.get()));
                event.setCost(10);
                event.setMaterialCost(1);
                return;
            }
        }
    }
}
