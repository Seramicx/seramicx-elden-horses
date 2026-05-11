package com.hughbone.eldenhorses.enchantment;

import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public final class SpectralEnchantmentCategory {

    public static final EnchantmentCategory HORSE_ARMOR =
            EnchantmentCategory.create("HORSE_ARMOR", item -> item instanceof HorseArmorItem);

    private SpectralEnchantmentCategory() {}
}
