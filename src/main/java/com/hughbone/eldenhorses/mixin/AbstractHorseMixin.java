package com.hughbone.eldenhorses.mixin;

import com.hughbone.eldenhorses.cap.HorseEldenArmorCap;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {

    @Shadow protected float playerJumpPendingScale;

    private boolean elden_doubleJumped = false;

    @Inject(
            method = "tickRidden(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"))
    private void elden_tickRidden(Player rider, Vec3 input, CallbackInfo ci) {
        AbstractHorse self = (AbstractHorse) (Object) this;
        if (!HorseEldenArmorCap.of(self).hasSpectralLeap()) return;

        if (self.onGround()) {
            elden_doubleJumped = false;
            return;
        }

        if (!elden_doubleJumped && playerJumpPendingScale > 0.0F) {
            Vec3 v = self.getDeltaMovement();
            self.setDeltaMovement(v.x, self.getCustomJump(), v.z);
            float yawRad = self.getYRot() * 0.017453292F;
            float h = Mth.sin(yawRad);
            float i = Mth.cos(yawRad);
            self.setDeltaMovement(self.getDeltaMovement()
                    .add(-0.4F * h * playerJumpPendingScale, 0.0, 0.4F * i * playerJumpPendingScale));
            elden_doubleJumped = true;
            emitSpectralLeapEffect(self);
        }
    }

    private static void emitSpectralLeapEffect(AbstractHorse horse) {
        Level level = horse.level();
        if (!level.isClientSide()) return;

        double cx = horse.getX();
        double cy = horse.getY();
        double cz = horse.getZ();
        RandomSource rand = horse.getRandom();

        DustColorTransitionOptions stardust = new DustColorTransitionOptions(
                new Vector3f(0.25f, 0.55f, 1.0f),
                new Vector3f(0.55f, 0.80f, 1.0f),
                0.7f);

        double ringRadius = 1.1;
        for (int i = 0; i < 24; i++) {
            double angle = (Math.PI * 2.0 * i) / 24.0;
            double px = cx + Math.cos(angle) * ringRadius;
            double py = cy + (rand.nextDouble() - 0.5) * 0.1;
            double pz = cz + Math.sin(angle) * ringRadius;
            level.addParticle(stardust, px, py, pz, 0.0, 0.0, 0.0);
        }

        for (int i = 0; i < 10; i++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double r = rand.nextDouble() * 0.9;
            double px = cx + Math.cos(angle) * r;
            double py = cy + (rand.nextDouble() - 0.5) * 0.1;
            double pz = cz + Math.sin(angle) * r;
            level.addParticle(stardust, px, py, pz, 0.0, 0.0, 0.0);
        }

        for (int i = 0; i < 6; i++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double r = rand.nextDouble() * 1.0;
            double px = cx + Math.cos(angle) * r;
            double py = cy + rand.nextDouble() * 0.1;
            double pz = cz + Math.sin(angle) * r;
            double vx = Math.cos(angle) * 0.02;
            double vy = 0.01;
            double vz = Math.sin(angle) * 0.02;
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, vx, vy, vz);
        }

        for (int i = 0; i < 4; i++) {
            double angle = rand.nextDouble() * Math.PI * 2.0;
            double r = rand.nextDouble() * 0.6;
            double px = cx + Math.cos(angle) * r;
            double py = cy + (rand.nextDouble() - 0.5) * 0.1;
            double pz = cz + Math.sin(angle) * r;
            level.addParticle(ParticleTypes.END_ROD, px, py, pz, 0.0, 0.005, 0.0);
        }

        level.playLocalSound(cx, cy, cz, SoundEvents.NOTE_BLOCK_CHIME.value(),
                SoundSource.PLAYERS, 0.5F, 1.4F, false);
        level.playLocalSound(cx, cy, cz, SoundEvents.NOTE_BLOCK_BELL.value(),
                SoundSource.PLAYERS, 0.3F, 1.2F, false);
    }
}
