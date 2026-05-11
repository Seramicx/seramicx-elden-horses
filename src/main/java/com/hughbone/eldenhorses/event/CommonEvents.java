package com.hughbone.eldenhorses.event;

import com.hughbone.eldenhorses.cap.HorseEldenArmorCap;
import com.hughbone.eldenhorses.cap.PlayerEldenHorseCap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CommonEvents {

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        Player p = e.getEntity();
        if (p.getVehicle() instanceof AbstractHorse h && HorseEldenArmorCap.of(h).hasSpectralSteed()) {
            e.setNewSpeed(e.getOriginalSpeed() * 5.0F);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof AbstractHorse h)) return;
        if (!HorseEldenArmorCap.of(h).hasSpectralSteed()) return;
        if (!e.getSource().is(DamageTypeTags.IS_FALL)) return;
        e.setAmount(Math.max(0f, e.getAmount() - 4f));
    }

    /**
     * Spectral Steed death protection. If the bonded horse would die from this
     * damage, cancel the death and trigger the cooldown unsummon on the
     * owning player's cap. Runs at HIGH priority so we get to inspect the
     * final damage value after any reductions from other mods.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingDamageEvent e) {
        if (e.getEntity().level().isClientSide) return;
        if (!(e.getEntity() instanceof Horse h)) return;
        if (!HorseEldenArmorCap.of(h).hasSpectralSteed()) return;
        if (h.getHealth() - e.getAmount() > 0F) return;

        ServerPlayer owner = findBondedPlayer(h);
        if (owner == null) return;

        e.setAmount(0F);
        h.setHealth(Math.max(1F, h.getHealth()));
        PlayerEldenHorseCap.of(owner).triggerSpectralDeath(owner);
    }

    /**
     * Locate the player who has this horse bound via PlayerEldenHorseCap.
     * Iterates the level's player list. Cheap given the typical small
     * player count vs the cost of a per-horse owner field.
     */
    private static ServerPlayer findBondedPlayer(Horse h) {
        if (!(h.level() instanceof ServerLevel sl)) return null;
        for (ServerPlayer sp : sl.getServer().getPlayerList().getPlayers()) {
            Horse bonded = PlayerEldenHorseCap.of(sp).getHorse();
            if (bonded != null && bonded.getUUID().equals(h.getUUID())) {
                return sp;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent e) {
        if (!e.isMounting()) return;
        if (!(e.getEntityBeingMounted() instanceof Horse horse)) return;
        if (!(e.getEntityMounting() instanceof Player p)) return;

        HorseEldenArmorCap.of(horse).updateFromHorse(horse);
        if (p instanceof ServerPlayer sp && HorseEldenArmorCap.of(horse).hasAny()) {
            PlayerEldenHorseCap.of(sp).bindHorse(sp, horse);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        Player oldP = e.getOriginal();
        Player newP = e.getEntity();
        oldP.reviveCaps();
        oldP.getCapability(PlayerEldenHorseCap.CAP).ifPresent(oldCap ->
                newP.getCapability(PlayerEldenHorseCap.CAP).ifPresent(newCap -> {
                    Horse h = oldCap.getHorse();
                    if (h != null) newCap.setHorse(h);
                })
        );
        oldP.invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        sp.getCapability(PlayerEldenHorseCap.CAP).ifPresent(c ->
                c.rehydrate((ServerLevel) sp.level()));
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        sp.getCapability(PlayerEldenHorseCap.CAP).ifPresent(c ->
                c.rehydrate((ServerLevel) sp.level()));
    }
}
