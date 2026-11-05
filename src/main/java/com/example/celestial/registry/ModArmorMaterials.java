package com.example.celestial.registry;

import static com.example.celestial.CelestialMod.MODID;

import java.util.EnumMap;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Материал брони Селестии: по 30 защиты на каждую часть. Текстуры: textures/models/armor/celestia_layer_1.png. */
public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, MODID);

    public static final int DEFENSE_PER_PIECE = 30;

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CELESTIA = MATERIALS.register("celestia",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, DEFENSE_PER_PIECE);
                        map.put(ArmorItem.Type.LEGGINGS, DEFENSE_PER_PIECE);
                        map.put(ArmorItem.Type.CHESTPLATE, DEFENSE_PER_PIECE);
                        map.put(ArmorItem.Type.HELMET, DEFENSE_PER_PIECE);
                        map.put(ArmorItem.Type.BODY, DEFENSE_PER_PIECE);
                    }),
                    25,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(ModItems.GUARDIAN_SOUL.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MODID, "celestia"))),
                    4.0F,
                    0.1F));

    private ModArmorMaterials() {}
}
