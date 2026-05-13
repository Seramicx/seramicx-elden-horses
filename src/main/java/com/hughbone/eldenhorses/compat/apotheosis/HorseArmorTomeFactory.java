package com.hughbone.eldenhorses.compat.apotheosis;

import com.hughbone.eldenhorses.enchantment.SpectralEnchantmentCategory;
import dev.shadowsoffire.apotheosis.ench.objects.TomeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class HorseArmorTomeFactory {
    private HorseArmorTomeFactory() {}

    public static Item create() {
        return new TomeItem(Items.IRON_HORSE_ARMOR, SpectralEnchantmentCategory.HORSE_ARMOR);
    }
}
