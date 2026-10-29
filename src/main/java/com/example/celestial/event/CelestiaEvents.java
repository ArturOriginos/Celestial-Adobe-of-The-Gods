package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.advancement.ModAdvancements;
import com.example.celestial.entity.GuardianOfGods;
import com.example.celestial.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = CelestialMod.MODID)
public final class CelestiaEvents {
    private CelestiaEvents() {}

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

    /** ... и корректно собирает дроп со всего, что относится к кирке и лопате (руды, обсидиан и т.д.). */
    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (!event.getEntity().getMainHandItem().is(ModItems.CELESTIA_PICKAXE.get())) return;
        if (pickaxeBlock(event.getTargetBlock())) {
            event.setCanHarvest(true);
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
}
