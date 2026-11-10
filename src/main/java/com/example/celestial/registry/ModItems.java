package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import com.example.celestial.item.FlightRingItem;
import com.example.celestial.item.CheatPermitItem;
import com.example.celestial.item.CelestiaAxeItem;
import com.example.celestial.item.CelestiaPickShovelItem;
import com.example.celestial.item.CelestiaShearsItem;
import com.example.celestial.item.CelestiaShieldItem;
import com.example.celestial.item.CelestiaSwordItem;
import com.example.celestial.item.CelestiaTier;
import com.example.celestial.item.CursedAxeItem;
import com.example.celestial.item.CursedPickShovelItem;
import com.example.celestial.item.CursedShieldItem;
import com.example.celestial.item.PaxelItem;
import com.example.celestial.item.PerfectPaxelItem;
import com.example.celestial.item.CursedSwordItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem;
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

    /** Объединённая душа богов и проклятых: крафт из Души Хранителя Богов и Души проклятого. */
    public static final DeferredItem<Item> UNITED_SOUL =
            ITEMS.registerSimpleItem("united_soul", new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    /** Всезнающая книга: выпадает из Всезнающего Ангела, ПКМ открывает сюжет. */
    public static final DeferredItem<Item> ALL_KNOWING_TOME = ITEMS.register("all_knowing_tome",
            () -> new com.example.celestial.item.AllKnowingTomeItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    /** Часы Фенрира: ПКМ останавливает / возобновляет течение дня и ночи. */
    public static final DeferredItem<Item> FENRIR_CLOCK = ITEMS.register("fenrir_clock",
            () -> new com.example.celestial.item.FenrirClockItem(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    /** Сердце Цербера: пока лежит в хотбаре или второй руке - Восстановление IV, Сила III и голод не расходуется. */
    public static final DeferredItem<Item> CERBERUS_HEART =
            ITEMS.registerSimpleItem("cerberus_heart", new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    /** Душа Бога Селестии: падает с одноимённого босса, нужна для Совершенного Пакселя. */
    public static final DeferredItem<Item> CELESTIA_GOD_SOUL =
            ITEMS.registerSimpleItem("celestia_god_soul", new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

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

    // ----------------------------------------------------------- Инструменты проклятых
    /** Меч проклятых: 40 урона (1 рука + 39), неразрушим. */
    public static final DeferredItem<CursedSwordItem> CURSED_SWORD = ITEMS.register("cursed_sword",
            () -> new CursedSwordItem(CelestiaTier.SWORD, celestia(39.0, -2.4)));

    /** Топор проклятых: мгновенно срубает дерево целиком (см. CursedEvents), неразрушим. */
    public static final DeferredItem<CursedAxeItem> CURSED_AXE = ITEMS.register("cursed_axe",
            () -> new CursedAxeItem(CelestiaTier.AXE, celestia(23.0, -3.0)));

    /** Кирко-лопата проклятых: мгновенно ломает любой блок, кроме бедрока и деревьев. */
    public static final DeferredItem<CursedPickShovelItem> CURSED_PICKAXE = ITEMS.register("cursed_pickaxe",
            () -> new CursedPickShovelItem(CelestiaTier.PICK, celestia(9.0, -2.8)));

    /** Щит проклятых: возвращает полученный урон атакующему. */
    public static final DeferredItem<CursedShieldItem> CURSED_SHIELD = ITEMS.register("cursed_shield",
            () -> new CursedShieldItem(new Item.Properties()
                    .rarity(Rarity.EPIC).fireResistant().stacksTo(1)
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))));

    /** Паксель Богов и Проклятых: мульти-тул, мгновенно добывает камень, дерево и землю, 50 урона, неразрушим. */
    public static final DeferredItem<PaxelItem> PAXEL = ITEMS.register("paxel",
            () -> new PaxelItem(CelestiaTier.AXE, celestia(49.0, -2.6)));

    /** Совершенный Паксель: как Паксель Богов и Проклятых, но 250 урона (1 рука + 249). Солнечная наковальня: Паксель + Душа Бога Селестии. */
    public static final DeferredItem<PerfectPaxelItem> PERFECT_PAXEL = ITEMS.register("perfect_paxel",
            () -> new PerfectPaxelItem(CelestiaTier.AXE, celestia(249.0, -2.6)));

    /** Любой из пакселей (обычный или совершенный): общее поведение мульти-тула. */
    public static boolean isPaxel(net.minecraft.world.item.ItemStack stack) {
        return stack.is(PAXEL.get()) || stack.is(PERFECT_PAXEL.get());
    }

    public static final DeferredItem<DeferredSpawnEggItem> GUARDIAN_EGG = ITEMS.register("guardian_of_gods_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.GUARDIAN, 0xE0A800, 0xFFF3C4, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ALL_KNOWING_ANGEL_EGG = ITEMS.register("all_knowing_angel_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ALL_KNOWING_ANGEL, 0x2A0A3C, 0xFF3A6A, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CURSED_BOSS_EGG = ITEMS.register("cursed_boss_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CURSED_BOSS, 0x1A0A12, 0xD01824, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> FENRIR_EGG = ITEMS.register("fenrir_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FENRIR, 0x2B0A3D, 0xFF3C5A, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CELESTIA_GOD_EGG = ITEMS.register("celestia_god_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CELESTIA_GOD, 0xF6D77A, 0x8F8BD8, new Item.Properties()));


    public static final DeferredItem<DeferredSpawnEggItem> CURSED_PIG_EGG = ITEMS.register("cursed_pig_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CURSED_PIG, 0x2A0E16, 0xF0708A, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CURSED_COW_EGG = ITEMS.register("cursed_cow_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CURSED_COW, 0x1A0A12, 0xB01824, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CURSED_SHEEP_EGG = ITEMS.register("cursed_sheep_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CURSED_SHEEP, 0x2E1A3A, 0xD01824, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CURSED_CHICKEN_EGG = ITEMS.register("cursed_chicken_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CURSED_CHICKEN, 0x1A0A12, 0xFF8A1E, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CURSED_ZOMBIE_EGG = ITEMS.register("cursed_zombie_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CURSED_ZOMBIE, 0x16302A, 0xD01824, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> CURSED_SKELETON_EGG = ITEMS.register("cursed_skeleton_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CURSED_SKELETON, 0x2A2A32, 0xD01824, new Item.Properties()));

    /** Броня Селестии: по 30 защиты, нимб над игроком. Делается в Солнечной наковальне: незеритовая часть + душа Хранителя богов. */
    public static final DeferredItem<ArmorItem> CELESTIA_CHESTPLATE = ITEMS.register("celestia_chestplate",
            () -> new ArmorItem(ModArmorMaterials.CELESTIA, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<ArmorItem> CELESTIA_BOOTS = ITEMS.register("celestia_boots",
            () -> new ArmorItem(ModArmorMaterials.CELESTIA, ArmorItem.Type.BOOTS,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    /** Ботинки Бога: Селестийские ботинки + Кольцо полёта (Солнечная наковальня). 50 защиты, полёт, +1 строка здоровья. */
    public static final DeferredItem<ArmorItem> DIVINE_BOOTS = ITEMS.register("divine_boots",
            () -> new ArmorItem(ModArmorMaterials.DIVINE, ArmorItem.Type.BOOTS,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Нагрудник Бога: Селестийский нагрудник + Душа Бога Селестии (Солнечная наковальня). 100 защиты, +2 строки здоровья, Сопротивление V, аура. */
    public static final DeferredItem<ArmorItem> DIVINE_CHESTPLATE = ITEMS.register("divine_chestplate",
            () -> new ArmorItem(ModArmorMaterials.DIVINE, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                            .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** Кольцо полёта богов: падает с Бога Селестии, даёт полёт, пока лежит в инвентаре. */
    public static final DeferredItem<Item> FLIGHT_RING = ITEMS.register("ring_of_gods_flight",
            () -> new FlightRingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** «Официальное разрешение на читы»: падает с Бога Селестии, ничего не делает. */
    public static final DeferredItem<Item> CHEAT_PERMIT = ITEMS.register("official_cheat_permit",
            () -> new CheatPermitItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

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
