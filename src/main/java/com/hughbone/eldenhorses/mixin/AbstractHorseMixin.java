package com.hughbone.eldenhorses.mixin;

import com.hughbone.eldenhorses.cap.HorseEldenArmorCap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
        }
    }
}
