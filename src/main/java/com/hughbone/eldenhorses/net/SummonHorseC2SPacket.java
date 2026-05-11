package com.hughbone.eldenhorses.net;

import com.hughbone.eldenhorses.cap.PlayerEldenHorseCap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SummonHorseC2SPacket {
    public SummonHorseC2SPacket() {}
    public SummonHorseC2SPacket(FriendlyByteBuf buf) {}

    public static void encode(SummonHorseC2SPacket p, FriendlyByteBuf buf) {}
    public static SummonHorseC2SPacket decode(FriendlyByteBuf buf) { return new SummonHorseC2SPacket(buf); }

    public static void handle(SummonHorseC2SPacket p, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            PlayerEldenHorseCap.of(sender).toggleSummon(sender);
        });
        ctx.setPacketHandled(true);
    }
}
