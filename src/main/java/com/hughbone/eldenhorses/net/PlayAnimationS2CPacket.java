package com.hughbone.eldenhorses.net;

import com.hughbone.eldenhorses.client.ClientAnimationHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Server-to-client: tell observers (and the player's own client) to play
// a registered PlayerAnimator animation on a specific player.
//
// type byte:
//   0 = "whistle" animation (SUMMONING intro)
//   1 = "mount" animation (SUMMONING end)
//   2 = "dismount" animation (UNSUMMONING midpoint)
public class PlayAnimationS2CPacket {
    public static final byte TYPE_WHISTLE = 0;
    public static final byte TYPE_MOUNT = 1;
    public static final byte TYPE_DISMOUNT = 2;

    private final int playerEntityId;
    private final byte type;

    public PlayAnimationS2CPacket(int playerEntityId, byte type) {
        this.playerEntityId = playerEntityId;
        this.type = type;
    }

    public static void encode(PlayAnimationS2CPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.playerEntityId);
        buf.writeByte(p.type);
    }

    public static PlayAnimationS2CPacket decode(FriendlyByteBuf buf) {
        return new PlayAnimationS2CPacket(buf.readVarInt(), buf.readByte());
    }

    public static void handle(PlayAnimationS2CPacket p, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientAnimationHooks.playPlayerAnimation(p.playerEntityId, p.type)));
        ctx.setPacketHandled(true);
    }
}
