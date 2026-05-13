package com.hughbone.eldenhorses.enchantment;

import com.hughbone.eldenhorses.EldenHorses;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, EldenHorses.MODID);

    public static final RegistryObject<Enchantment> SPECTRAL_STEED = ENCHANTMENTS.register(
            "spectral_steed",
            () -> new SpectralEnchantment(Enchantment.Rarity.RARE)
    );

    public static final RegistryObject<Enchantment> SPECTRAL_LEAP = ENCHANTMENTS.register(
            "spectral_leap",
            () -> new SpectralEnchantment(Enchantment.Rarity.RARE)
    );

    private ModEnchantments() {}

    private static final class SpectralEnchantment extends Enchantment {
        SpectralEnchantment(Rarity rarity) {
            super(rarity, SpectralEnchantmentCategory.HORSE_ARMOR, new EquipmentSlot[0]);
        }

        @Override public int getMaxLevel() { return 1; }
        @Override public int getMinCost(int level) { return 15; }
        @Override public int getMaxCost(int level) { return 50; }
    }
}
