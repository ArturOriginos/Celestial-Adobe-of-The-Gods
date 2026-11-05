package com.example.celestial;

import com.example.celestial.registry.ModEnchantments;
import com.example.celestial.registry.ModItems;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Общая логика для клиента (превью цены) и сервера (проверка и применение). */
public final class EnchantLogic {
    private EnchantLogic() {}

    /** Кастомное зачарование этого мода (требует улучшенную книгу). */
    public static boolean isCustom(Holder<Enchantment> ench) {
        return ench.unwrapKey()
                .map(key -> key.location().getNamespace().equals(CelestialMod.MODID))
                .orElse(false);
    }

    public static boolean isAdvancedBook(ItemStack stack) {
        return stack.is(ModItems.ADVANCED_BOOK.get());
    }

    /** Любая "книга": обычная, зачарованная или улучшенная. */
    public static boolean isBook(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK) || isAdvancedBook(stack);
    }

    /** Душа проклятого: вторая составляющая предметов проклятых в Солнечной наковальне. */
    public static boolean isCursedSoul(ItemStack stack) {
        return stack.is(ModItems.CURSED_SOUL.get());
    }

    /** Душа Хранителя богов: с ней в Солнечной наковальне делается броня Селестии. */
    public static boolean isGuardianSoul(ItemStack stack) {
        return stack.is(ModItems.GUARDIAN_SOUL.get());
    }

    /**
     * Солнечная наковальня: незеритовый нагрудник/ботинки + душа Хранителя богов => броня Селестии.
     * Зачарования переходят на новый предмет. Если предмет не подходит - пустой стек.
     */
    public static ItemStack celestiaArmorUpgrade(ItemStack tool) {
        Item result = null;
        if (tool.is(Items.NETHERITE_CHESTPLATE)) result = ModItems.CELESTIA_CHESTPLATE.get();
        else if (tool.is(Items.NETHERITE_BOOTS)) result = ModItems.CELESTIA_BOOTS.get();
        if (result == null) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(result);
        ItemEnchantments enchants = tool.get(DataComponents.ENCHANTMENTS);
        if (enchants != null && !enchants.isEmpty()) out.set(DataComponents.ENCHANTMENTS, enchants);
        return out;
    }

    /**
     * Солнечная наковальня: предмет Селестии + душа проклятого => такой же предмет проклятых.
     * Зачарования переходят на новый предмет. Если предмет не подходит - пустой стек.
     */
    public static ItemStack cursedUpgrade(ItemStack tool) {
        Item result = null;
        if (tool.is(ModItems.CELESTIA_SWORD.get())) result = ModItems.CURSED_SWORD.get();
        else if (tool.is(ModItems.CELESTIA_AXE.get())) result = ModItems.CURSED_AXE.get();
        else if (tool.is(ModItems.CELESTIA_PICKAXE.get())) result = ModItems.CURSED_PICKAXE.get();
        else if (tool.is(ModItems.CELESTIA_SHIELD.get())) result = ModItems.CURSED_SHIELD.get();
        if (result == null) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(result);
        ItemEnchantments enchants = tool.get(DataComponents.ENCHANTMENTS);
        if (enchants != null && !enchants.isEmpty()) out.set(DataComponents.ENCHANTMENTS, enchants);
        return out;
    }

    public static boolean isAnvil(ItemStack stack) {
        return stack.is(Items.ANVIL) || stack.is(Items.CHIPPED_ANVIL) || stack.is(Items.DAMAGED_ANVIL);
    }

    public static boolean isShears(ItemStack stack) {
        return stack.is(Items.SHEARS);
    }

    public static boolean isHelios(Holder<Enchantment> ench) {
        return ench.is(ModEnchantments.HELIOS_BLESSING);
    }

    public static boolean isNaturesWrath(Holder<Enchantment> ench) {
        return ench.is(ModEnchantments.NATURES_WRATH);
    }

    public static boolean isSupportedBy(Holder<Enchantment> ench, ItemStack stack) {
        return ench.value().definition().supportedItems().contains(stack.getItemHolder());
    }

    /**
     * Что можно положить на стол:
     *  - улучшенная книга принимает ТОЛЬКО кастомные зачарования;
     *  - обычная наковальня принимает ТОЛЬКО Благословение Гелиоса (становится Солнечной наковальней);
     *  - обычные ножницы принимают ТОЛЬКО Гнев Природы (становятся Ножницами Селестии);
     *  - обычная книга и инструменты принимают только ванильные (кастомные - через улучшенную книгу и Солнечную наковальню).
     */
    public static boolean canApply(Holder<Enchantment> ench, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (isAdvancedBook(stack)) return isCustom(ench);
        if (isAnvil(stack)) return isHelios(ench);
        if (isShears(stack)) return isNaturesWrath(ench);
        if (isCustom(ench)) return false;
        return isBook(stack) || isSupportedBy(ench, stack);
    }

    public static DataComponentType<ItemEnchantments> enchantComponent(ItemStack stack) {
        boolean stored = stack.is(Items.ENCHANTED_BOOK) || isAdvancedBook(stack);
        return stored ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
    }

    public static ItemEnchantments existing(ItemStack stack) {
        return stack.getOrDefault(enchantComponent(stack), ItemEnchantments.EMPTY);
    }

    /** Стол увеличивает максимальный уровень ванильных зачарований вдвое (Эффективность 10, Шёлковое касание 2). */
    public static int maxLevel(Holder<Enchantment> ench) {
        int base = ench.value().definition().maxLevel();
        if (isCustom(ench)) return base;
        return Math.min(255, base * 2);
    }

    /**
     * Цена в уровнях опыта: сумма (уровень * (2 + стоимость на наковальне)) по каждому
     * выбранному зачарованию, но не больше AEConfig.MAX_COST (по умолчанию 100).
     */
    public static int cost(ItemStack stack, Map<Holder<Enchantment>, Integer> wanted) {
        ItemEnchantments existing = existing(stack);
        int total = 0;
        for (Map.Entry<Holder<Enchantment>, Integer> e : wanted.entrySet()) {
            int level = e.getValue();
            if (level <= existing.getLevel(e.getKey())) continue;
            total += level * (2 + e.getKey().value().definition().anvilCost());
        }
        if (total <= 0) return 0;
        return Math.min(AEConfig.MAX_COST.get(), total);
    }
}
