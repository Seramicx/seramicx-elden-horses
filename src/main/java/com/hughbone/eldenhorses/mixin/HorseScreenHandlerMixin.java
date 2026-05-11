package com.hughbone.eldenhorses.mixin;

import com.hughbone.eldenhorses.cap.HorseEldenArmorCap;
import com.hughbone.eldenhorses.cap.PlayerEldenHorseCap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.HorseInventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HorseInventoryMenu.class)
public abstract class HorseScreenHandlerMixin {

    @Shadow @Final private AbstractHorse horse;

    @Inject(method = "removed", at = @At("HEAD"))
    private void elden_close(Player player, CallbackInfo ci) {
        if (!(horse instanceof Horse h)) return;
        HorseEldenArmorCap.of(h).updateFromHorse(h);
        if (player instanceof ServerPlayer sp
                && h.equals(sp.getVehicle())
                && HorseEldenArmorCap.of(h).hasAny()) {
            PlayerEldenHorseCap.of(sp).bindHorse(sp, h);
        }
    }
}
