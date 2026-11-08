package com.example.celestial.jei;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.menu.SolarAnvilMenu;
import com.example.celestial.registry.ModBlocks;
import com.example.celestial.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/** Интеграция с JEI: показывает крафты Солнечной наковальни. Без JEI в игре не загружается. */
@JeiPlugin
public class CelestialJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new SolarAnvilCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(SolarAnvilCategory.TYPE, buildRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SOLAR_ANVIL_ITEM.get()), SolarAnvilCategory.TYPE);
    }

    /** Все крафты Солнечной наковальни (должны совпадать с SolarAnvilMenu / EnchantLogic). */
    private static List<SolarAnvilJeiRecipe> buildRecipes() {
        List<SolarAnvilJeiRecipe> list = new ArrayList<>();
        ItemStack guardianSoul = new ItemStack(ModItems.GUARDIAN_SOUL.get());
        ItemStack cursedSoul = new ItemStack(ModItems.CURSED_SOUL.get());
        ItemStack godSoul = new ItemStack(ModItems.CELESTIA_GOD_SOUL.get());

        // броня Селестии: незеритовая часть + душа Хранителя богов
        add(list, Items.NETHERITE_CHESTPLATE, guardianSoul, ModItems.CELESTIA_CHESTPLATE.get(), SolarAnvilMenu.ARMOR_UPGRADE_COST);
        add(list, Items.NETHERITE_BOOTS, guardianSoul, ModItems.CELESTIA_BOOTS.get(), SolarAnvilMenu.ARMOR_UPGRADE_COST);

        // предметы проклятых: предмет Селестии + душа проклятого
        add(list, ModItems.CELESTIA_SWORD.get(), cursedSoul, ModItems.CURSED_SWORD.get(), SolarAnvilMenu.CURSED_UPGRADE_COST);
        add(list, ModItems.CELESTIA_AXE.get(), cursedSoul, ModItems.CURSED_AXE.get(), SolarAnvilMenu.CURSED_UPGRADE_COST);
        add(list, ModItems.CELESTIA_PICKAXE.get(), cursedSoul, ModItems.CURSED_PICKAXE.get(), SolarAnvilMenu.CURSED_UPGRADE_COST);
        add(list, ModItems.CELESTIA_SHIELD.get(), cursedSoul, ModItems.CURSED_SHIELD.get(), SolarAnvilMenu.CURSED_UPGRADE_COST);

        // Совершенный Паксель: Паксель Богов и Проклятых + Душа Бога Селестии
        add(list, ModItems.PAXEL.get(), godSoul, ModItems.PERFECT_PAXEL.get(), SolarAnvilMenu.PERFECT_PAXEL_COST);

        // Броня Бога
        add(list, ModItems.CELESTIA_BOOTS.get(), new ItemStack(ModItems.FLIGHT_RING.get()), ModItems.DIVINE_BOOTS.get(), SolarAnvilMenu.DIVINE_BOOTS_COST);
        add(list, ModItems.CELESTIA_CHESTPLATE.get(), godSoul, ModItems.DIVINE_CHESTPLATE.get(), SolarAnvilMenu.DIVINE_CHESTPLATE_COST);

        // Ножницы Селестии: обычные ножницы + улучшенная книга с Гневом Природы
        add(list, Items.SHEARS, new ItemStack(ModItems.ADVANCED_BOOK.get()), ModItems.CELESTIA_SHEARS.get(), 10);
        return list;
    }

    private static void add(List<SolarAnvilJeiRecipe> list, ItemLike tool, ItemStack catalyst, ItemLike result, int cost) {
        list.add(new SolarAnvilJeiRecipe(new ItemStack(tool), catalyst.copy(), new ItemStack(result), cost));
    }
}
