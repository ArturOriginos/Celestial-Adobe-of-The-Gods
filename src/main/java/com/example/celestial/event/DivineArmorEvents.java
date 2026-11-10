package com.example.celestial.event;

import com.example.celestial.CelestialMod;
import com.example.celestial.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Броня Бога.
 *  Ботинки: +20 максимального здоровья (1 строка сердец) и полёт (см. FlightRingEvents).
 *  Нагрудник: +40 максимального здоровья (2 строки) и божественная аура.
 * Бонусы действуют, пока предмет надет, и пропадают после снятия.
 */
@EventBusSubscriber(modid = CelestialMod.MODID)
public final class DivineArmorEvents {
    private static final ResourceLocation BOOTS_HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "divine_boots_health");
    private static final ResourceLocation CHEST_HEALTH_ID =
            ResourceLocation.fromNamespaceAndPath(CelestialMod.MODID, "divine_chestplate_health");
    public static final double BOOTS_HEALTH_BONUS = 20.0D;
    public static final double CHEST_HEALTH_BONUS = 40.0D;

    private DivineArmorEvents() {}

    public static boolean wearsBoots(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.DIVINE_BOOTS.get());
    }

    public static boolean wearsChestplate(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.DIVINE_CHESTPLATE.get());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 5 != 0) return;

        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            boolean changed = sync(health, BOOTS_HEALTH_ID, BOOTS_HEALTH_BONUS, wearsBoots(player));
            changed |= sync(health, CHEST_HEALTH_ID, CHEST_HEALTH_BONUS, wearsChestplate(player));
            if (changed && player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        }
    }

    /** Добавляет или убирает модификатор; возвращает true, если что-то изменилось. */
    private static boolean sync(AttributeInstance attribute, ResourceLocation id, double amount, boolean wanted) {
        boolean has = attribute.getModifier(id) != null;
        if (wanted && !has) {
            attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
            return true;
        }
        if (!wanted && has) {
            attribute.removeModifier(id);
            return true;
        }
        return false;
    }
}
