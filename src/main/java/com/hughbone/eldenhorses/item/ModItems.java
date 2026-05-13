package com.hughbone.eldenhorses.item;

import com.hughbone.eldenhorses.EldenHorses;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, EldenHorses.MODID);

    public static final RegistryObject<Item> NETHERITE_HORSE_ARMOR = ITEMS.register(
            "netherite_horse_armor",
            () -> new EldenHorseArmor(new Item.Properties().stacksTo(1).fireResistant())
    );

    public static final RegistryObject<Item> HORSE_ARMOR_TOME =
            ModList.get().isLoaded("apotheosis")
                    ? ITEMS.register("horse_armor_tome",
                            () -> com.hughbone.eldenhorses.compat.apotheosis.HorseArmorTomeFactory.create())
                    : null;
}
