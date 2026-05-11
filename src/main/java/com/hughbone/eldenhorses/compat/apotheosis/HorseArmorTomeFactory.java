package com.hughbone.eldenhorses.compat.apotheosis;

import com.hughbone.eldenhorses.enchantment.SpectralEnchantmentCategory;
import dev.shadowsoffire.apotheosis.ench.objects.TomeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

// Class-loaded only when Apotheosis is present (gated in ModItems). Keeps
// the TomeItem reference out of any always-loaded class so a missing
// Apotheosis at runtime won't trigger NoClassDefFoundError.
public final class HorseArmorTomeFactory {
    private HorseArmorTomeFactory() {}

    public static Item create() {
        // rep = iron horse armor: TomeItem will call ench.canApplyAtEnchantingTable(ironStack)
        // for each candidate, so any enchantment whose category accepts horse armor
        // (our Spectral set, Shiny Horses' Protection/Mending, etc.) passes the filter.
        return new TomeItem(Items.IRON_HORSE_ARMOR, SpectralEnchantmentCategory.HORSE_ARMOR);
    }
}
