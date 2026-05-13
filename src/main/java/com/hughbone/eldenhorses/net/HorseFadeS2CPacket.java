package com.hughbone.eldenhorses.net;

import com.hughbone.eldenhorses.client.ClientAnimationHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HorseFadeS2CPacket {
    private final int horseEntityId;
    private final boolean fadeIn;
    private final int durationTicks;

    public HorseFadeS2CPacket(int horseEntityId, boolean fadeIn, int durationTicks) {
        this.horseEntityId = horseEntityId;
        this.fadeIn = fadeIn;
        this.durationTicks = durationTicks;
    }

    public static void encode(HorseFadeS2CPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.horseEntityId);
        buf.writeBoolean(p.fadeIn);
        buf.writeVarInt(p.durationTicks);
    }

    public static HorseFadeS2CPacket decode(FriendlyByteBuf buf) {
        return new HorseFadeS2CPacket(buf.readVarInt(), buf.readBoolean(), buf.readVarInt());
    }

    public static void handle(HorseFadeS2CPacket p, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientAnimationHooks.startHorseFade(p.horseEntityId, p.fadeIn, p.durationTicks)));
        ctx.setPacketHandled(true);
    }
}
