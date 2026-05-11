package com.hughbone.eldenhorses.cap;

import com.hughbone.eldenhorses.EldenHorsesConfig;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@AutoRegisterCapability
public interface PlayerEldenHorseCap {
    Capability<PlayerEldenHorseCap> CAP = CapabilityManager.get(new CapabilityToken<>() {});

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
    };

    final class Impl implements PlayerEldenHorseCap {
        @Nullable private Horse horse;
        private boolean summoned;
        @Nullable private CompoundTag pendingNbt;
        @Nullable private UUID pendingBoundUuid;
        private boolean pendingSummonedState;
        /** Wall-clock millis when summon becomes available again. 0 = no cooldown. */
        private long cooldownEndMs;

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
                Horse h = horse;
                horse = null;
                h.remove(Entity.RemovalReason.DISCARDED);
                h.revive();
                horse = h;
                summoned = false;
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
                summonAtPlayer(self);
                summoned = true;
            }
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

        private void summonAtPlayer(ServerPlayer self) {
            if (horse == null) return;
            Horse h = horse;
            ServerLevel target = (ServerLevel) self.level();
            boolean dimChanged = h.level() != target;

            h.revive();

            if (dimChanged) {
                Entity moved = h.changeDimension(target);
                if (!(moved instanceof Horse mh)) {
                    horse = null;
                    return;
                }
                h = mh;
                horse = mh;
            }

            h.fallDistance = self.fallDistance;
            // Spawn the horse facing the player's actual movement direction
            // rather than yRot. yRot tracks the camera (especially in SSR
            // decoupled mode), which is wrong when the user is e.g. galloping
            // backwards (S key), the horse would spawn facing camera, opposite
            // of where it's moving. Velocity is the only authoritative
            // server-side signal for "where am I going". When standing still,
            // fall back to yRot (no regression on no-movement summon).
            float spawnYaw = computeMovementYaw(self);
            h.moveTo(self.getX(), self.getY(), self.getZ(), spawnYaw, self.getXRot());
            h.setDeltaMovement(self.getDeltaMovement());

            if (!dimChanged) {
                target.tryAddFreshEntityWithPassengers(h);
            }

            self.startRiding(h, true);
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
