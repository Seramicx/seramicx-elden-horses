package com.hughbone.eldenhorses.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.HorseArmorItem;

public class EldenHorseArmor extends HorseArmorItem {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("elden_horses", "textures/entity/horse_armor_netherite.png");

    public EldenHorseArmor(Properties props) {
        super(15, "netherite", props);
    }

    @Override
    public ResourceLocation getTexture() {
        return TEXTURE;
    }
}
