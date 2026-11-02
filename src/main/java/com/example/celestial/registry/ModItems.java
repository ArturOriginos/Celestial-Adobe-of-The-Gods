package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.item.CelestiaAxeItem;
import com.example.celestial.item.CelestiaPickShovelItem;
import com.example.celestial.item.CelestiaShearsItem;
import com.example.celestial.item.CelestiaShieldItem;
import com.example.celestial.item.CelestiaSwordItem;
import com.example.celestial.item.CelestiaTier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    /** Благо Гефеста и Афины: на ней хранятся кастомные зачарования. */
    public static final DeferredItem<Item> ADVANCED_BOOK =
            ITEMS.registerSimpleItem("advanced_book", new Item.Properties().stacksTo(16));

    /** Душа Хранителя Богов: основа предметов Селестии. */
    public static final DeferredItem<Item> GUARDIAN_SOUL =
            ITEMS.registerSimpleItem("guardian_soul", new Item.Properties().rarity(Rarity.EPIC).fireResistant());

    /** Душа проклятого: выпадает из босса Проклятый. */
    public static final DeferredItem<Item> CURSED_SOUL =
            ITEMS.registerSimpleItem("cursed_soul", new Item.Properties().rarity(Rarity.EPIC).fireResistant());

    /** Руна Селестии: выпадает из Селестийской руды. */
    public static final DeferredItem<Item> CELESTIA_RUNE =
            ITEMS.registerSimpleItem("celestia_rune", new Item.Properties().rarity(Rarity.RARE));

    /** Ключ Селестии (ключ в обитель богов). */
    public static final DeferredItem<Item> CELESTIA_KEY =
            ITEMS.registerSimpleItem("celestia_key", new Item.Properties().rarity(Rarity.EPIC).stacksTo(1));

    // Урон = 1 (рука) + модификатор. Меч 20, топор 24, кирко-лопата 10.
    public static final DeferredItem<CelestiaSwordItem> CELESTIA_SWORD = ITEMS.register("celestia_sword",
            () -> new CelestiaSwordItem(CelestiaTier.SWORD, celestia(19.0, -2.4)));

    public static final DeferredItem<CelestiaAxeItem> CELESTIA_AXE = ITEMS.register("celestia_axe",
            () -> new CelestiaAxeItem(CelestiaTier.AXE, celestia(23.0, -3.0)));

    public static final DeferredItem<CelestiaPickShovelItem> CELESTIA_PICKAXE = ITEMS.register("celestia_pickaxe",
            () -> new CelestiaPickShovelItem(CelestiaTier.PICK, celestia(9.0, -2.8)));

    public static final DeferredItem<CelestiaShieldItem> CELESTIA_SHIELD = ITEMS.register("celestia_shield",
            () -> new CelestiaShieldItem(new Item.Properties()
                    .rarity(Rarity.EPIC).fireResistant().stacksTo(1)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))));

    /** Ножницы Селестии: бесконечные, 1 урон, 2% шанс мгновенного убийства. */
    public static final DeferredItem<CelestiaShearsItem> CELESTIA_SHEARS = ITEMS.register("celestia_shears",
            () -> new CelestiaShearsItem(new Item.Properties()
                    .rarity(Rarity.EPIC).fireResistant().stacksTo(1)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .component(DataComponents.TOOL, ShearsItem.createToolProperties())));

    public static final DeferredItem<DeferredSpawnEggItem> GUARDIAN_EGG = ITEMS.register("guardian_of_gods_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GUARDIAN, 0xE0A800, 0xFFF3C4, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CURSED_BOSS_EGG = ITEMS.register("cursed_boss_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CURSED_BOSS, 0x1A0A12, 0xD01824, new Item.Properties()));

    private static Item.Properties celestia(double damage, double speed) {
        ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_damage"),
                                damage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(ResourceLocation.withDefaultNamespace("base_attack_speed"),
                                speed, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
        return new Item.Properties()
                .rarity(Rarity.EPIC)
                .fireResistant()
                .stacksTo(1)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true)) // бесконечная прочность
                .attributes(attributes);
    }

    private ModItems() {}
}
