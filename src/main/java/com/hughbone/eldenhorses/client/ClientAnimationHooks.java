package com.hughbone.eldenhorses.client;

import com.hughbone.eldenhorses.EldenHorses;
import com.hughbone.eldenhorses.net.PlayAnimationS2CPacket;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// Client-only entry point for the summon/unsummon animation system.
//
// Responsibilities:
//   1. Register a per-player ModifierLayer with PlayerAnimator so we have
//      somewhere to push the whistle animation.
//   2. Handle PlayAnimationS2CPacket: resolve target player, push whistle
//      animation onto their layer.
//   3. Maintain a client-side fade map for horse entities; render mixin
//      queries this map for the current alpha.
@Mod.EventBusSubscriber(modid = EldenHorses.MODID, value = Dist.CLIENT)
public final class ClientAnimationHooks {

    private ClientAnimationHooks() {}

    // Two separate animation layers so whistle (arms + head) and mount /
    // dismount (legs + body) can stack instead of replacing each other.
    private static final ResourceLocation ARMS_LAYER_ID =
            new ResourceLocation(EldenHorses.MODID, "summon_arms_layer");
    private static final ResourceLocation BODY_LAYER_ID =
            new ResourceLocation(EldenHorses.MODID, "summon_body_layer");
    private static final ResourceLocation ANIM_WHISTLE =
            new ResourceLocation(EldenHorses.MODID, "whistle");
    private static final ResourceLocation ANIM_MOUNT =
            new ResourceLocation(EldenHorses.MODID, "mount");
    private static final ResourceLocation ANIM_DISMOUNT =
            new ResourceLocation(EldenHorses.MODID, "dismount");

    @Mod.EventBusSubscriber(modid = EldenHorses.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent e) {
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    ARMS_LAYER_ID,
                    1000,
                    player -> new ModifierLayer<IAnimation>());
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    BODY_LAYER_ID,
                    1001,
                    player -> new ModifierLayer<IAnimation>());
        }
    }

    public static void playPlayerAnimation(int entityId, byte type) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity ent = mc.level.getEntity(entityId);
        if (!(ent instanceof AbstractClientPlayer player)) return;

        ResourceLocation layerKey;
        ResourceLocation animKey;
        switch (type) {
            case PlayAnimationS2CPacket.TYPE_WHISTLE -> {
                layerKey = ARMS_LAYER_ID;
                animKey = ANIM_WHISTLE;
            }
            case PlayAnimationS2CPacket.TYPE_MOUNT -> {
                layerKey = BODY_LAYER_ID;
                animKey = ANIM_MOUNT;
                // Kick the vertical render offset alongside the pose anim.
                startMountAnim(entityId);
            }
            case PlayAnimationS2CPacket.TYPE_DISMOUNT -> {
                layerKey = BODY_LAYER_ID;
                animKey = ANIM_DISMOUNT;
            }
            default -> { return; }
        }

        IAnimation existing = PlayerAnimationAccess
                .getPlayerAssociatedData(player)
                .get(layerKey);
        if (!(existing instanceof ModifierLayer<?> rawLayer)) return;
        @SuppressWarnings("unchecked")
        ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) rawLayer;

        KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(animKey);
        if (anim == null) return;
        // Crossfade in over 3 ticks so poses don't snap when transitioning.
        layer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(3, dev.kosmx.playerAnim.core.util.Ease.INOUTSINE),
                new KeyframeAnimationPlayer(anim));
    }

    // -------- Horse fade tracking (client-side only) -------- //

    private record FadeState(long startTick, int duration, boolean fadeIn) {}

    // Keyed by client entity id. Cleared either when a fade completes or
    // via the periodic sweep below.
    private static final Map<Integer, FadeState> FADES = new HashMap<>();

    public static void startHorseFade(int entityId, boolean fadeIn, int duration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        FADES.put(entityId, new FadeState(mc.level.getGameTime(), duration, fadeIn));
    }

    // Returns 1.0 (fully opaque) when not in a fade. Returns the in-progress
    // alpha otherwise; clamps to 0.0 / 1.0 at the endpoints.
    public static float getFadeAlpha(int entityId) {
        FadeState s = FADES.get(entityId);
        if (s == null) return 1.0f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 1.0f;
        long elapsed = mc.level.getGameTime() - s.startTick;
        if (elapsed <= 0) return s.fadeIn ? 0.0f : 1.0f;
        if (elapsed >= s.duration) {
            // Fade-in done -> remove (entity is fully opaque now).
            // Fade-out done -> keep at alpha 0; entity will be removed by
            // server packet shortly and the periodic sweep handles cleanup.
            if (s.fadeIn) FADES.remove(entityId);
            return s.fadeIn ? 1.0f : 0.0f;
        }
        float t = (float) elapsed / (float) s.duration;
        return s.fadeIn ? t : 1.0f - t;
    }

    public static boolean isFading(int entityId) {
        return FADES.containsKey(entityId);
    }

    // -------- Player mount-animation vertical offset -------- //
    //
    // Triggered alongside the mount PlayerAnimator pose. The mounted player
    // is rendered with a downward Y offset that interpolates from
    // -1.5 blocks (standing next to the horse on the ground) to 0
    // (settled in the saddle) across MOUNT_ANIM_DURATION ticks. This is the
    // only way to get a true "hop onto horse" visual; PlayerAnimator's body
    // translation only moves one body part's pivot, which produces a
    // mole-from-underground effect instead.

    private static final Map<Integer, Long> MOUNT_ANIM_STARTS = new HashMap<>();
    private static final int MOUNT_ANIM_DURATION = 12;
    private static final float MOUNT_ANIM_START_OFFSET = 1.5f;

    public static void startMountAnim(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        MOUNT_ANIM_STARTS.put(entityId, mc.level.getGameTime());
    }

    public static float getMountAnimOffset(int entityId, float partialTick) {
        Long start = MOUNT_ANIM_STARTS.get(entityId);
        if (start == null) return 0f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0f;
        float elapsed = (mc.level.getGameTime() - start) + partialTick;
        if (elapsed < 0) return MOUNT_ANIM_START_OFFSET;
        if (elapsed >= MOUNT_ANIM_DURATION) {
            MOUNT_ANIM_STARTS.remove(entityId);
            return 0f;
        }
        float t = elapsed / MOUNT_ANIM_DURATION;
        // Jump arc with overshoot. Returned value is the amount to translate
        // the player DOWN from their server position (saddle):
        //   +1.5 = on ground
        //    0   = exactly at saddle
        //   -0.2 = briefly above saddle (peak of jump)
        //
        // Phase 1 (0..0.5): rapid ease-out rise from +1.5 to -0.2 overshoot.
        // Phase 2 (0.5..1.0): smoothstep settle from -0.2 back to 0.
        if (t < 0.5f) {
            float p = t / 0.5f;
            float eased = 1f - (1f - p) * (1f - p);
            return MOUNT_ANIM_START_OFFSET + (-0.2f - MOUNT_ANIM_START_OFFSET) * eased;
        }
        float p = (t - 0.5f) / 0.5f;
        float eased = p * p * (3f - 2f * p);
        return -0.2f + 0.2f * eased;
    }

    // Sweep stale entries every 5s. Catches the case where a fade-out
    // completes but the entity was already removed (or never tracked
    // by us again). Prevents the map from growing unbounded.
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            FADES.clear();
            return;
        }
        if (mc.level.getGameTime() % 100 != 0) return;
        long now = mc.level.getGameTime();
        Iterator<Map.Entry<Integer, FadeState>> it = FADES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, FadeState> entry = it.next();
            FadeState s = entry.getValue();
            // Anything older than its duration plus a generous 20-tick grace
            // is safe to drop. Entity-removed-mid-fade also lands here.
            if (now - s.startTick > s.duration + 20) {
                it.remove();
                continue;
            }
            if (mc.level.getEntity(entry.getKey()) == null) {
                it.remove();
            }
        }
    }
}
