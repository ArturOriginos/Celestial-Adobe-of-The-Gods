package com.example.celestial.menu;

import com.example.celestial.EnchantLogic;
import com.example.celestial.registry.ModBlocks;
import com.example.celestial.registry.ModMenus;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Меню Солнечной наковальни: предмет + улучшенная книга -> предмет с кастомными зачарованиями. */
public class SolarAnvilMenu extends AbstractContainerMenu {
    public static final int TOOL_SLOT = 0;
    public static final int BOOK_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    /** Цена (в уровнях опыта) превращения предмета Селестии в предмет проклятых. */
    public static final int CURSED_UPGRADE_COST = 30;
    /** Цена (в уровнях опыта) создания брони Селестии. */
    public static final int ARMOR_UPGRADE_COST = 30;
    /** Цена (в уровнях опыта) создания Совершенного Пакселя. */
    public static final int PERFECT_PAXEL_COST = 50;

    private final Container inputs = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            SolarAnvilMenu.this.slotsChanged(this);
        }
    };
    private final Container result = new SimpleContainer(1);
    private final ContainerLevelAccess access;
    private final Player player;
    private final DataSlot cost = DataSlot.standalone();

    /** Клиентский конструктор. */
    public SolarAnvilMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public SolarAnvilMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.SOLAR_ANVIL.get(), id);
        this.access = access;
        this.player = inventory.player;

        this.addSlot(new Slot(inputs, TOOL_SLOT, 25, 53) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return !EnchantLogic.isBook(stack) && !EnchantLogic.isCursedSoul(stack) && !EnchantLogic.isGuardianSoul(stack)
                        && !EnchantLogic.isCelestiaGodSoul(stack);
            }
        });
        this.addSlot(new Slot(inputs, BOOK_SLOT, 74, 53) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return EnchantLogic.isAdvancedBook(stack) || EnchantLogic.isCursedSoul(stack) || EnchantLogic.isGuardianSoul(stack)
                        || EnchantLogic.isCelestiaGodSoul(stack);
            }
        });
        this.addSlot(new Slot(result, 0, 135, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player taker) {
                int c = cost.get();
                return hasItem() && c > 0 && (taker.getAbilities().instabuild || taker.experienceLevel >= c);
            }

            @Override
            public void onTake(Player taker, ItemStack stack) {
                int c = cost.get();
                if (!taker.getAbilities().instabuild) {
                    taker.giveExperienceLevels(-c);
                }
                inputs.setItem(TOOL_SLOT, ItemStack.EMPTY);
                inputs.setItem(BOOK_SLOT, ItemStack.EMPTY); // книга расходуется
                access.execute((lvl, pos) -> lvl.playSound(null, pos, SoundEvents.ANVIL_USE,
                        SoundSource.BLOCKS, 1.0F, lvl.random.nextFloat() * 0.1F + 0.9F));
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 97 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 159));
        }
        this.addDataSlot(cost);
    }

    public int getCost() {
        return cost.get();
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == inputs) updateResult();
    }

    private void updateResult() {
        if (player.level().isClientSide) return; // результат и цена синхронизируются с сервера
        result.setItem(0, ItemStack.EMPTY);
        cost.set(0);

        ItemStack tool = inputs.getItem(TOOL_SLOT);
        ItemStack book = inputs.getItem(BOOK_SLOT);
        // Паксель Богов и Проклятых + душа Бога Селестии => Совершенный Паксель
        if (EnchantLogic.isCelestiaGodSoul(book)) {
            ItemStack perfect = EnchantLogic.perfectPaxelUpgrade(tool);
            if (!perfect.isEmpty()) {
                result.setItem(0, perfect);
                cost.set(PERFECT_PAXEL_COST);
            }
            return;
        }
        // незеритовый нагрудник/ботинки + душа Хранителя богов => броня Селестии
        if (EnchantLogic.isGuardianSoul(book)) {
            ItemStack armor = EnchantLogic.celestiaArmorUpgrade(tool);
            if (!armor.isEmpty()) {
                result.setItem(0, armor);
                cost.set(ARMOR_UPGRADE_COST);
            }
            return;
        }
        // предмет Селестии + душа проклятого => предмет проклятых
        if (EnchantLogic.isCursedSoul(book)) {
            ItemStack upgraded = EnchantLogic.cursedUpgrade(tool);
            if (!upgraded.isEmpty()) {
                result.setItem(0, upgraded);
                cost.set(CURSED_UPGRADE_COST);
            }
            return;
        }

        if (tool.isEmpty() || !EnchantLogic.isAdvancedBook(book) || EnchantLogic.isBook(tool)) return;

        ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (stored.isEmpty()) return;

        if (EnchantLogic.isShears(tool)) {
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
                if (EnchantLogic.isNaturesWrath(entry.getKey())) {
                    result.setItem(0, new ItemStack(com.example.celestial.registry.ModItems.CELESTIA_SHEARS.get()));
                    cost.set(10);
                    return;
                }
            }
            return;
        }

        ItemStack out = tool.copy();
        out.setCount(1);
        ItemEnchantments.Mutable mutable =
                new ItemEnchantments.Mutable(out.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
        int total = 0;
        boolean changed = false;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
            Holder<Enchantment> ench = entry.getKey();
            int level = entry.getIntValue();
            if (!EnchantLogic.isSupportedBy(ench, tool)) continue;
            if (level <= mutable.getLevel(ench)) continue;
            mutable.removeIf(other -> !other.equals(ench) && !Enchantment.areCompatible(other, ench));
            mutable.set(ench, level);
            total += level;
            changed = true;
        }
        if (!changed) return;

        out.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        result.setItem(0, out);
        cost.set(Math.max(1, total));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index == RESULT_SLOT) {
                if (!slot.mayPickup(player)) return ItemStack.EMPTY;
                if (!this.moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(stack, copy);
            } else if (index < RESULT_SLOT) {
                if (!this.moveItemStackTo(stack, 3, 39, false)) return ItemStack.EMPTY;
            } else if (EnchantLogic.isAdvancedBook(stack) || EnchantLogic.isCursedSoul(stack) || EnchantLogic.isGuardianSoul(stack)
                    || EnchantLogic.isCelestiaGodSoul(stack)) {
                if (!this.moveItemStackTo(stack, BOOK_SLOT, BOOK_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (!EnchantLogic.isBook(stack)) {
                if (!this.moveItemStackTo(stack, TOOL_SLOT, TOOL_SLOT + 1, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SOLAR_ANVIL.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> this.clearContainer(player, inputs));
    }
}
