package com.hughbone.eldenhorses.net;

import com.hughbone.eldenhorses.EldenHorses;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EldenHorses.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(
                id++,
                SummonHorseC2SPacket.class,
                SummonHorseC2SPacket::encode,
                SummonHorseC2SPacket::decode,
                SummonHorseC2SPacket::handle);
    }
}
