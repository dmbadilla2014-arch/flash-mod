package com.flashmod;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public final class ModItems {
    public static final RegistryEntry<ArmorMaterial> MATERIAL = Registry.registerReference(
            Registries.ARMOR_MATERIAL, Identifier.of(FlashMod.ID, "flash"),
            new ArmorMaterial(
                    Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 8,
                           ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3, ArmorItem.Type.BODY, 11),
                    15, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.ofItems(Items.RED_WOOL),
                    List.of(new ArmorMaterial.Layer(Identifier.of(FlashMod.ID, "flash"))),
                    1.0f, 0.0f));

    public static final Item COWL = reg("flash_cowl", ArmorItem.Type.HELMET);
    public static final Item CHEST = reg("flash_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final Item LEGS = reg("flash_leggings", ArmorItem.Type.LEGGINGS);
    public static final Item BOOTS = reg("flash_boots", ArmorItem.Type.BOOTS);

    private static Item reg(String name, ArmorItem.Type type) {
        return Registry.register(Registries.ITEM, Identifier.of(FlashMod.ID, name),
                new ArmorItem(MATERIAL, type, new Item.Settings().maxDamage(type.getMaxDamage(30))));
    }

    public static boolean hasSuit(LivingEntity e) {
        return e.getEquippedStack(EquipmentSlot.HEAD).isOf(COWL)
                && e.getEquippedStack(EquipmentSlot.CHEST).isOf(CHEST)
                && e.getEquippedStack(EquipmentSlot.LEGS).isOf(LEGS)
                && e.getEquippedStack(EquipmentSlot.FEET).isOf(BOOTS);
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(COWL); entries.add(CHEST); entries.add(LEGS); entries.add(BOOTS);
        });
    }
}
