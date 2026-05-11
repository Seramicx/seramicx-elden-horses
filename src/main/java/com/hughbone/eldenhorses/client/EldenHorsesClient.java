package com.hughbone.eldenhorses.client;

import com.hughbone.eldenhorses.EldenHorses;
import com.hughbone.eldenhorses.net.NetworkHandler;
import com.hughbone.eldenhorses.net.SummonHorseC2SPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EldenHorses.MODID, value = Dist.CLIENT)
public class EldenHorsesClient {

    public static final KeyMapping SUMMON_HORSE = new KeyMapping(
            "key.eldenhorses.summonhorse",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "eldenhorses.category");

    private static int cooldownTicks = 0;

    @Mod.EventBusSubscriber(modid = EldenHorses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent e) {
            e.register(SUMMON_HORSE);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (cooldownTicks > 0) cooldownTicks--;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (cooldownTicks == 0 && SUMMON_HORSE.isDown()) {
            try {
                NetworkHandler.INSTANCE.sendToServer(new SummonHorseC2SPacket());
            } catch (IllegalStateException ex) {
                mc.player.displayClientMessage(Component.literal("Failed: Mod Not On Server?"), true);
            }
            cooldownTicks = 20;
        }
    }
}
