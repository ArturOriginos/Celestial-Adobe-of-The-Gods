package com.example.celestial.menu;

import com.example.celestial.EnchantLogic;
import com.example.celestial.registry.ModBlocks;
import com.example.celestial.registry.ModMenus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class AdvancedEnchantingMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT = 0;
    public static final int INV_LEFT = 34;
    public static final int INV_TOP = 150;

    private final Container input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            AdvancedEnchantingMenu.this.slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;

    /** Клиентский конструктор. */
    public AdvancedEnchantingMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public AdvancedEnchantingMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.ADVANCED_ENCHANTING.get(), id);
        this.access = access;

        this.addSlot(new Slot(input, 0, 8, 18) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, INV_LEFT + col * 18, INV_TOP + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, INV_LEFT + col * 18, INV_TOP + 58));
        }
    }

    /** Серверная проверка и применение запроса игрока. */
    public void tryEnchant(ServerPlayer player, Map<ResourceLocation, Integer> request) {
        ItemStack stack = input.getItem(INPUT_SLOT);
        if (stack.isEmpty() || request.isEmpty() || request.size() > 64) return;

        Registry<Enchantment> registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        Map<Holder<Enchantment>, Integer> wanted = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> e : request.entrySet()) {
            Optional<Holder.Reference<Enchantment>> opt = registry.getHolder(e.getKey());
            if (opt.isEmpty()) return;
            Holder<Enchantment> holder = opt.get();
            int level = e.getValue();
            if (level < 1 || level > EnchantLogic.maxLevel(holder) || !EnchantLogic.canApply(holder, stack)) return;
            wanted.put(holder, level);
        }

        // Выбранные зачарования не должны конфликтовать друг с другом
        List<Holder<Enchantment>> keys = new ArrayList<>(wanted.keySet());
        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                if (!Enchantment.areCompatible(keys.get(i), keys.get(j))) return;
            }
        }

        int cost = EnchantLogic.cost(stack, wanted);
        if (cost <= 0) return;

        boolean creative = player.getAbilities().instabuild;
        if (!creative && player.experienceLevel < cost) {
            player.displayClientMessage(
                    Component.translatable("message.celestial.not_enough_levels", cost), true);
            return;
        }

        ItemStack result;
        if (EnchantLogic.isAnvil(stack)) {
            // Благословение Гелиоса превращает обычную наковальню в Солнечную
            result = new ItemStack(ModBlocks.SOLAR_ANVIL_ITEM.get());
        } else {
            result = stack.is(Items.BOOK) ? new ItemStack(Items.ENCHANTED_BOOK) : stack.copy();
            result.setCount(1);

            DataComponentType<ItemEnchantments> type = EnchantLogic.enchantComponent(result);
            ItemEnchantments.Mutable mutable =
                    new ItemEnchantments.Mutable(result.getOrDefault(type, ItemEnchantments.EMPTY));
            for (Map.Entry<Holder<Enchantment>, Integer> e : wanted.entrySet()) {
                Holder<Enchantment> selected = e.getKey();
                if (e.getValue() <= mutable.getLevel(selected)) continue; // не понижаем уровень
                // Заменяем конфликтующие зачарования, которые уже были на предмете
                mutable.removeIf(other -> !other.equals(selected) && !Enchantment.areCompatible(other, selected));
                mutable.set(selected, e.getValue());
            }
            result.set(type, mutable.toImmutable());
        }

        if (!creative) {
            player.giveExperienceLevels(-cost);
        }
        input.setItem(INPUT_SLOT, result);
        player.awardStat(Stats.ENCHANT_ITEM);
        CriteriaTriggers.ENCHANTED_ITEM.trigger(player, result, cost);

        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F));
        this.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index == INPUT_SLOT) {
                if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ADVANCED_ENCHANTING_TABLE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> this.clearContainer(player, input));
    }
}
