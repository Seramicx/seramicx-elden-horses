package com.hughbone.eldenhorses;

import com.hughbone.eldenhorses.cap.ModCapabilities;
import com.hughbone.eldenhorses.enchantment.ModEnchantments;
import com.hughbone.eldenhorses.event.CommonEvents;
import com.hughbone.eldenhorses.item.ModItems;
import com.hughbone.eldenhorses.loot.SpectralLootModifier;
import com.hughbone.eldenhorses.net.NetworkHandler;
import com.hughbone.eldenhorses.sound.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EldenHorses.MODID)
public class EldenHorses {
    public static final String MODID = "elden_horses";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EldenHorses() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modBus);
        ModEnchantments.ENCHANTMENTS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        SpectralLootModifier.SERIALIZERS.register(modBus);

        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON, EldenHorsesConfig.COMMON_SPEC, "elden_horses-common.toml");
        modBus.register(EldenHorsesConfig.class);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::buildCreativeTab);

        MinecraftForge.EVENT_BUS.register(CommonEvents.class);
        MinecraftForge.EVENT_BUS.register(ModCapabilities.class);
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        NetworkHandler.register();
        LOGGER.info("Elden Horses Initialized.");
    }

    private static final ResourceLocation APOTH_ENCH_TAB =
            new ResourceLocation("apotheosis", "ench");

    private static final ResourceLocation APOTH_BOOTS_TOME =
            new ResourceLocation("apotheosis", "boots_tome");

    private void buildCreativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            e.accept(ModItems.NETHERITE_HORSE_ARMOR.get());
        }
        if (ModItems.HORSE_ARMOR_TOME != null
                && e.getTabKey().location().equals(APOTH_ENCH_TAB)) {
            Item boots = ForgeRegistries.ITEMS.getValue(APOTH_BOOTS_TOME);
            ItemStack ours = new ItemStack(ModItems.HORSE_ARMOR_TOME.get());
            if (boots != null && boots != net.minecraft.world.item.Items.AIR) {
                e.getEntries().putAfter(
                        new ItemStack(boots),
                        ours,
                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                e.accept(ours);
            }
        }
    }
}
