package com.example.celestial;

import com.example.celestial.registry.ModEnchantments;
import com.example.celestial.registry.ModItems;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
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

    public static boolean isAnvil(ItemStack stack) {
        return stack.is(Items.ANVIL) || stack.is(Items.CHIPPED_ANVIL) || stack.is(Items.DAMAGED_ANVIL);
    }

    public static boolean isHelios(Holder<Enchantment> ench) {
        return ench.is(ModEnchantments.HELIOS_BLESSING);
    }

    public static boolean isSupportedBy(Holder<Enchantment> ench, ItemStack stack) {
        return ench.value().definition().supportedItems().contains(stack.getItemHolder());
    }

    /**
     * Что можно положить на стол:
     *  - улучшенная книга принимает ТОЛЬКО кастомные зачарования;
     *  - обычная наковальня принимает ТОЛЬКО Благословение Гелиоса (превращается в Солнечную наковальню);
     *  - обычная книга и инструменты принимают только ванильные (кастомные - через улучшенную книгу и Солнечную наковальню).
     */
    public static boolean canApply(Holder<Enchantment> ench, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (isAdvancedBook(stack)) return isCustom(ench);
        if (isAnvil(stack)) return isHelios(ench); // наковальня принимает только Благословение Гелиоса
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

    public static int maxLevel(Holder<Enchantment> ench) {
        int base = ench.value().definition().maxLevel();
        if (isCustom(ench) || base <= 1 || !AEConfig.ALLOW_ABOVE_MAX.get()) return base;
        return Math.max(base, AEConfig.ABSOLUTE_MAX_LEVEL.get());
    }

    /**
     * Цена в уровнях опыта: сумма (уровень * (2 + стоимость на наковальне)) по каждому
     * выбранному зачарованию, но не больше AEConfig.MAX_COST (по умолчанию 100).
     * Зачарования, которые не повышают текущий уровень на предмете, бесплатны.
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
