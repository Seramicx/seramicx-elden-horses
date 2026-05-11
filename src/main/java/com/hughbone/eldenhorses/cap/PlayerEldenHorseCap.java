package com.hughbone.eldenhorses.cap;

import com.hughbone.eldenhorses.EldenHorsesConfig;
import com.hughbone.eldenhorses.net.HorseFadeS2CPacket;
import com.hughbone.eldenhorses.net.NetworkHandler;
import com.hughbone.eldenhorses.net.PlayAnimationS2CPacket;
import com.hughbone.eldenhorses.sound.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@AutoRegisterCapability
public interface PlayerEldenHorseCap {
    Capability<PlayerEldenHorseCap> CAP = CapabilityManager.get(new CapabilityToken<>() {});

    enum AnimationPhase { IDLE, SUMMONING, UNSUMMONING }

    int SUMMON_TICKS = 40;
    // Everything fires at tick 0: whistle sound, whistle animation (arms
    // layer), mount animation (body layer), horse spawn (invisible),
    // startRiding, fade-in. The mount animation also drives a render-time
    // vertical offset on the player so they visually start on the ground
    // beside the horse and rise into the saddle as the horse fades in.
    int SUMMON_MOUNT_END_TICK = 12;
    int SUMMON_FADE_DURATION = 32;

    int UNSUMMON_TICKS = 24;
    int UNSUMMON_FADE_DURATION = 12;
    int UNSUMMON_DISMOUNT_TICK = UNSUMMON_FADE_DURATION;

    @Nullable Horse getHorse();
    boolean isSummoned();
    void setHorse(@Nullable Horse horse);
    void bindHorse(ServerPlayer self, @Nullable Horse newHorse);
    void toggleSummon(ServerPlayer self);
    void rehydrate(ServerLevel level);
    /** True while the horse is recovering from a Spectral Steed death. */
    boolean isOnCooldown();
    /** Milliseconds until the cooldown expires (0 if not on cooldown). */
    long getCooldownRemainingMs();
    /**
     * Spectral Steed death-protection trigger: auto-unsummon the bonded horse
     * (don't actually kill it) and start the cooldown timer from config.
     */
    void triggerSpectralDeath(ServerPlayer self);

    /** Current animation phase. IDLE means no animation in progress. */
    AnimationPhase getAnimationPhase();

    /** Tick index within the current phase (0..N). 0 when IDLE. */
    int getAnimationPhaseTick();

    /**
     * Called every server tick by CommonEvents. Advances any in-progress
     * summon or unsummon animation. No-op when phase is IDLE.
     */
    void tickAnimation(ServerPlayer self);

    /**
     * Force the animation back to IDLE without firing remaining side effects.
     * Used on dimension change, logout cleanup, etc.
     */
    void cancelAnimation();

    static PlayerEldenHorseCap of(ServerPlayer p) {
        return p.getCapability(CAP).orElse(EMPTY);
    }

    PlayerEldenHorseCap EMPTY = new PlayerEldenHorseCap() {
        @Override @Nullable public Horse getHorse() { return null; }
        @Override public boolean isSummoned() { return false; }
        @Override public void setHorse(@Nullable Horse h) {}
        @Override public void bindHorse(ServerPlayer self, @Nullable Horse h) {}
        @Override public void toggleSummon(ServerPlayer self) {}
        @Override public void rehydrate(ServerLevel level) {}
        @Override public boolean isOnCooldown() { return false; }
        @Override public long getCooldownRemainingMs() { return 0L; }
        @Override public void triggerSpectralDeath(ServerPlayer self) {}
        @Override public AnimationPhase getAnimationPhase() { return AnimationPhase.IDLE; }
        @Override public int getAnimationPhaseTick() { return 0; }
        @Override public void tickAnimation(ServerPlayer self) {}
        @Override public void cancelAnimation() {}
    };

    final class Impl implements PlayerEldenHorseCap {
        @Nullable private Horse horse;
        private boolean summoned;
        @Nullable private CompoundTag pendingNbt;
        @Nullable private UUID pendingBoundUuid;
        private boolean pendingSummonedState;
        /** Wall-clock millis when summon becomes available again. 0 = no cooldown. */
        private long cooldownEndMs;

        // Animation phase. Transient (not persisted). Logging out mid-anim
        // resets to IDLE on next login; the cooldown / bound horse state is
        // saved normally and works fine.
        private AnimationPhase phase = AnimationPhase.IDLE;
        private int phaseTick = 0;
        @Override public AnimationPhase getAnimationPhase() { return phase; }
        @Override public int getAnimationPhaseTick() { return phaseTick; }

        @Override @Nullable public Horse getHorse() { return horse; }
        @Override public boolean isSummoned() { return summoned; }
        @Override public void setHorse(@Nullable Horse h) { this.horse = h; this.summoned = (h != null); }

        @Override public boolean isOnCooldown() {
            return cooldownEndMs > System.currentTimeMillis();
        }
        @Override public long getCooldownRemainingMs() {
            long remain = cooldownEndMs - System.currentTimeMillis();
            return remain > 0 ? remain : 0L;
        }

        @Override
        public void triggerSpectralDeath(ServerPlayer self) {
            if (horse != null && !horse.isRemoved()) {
                Horse h = horse;
                horse = null;
                h.remove(Entity.RemovalReason.DISCARDED);
                h.revive();
                horse = h;
            }
            summoned = false;
            cooldownEndMs = System.currentTimeMillis()
                    + EldenHorsesConfig.spectralSteedDeathCooldownSeconds * 1000L;
            self.displayClientMessage(
                    Component.literal("Your spectral steed has fallen. Recovering..."), true);
        }

        @Override
        public void bindHorse(ServerPlayer self, @Nullable Horse newHorse) {
            if (newHorse == null) { horse = null; summoned = false; return; }
            if (horse != null && horse.getUUID().equals(newHorse.getUUID())) {
                summoned = true;
                return;
            }
            horse = newHorse;
            summoned = true;
        }

        @Override
        public void toggleSummon(ServerPlayer self) {
            // Re-press while an animation is mid-flight: ignore. Prevents
            // mid-cycle teardown and weird half-states.
            if (phase != AnimationPhase.IDLE) return;

            if (horse == null) {
                // Retry rehydrate in case the horse's chunk wasn't loaded
                // at login time but has loaded since.
                if (self.level() instanceof ServerLevel sl) {
                    rehydrate(sl);
                }
            }
            if (horse == null) {
                // Last resort: bind from the current vehicle (covers the
                // edge case where vanilla re-mounted the player on load
                // but Forge's EntityMountEvent didn't fire to trigger
                // onMount).
                if (self.getVehicle() instanceof Horse vehicle) {
                    com.hughbone.eldenhorses.cap.HorseEldenArmorCap hcap =
                            com.hughbone.eldenhorses.cap.HorseEldenArmorCap.of(vehicle);
                    hcap.updateFromHorse(vehicle);
                    if (hcap.hasSpectralSteed()) {
                        horse = vehicle;
                        summoned = true;
                    }
                }
                if (horse == null) {
                    self.displayClientMessage(Component.literal("No Horse Found!"), true);
                    return;
                }
            }

            // Re-read the bound horse's armor in case the player took off /
            // removed the enchanted armor since binding. Without this, a
            // bound horse stays summon-able forever even after losing the
            // enchant.
            com.hughbone.eldenhorses.cap.HorseEldenArmorCap horseCap =
                    com.hughbone.eldenhorses.cap.HorseEldenArmorCap.of(horse);
            horseCap.updateFromHorse(horse);
            if (!horseCap.hasSpectralSteed()) {
                self.displayClientMessage(
                        Component.literal("Your horse no longer has Spectral Steed."), true);
                return;
            }

            if (summoned) {
                if (horse.isRemoved()) {
                    horse = null;
                    summoned = false;
                    self.displayClientMessage(Component.literal("No Horse Found!"), true);
                    return;
                }
                phase = AnimationPhase.UNSUMMONING;
                phaseTick = 0;
            } else {
                if (isOnCooldown()) {
                    long secs = (getCooldownRemainingMs() + 999L) / 1000L;
                    self.displayClientMessage(
                            Component.literal("Spectral steed recovering: " + secs + "s"), true);
                    return;
                }
                if (!horse.isAlive()) {
                    horse = null;
                    self.displayClientMessage(Component.literal("No Horse Found!"), true);
                    return;
                }
                phase = AnimationPhase.SUMMONING;
                phaseTick = 0;
            }
        }

        @Override
        public void cancelAnimation() {
            phase = AnimationPhase.IDLE;
            phaseTick = 0;
        }

        @Override
        public void tickAnimation(ServerPlayer self) {
            if (phase == AnimationPhase.IDLE) return;

            // Bail on any pathological state: missing horse, dead horse,
            // dimension mismatch (only relevant after the horse has been
            // placed in-world during a SUMMONING phase, or for an active
            // UNSUMMONING). Iframes drop with the cancel.
            if (horse == null) {
                phase = AnimationPhase.IDLE;
                phaseTick = 0;
                return;
            }
            // Horse exists in-world from tick 0 for both phases (summon
            // spawns it co-located with the player at the start; unsummon
            // begins with the horse already present).
            if (horse.isRemoved() || !horse.isAlive()
                    || horse.level() != self.level()) {
                phase = AnimationPhase.IDLE;
                phaseTick = 0;
                return;
            }

            int t = phaseTick;
            if (phase == AnimationPhase.SUMMONING) {
                tickSummonPhase(self, t);
            } else {
                tickUnsummonPhase(self, t);
            }
            phaseTick++;
        }

        private void tickSummonPhase(ServerPlayer self, int t) {
            ServerLevel level = (ServerLevel) self.level();

            if (t == 0) {
                // Custom whistle ogg shipped at assets/elden_horses/sounds/whistle.ogg.
                // Played from the player so it follows them if they walk
                // during the whistle window.
                level.playSound(null, self.getX(), self.getY(), self.getZ(),
                        ModSounds.WHISTLE.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                // Materialize layer: subtle beacon hum + amethyst chime
                // under the whistle as the horse begins to manifest.
                level.playSound(null, self.getX(), self.getY(), self.getZ(),
                        SoundEvents.BEACON_ACTIVATE,
                        SoundSource.AMBIENT, 0.45F, 1.6F);
                level.playSound(null, self.getX(), self.getY(), self.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.BLOCKS, 0.6F, 1.7F);

                NetworkHandler.INSTANCE.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> self),
                        new PlayAnimationS2CPacket(self.getId(), PlayAnimationS2CPacket.TYPE_WHISTLE));

                placeHorseForFadeIn(self, level);
                self.startRiding(horse, true);
                summoned = true;
                NetworkHandler.INSTANCE.send(
                        PacketDistributor.TRACKING_ENTITY.with(() -> horse),
                        new HorseFadeS2CPacket(horse.getId(), true, SUMMON_FADE_DURATION));
                NetworkHandler.INSTANCE.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> self),
                        new PlayAnimationS2CPacket(self.getId(), PlayAnimationS2CPacket.TYPE_MOUNT));
            }

            // Particles emit during the mount rise (matches the window the
            // player visibly transitions from ground onto horse).
            if (t < SUMMON_MOUNT_END_TICK) {
                emitGatherParticles(level, self, t, SUMMON_MOUNT_END_TICK);
            }

            if (t == SUMMON_MOUNT_END_TICK) {
                // Mount-rise complete. Re-enable horse so it responds to
                // gravity / steering. The horse is still partially
                // translucent (fade continues for another 20 ticks) but
                // rideable.
                horse.setNoAi(false);
                horse.setNoGravity(false);
                horse.setInvulnerable(false);
            }

            if (t >= SUMMON_TICKS - 1) {
                phase = AnimationPhase.IDLE;
                phaseTick = 0;
            }
        }

        private void tickUnsummonPhase(ServerPlayer self, int t) {
            ServerLevel level = (ServerLevel) self.level();

            if (t == 0) {
                // Start the fade-out NOW but keep the player mounted. The
                // horse stays alive and steerable while it visually
                // dissolves underneath, so if you press unsummon while
                // galloping forward you keep galloping for the first half
                // and the velocity gets transferred to you on dismount.
                // Invulnerable so a passing mob can't kill the fading
                // horse and abort the dismount mid-animation; AI stays on
                // so steering still works.
                horse.setInvulnerable(true);
                NetworkHandler.INSTANCE.send(
                        PacketDistributor.TRACKING_ENTITY.with(() -> horse),
                        new HorseFadeS2CPacket(horse.getId(), false, UNSUMMON_FADE_DURATION));
                // Dissolve audio: beacon-deactivate hum + descending
                // amethyst chime. Inverse-ish of the summon manifest.
                level.playSound(null, self.getX(), self.getY(), self.getZ(),
                        SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.AMBIENT, 0.5F, 1.3F);
                level.playSound(null, self.getX(), self.getY(), self.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.BLOCKS, 0.55F, 0.85F);
            }

            // Particles for the whole window. Player position works as
            // the center pre-dismount (player on horse, co-located) and
            // post-dismount (still close to where horse was).
            emitGatherParticles(level, self, t, UNSUMMON_TICKS);

            if (t == UNSUMMON_DISMOUNT_TICK) {
                // Capture momentum from the (now-invisible) horse, dismount,
                // transfer velocity to the player so they slide forward
                // instead of dropping in place. Disable horse AI/physics
                // for the remaining ticks until removal.
                Vec3 vel = horse.getDeltaMovement();
                self.stopRiding();
                self.setDeltaMovement(vel.x, vel.y, vel.z);
                self.hurtMarked = true;
                horse.setNoAi(true);
                horse.setNoGravity(true);
                horse.setInvulnerable(true);
                NetworkHandler.INSTANCE.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> self),
                        new PlayAnimationS2CPacket(self.getId(), PlayAnimationS2CPacket.TYPE_DISMOUNT));
            }

            if (t >= UNSUMMON_TICKS - 1) {
                // End of animation: existing teardown. Removes the entity
                // from the world but keeps the cap's horse reference alive
                // (via revive) so subsequent summons work.
                Horse h = horse;
                horse = null;
                h.remove(Entity.RemovalReason.DISCARDED);
                h.revive();
                horse = h;
                summoned = false;
                phase = AnimationPhase.IDLE;
                phaseTick = 0;
            }
        }

        private void placeHorseForFadeIn(ServerPlayer self, ServerLevel target) {
            Horse h = horse;
            boolean dimChanged = h.level() != target;

            h.revive();

            if (dimChanged) {
                Entity moved = h.changeDimension(target);
                if (!(moved instanceof Horse mh)) {
                    // Couldn't migrate. Drop the animation, leave state
                    // consistent.
                    horse = null;
                    phase = AnimationPhase.IDLE;
                    return;
                }
                h = mh;
                horse = mh;
            }

            h.fallDistance = self.fallDistance;
            float spawnYaw = computeMovementYaw(self);
            h.moveTo(self.getX(), self.getY(), self.getZ(), spawnYaw, self.getXRot());
            h.setDeltaMovement(self.getDeltaMovement());
            h.setNoAi(true);
            h.setNoGravity(true);
            h.setInvulnerable(true);

            if (!dimChanged) {
                target.tryAddFreshEntityWithPassengers(h);
            }
        }

        private static void emitGatherParticles(ServerLevel level, ServerPlayer self,
                                                int tick, int totalTicks) {
            // Small saturated cyan stardust around the horse body region,
            // kept below eye height so it doesn't blind the player in 1st
            // person. END_ROD / FIREWORK removed (their textures are huge
            // and unscalable). Soul-fire-flame retained at one per tick
            // for the deep blue base near the ground.
            //
            // Dust size 0.5 is roughly half the default 1.0 particle scale.
            DustColorTransitionOptions stardust = new DustColorTransitionOptions(
                    new Vector3f(0.25f, 0.55f, 1.0f),
                    new Vector3f(0.55f, 0.80f, 1.0f),
                    0.5f);

            double cx = self.getX();
            double cy = self.getY();
            double cz = self.getZ();
            var rand = level.random;

            // Cyan dust scattered around the horse's body height. py is
            // capped at cy + 1.4 so particles stay below the player's
            // 1st-person camera (eye height ~cy + 1.62).
            for (int i = 0; i < 6; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double radius = 0.5 + rand.nextDouble() * 1.0;
                double px = cx + Math.cos(angle) * radius;
                double py = cy + 0.2 + rand.nextDouble() * 1.2;
                double pz = cz + Math.sin(angle) * radius;
                level.sendParticles(stardust, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
            }

            // One soul-fire ember per tick low to the ground (deep blue
            // base color, sparse enough not to clutter).
            double angle = rand.nextDouble() * Math.PI * 2;
            double radius = 0.5 + rand.nextDouble() * 0.6;
            double px = cx + Math.cos(angle) * radius;
            double py = cy + rand.nextDouble() * 0.4;
            double pz = cz + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    px, py, pz, 0,
                    (rand.nextDouble() - 0.5) * 0.03,
                    0.02 + rand.nextDouble() * 0.02,
                    (rand.nextDouble() - 0.5) * 0.03,
                    0.04);
        }

        private static float computeMovementYaw(ServerPlayer self) {
            Vec3 v = self.getDeltaMovement();
            if (v.x * v.x + v.z * v.z < 1.0E-4) {
                return self.getYRot();
            }
            // MC look vector for yaw y is (-sin(y), _, cos(y)); inverting that
            // for a velocity (vx, vz) gives yaw = atan2(-vx, vz).
            return (float) (Math.atan2(-v.x, v.z) * (180.0 / Math.PI));
        }

        @Override
        public void rehydrate(ServerLevel level) {
            if (horse != null) return;

            // Prefer live-entity lookup by UUID. Covers the common case
            // where the player logged out with the horse summoned and
            // wandering: vanilla persisted the horse entity, we just need
            // to reconnect our reference. Cheaper than recreating from NBT
            // and keeps the original entity (its position, AI state, etc).
            if (pendingBoundUuid != null) {
                if (level.getEntity(pendingBoundUuid) instanceof Horse live) {
                    horse = live;
                    summoned = pendingSummonedState;
                    pendingBoundUuid = null;
                    return;
                }
                // Entity not loaded right now (chunk unloaded). Keep the
                // UUID so toNbt re-saves it; rehydrate may succeed later.
            }

            // Fallback: recreate the horse from saved NBT. Only used when
            // the player logged out with horse unsummoned (no live entity
            // in the world).
            if (pendingNbt == null) return;
            CompoundTag t = pendingNbt;
            pendingNbt = null;
            EntityType.create(t, level).ifPresent(e -> {
                if (e instanceof Horse h) {
                    horse = h;
                    summoned = false;
                }
            });
        }

        CompoundTag toNbt() {
            CompoundTag root = new CompoundTag();
            root.putBoolean("Summoned", summoned);
            if (cooldownEndMs > 0L) {
                root.putLong("CooldownEndMs", cooldownEndMs);
            }
            // Always persist the bound horse's UUID so we can reconnect on
            // load even when the horse stays as a live entity in the world.
            if (horse != null) {
                root.putUUID("BoundHorseUUID", horse.getUUID());
                if (!summoned) {
                    CompoundTag tag = new CompoundTag();
                    if (horse.saveAsPassenger(tag)) {
                        root.put("Elden_Horse", tag);
                    }
                }
            } else if (pendingBoundUuid != null) {
                // Player logged in but rehydrate hasn't found the entity
                // yet (chunk likely unloaded). Round-trip the UUID.
                root.putUUID("BoundHorseUUID", pendingBoundUuid);
                if (pendingNbt != null && !pendingSummonedState) {
                    root.put("Elden_Horse", pendingNbt);
                    root.putBoolean("Summoned", false);
                }
            } else if (pendingNbt != null && !pendingSummonedState) {
                // Pre-UUID save format. Preserve as-is for back-compat.
                root.put("Elden_Horse", pendingNbt);
                root.putBoolean("Summoned", false);
            }
            return root;
        }

        void fromNbt(CompoundTag root) {
            pendingSummonedState = root.getBoolean("Summoned");
            cooldownEndMs = root.getLong("CooldownEndMs");
            if (root.hasUUID("BoundHorseUUID")) {
                pendingBoundUuid = root.getUUID("BoundHorseUUID");
            }
            if (root.contains("Elden_Horse", Tag.TAG_COMPOUND) && !pendingSummonedState) {
                pendingNbt = root.getCompound("Elden_Horse");
            }
        }
    }

    final class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final Impl impl = new Impl();
        private LazyOptional<PlayerEldenHorseCap> opt = LazyOptional.of(() -> impl);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap != CAP) return LazyOptional.empty();
            // Re-create the LazyOptional if it was invalidated. Forge's
            // reviveCaps only flips a validity flag and does not restore
            // the underlying LazyOptionals; we have to do that ourselves.
            if (!opt.isPresent()) {
                opt = LazyOptional.of(() -> impl);
            }
            return opt.cast();
        }
        @Override public CompoundTag serializeNBT() { return impl.toNbt(); }
        @Override public void deserializeNBT(CompoundTag t) { impl.fromNbt(t); }
        public void invalidate() { opt.invalidate(); }
    }
}
