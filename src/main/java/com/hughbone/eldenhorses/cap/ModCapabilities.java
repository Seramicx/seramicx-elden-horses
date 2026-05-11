package com.hughbone.eldenhorses.cap;

import com.hughbone.eldenhorses.EldenHorses;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModCapabilities {
    private static final ResourceLocation HORSE_KEY = new ResourceLocation(EldenHorses.MODID, "horse_elden_armor");
    private static final ResourceLocation PLAYER_KEY = new ResourceLocation(EldenHorses.MODID, "player_elden_horse");

    @SubscribeEvent
    public static void attachEntity(AttachCapabilitiesEvent<Entity> event) {
        Entity obj = event.getObject();
        if (obj instanceof AbstractHorse) {
            HorseEldenArmorCap.Provider p = new HorseEldenArmorCap.Provider();
            event.addCapability(HORSE_KEY, p);
            event.addListener(p::invalidate);
        }
        if (obj instanceof Player) {
            PlayerEldenHorseCap.Provider p = new PlayerEldenHorseCap.Provider();
            event.addCapability(PLAYER_KEY, p);
            event.addListener(p::invalidate);
        }
    }
}
