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
    boolean isOnCooldown();
    long getCooldownRemainingMs();
    void triggerSpectralDeath(ServerPlayer self);

    AnimationPhase getAnimationPhase();
    int getAnimationPhaseTick();
    void tickAnimation(ServerPlayer self);
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
        private long cooldownEndMs;

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
            if (phase != AnimationPhase.IDLE) return;

            if (horse == null) {
                if (self.level() instanceof ServerLevel sl) {
                    rehydrate(sl);
                }
            }
            if (horse == null) {
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

            if (horse == null) {
                phase = AnimationPhase.IDLE;
                phaseTick = 0;
                return;
            }
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
                level.playSound(null, self.getX(), self.getY(), self.getZ(),
                        ModSounds.WHISTLE.get(),
                        SoundSource.PLAYERS, 0.6F, 1.0F);
                level.playSound(null, self.getX(), self.getY(), self.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS, 0.45F, 1.3F);
                level.playSound(null, self.getX(), self.getY(), self.getZ(),
                        SoundEvents.NOTE_BLOCK_CHIME.value(),
                        SoundSource.PLAYERS, 0.55F, 0.7F);

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

            if (t < SUMMON_MOUNT_END_TICK) {
                emitGatherParticles(level, self, t, SUMMON_MOUNT_END_TICK);
            }

            if (t == SUMMON_MOUNT_END_TICK) {
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
                horse.setInvulnerable(true);
                NetworkHandler.INSTANCE.send(
                        PacketDistributor.TRACKING_ENTITY.with(() -> horse),
                        new HorseFadeS2CPacket(horse.getId(), false, UNSUMMON_FADE_DURATION));
                level.playSound(null, self.getX(), self.getY(), self.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS, 0.45F, 0.7F);
                level.playSound(null, self.getX(), self.getY(), self.getZ(),
                        SoundEvents.NOTE_BLOCK_CHIME.value(),
                        SoundSource.PLAYERS, 0.55F, 1.5F);
            }

            emitGatherParticles(level, self, t, UNSUMMON_TICKS);

            if (t == UNSUMMON_DISMOUNT_TICK) {
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
            DustColorTransitionOptions stardust = new DustColorTransitionOptions(
                    new Vector3f(0.25f, 0.55f, 1.0f),
                    new Vector3f(0.55f, 0.80f, 1.0f),
                    0.5f);

            double cx = self.getX();
            double cy = self.getY();
            double cz = self.getZ();
            var rand = level.random;

            for (int i = 0; i < 6; i++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double radius = 0.5 + rand.nextDouble() * 1.0;
                double px = cx + Math.cos(angle) * radius;
                double py = cy + 0.2 + rand.nextDouble() * 1.2;
                double pz = cz + Math.sin(angle) * radius;
                level.sendParticles(stardust, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
            }

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
            return (float) (Math.atan2(-v.x, v.z) * (180.0 / Math.PI));
        }

        @Override
        public void rehydrate(ServerLevel level) {
            if (horse != null) return;

            if (pendingBoundUuid != null) {
                if (level.getEntity(pendingBoundUuid) instanceof Horse live) {
                    horse = live;
                    summoned = pendingSummonedState;
                    pendingBoundUuid = null;
                    return;
                }
            }

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
            if (horse != null) {
                root.putUUID("BoundHorseUUID", horse.getUUID());
                if (!summoned) {
                    CompoundTag tag = new CompoundTag();
                    if (horse.saveAsPassenger(tag)) {
                        root.put("Elden_Horse", tag);
                    }
                }
            } else if (pendingBoundUuid != null) {
                root.putUUID("BoundHorseUUID", pendingBoundUuid);
                if (pendingNbt != null && !pendingSummonedState) {
                    root.put("Elden_Horse", pendingNbt);
                    root.putBoolean("Summoned", false);
                }
            } else if (pendingNbt != null && !pendingSummonedState) {
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
            // Forge's reviveCaps doesn't restore LazyOptionals
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
