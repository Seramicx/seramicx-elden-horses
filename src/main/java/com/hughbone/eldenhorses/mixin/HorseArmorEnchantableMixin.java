package com.hughbone.eldenhorses.mixin;

import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Priority 900 so Shiny Horses (default 1000) wins when present.
@Mixin(value = Item.class, priority = 900)
public abstract class HorseArmorEnchantableMixin {

    @Inject(method = "getEnchantmentValue", at = @At("HEAD"), cancellable = true)
    private void elden_horseArmorEnchantValue(CallbackInfoReturnable<Integer> cir) {
        if (cir.isCancelled()) return;
        if ((Object) this instanceof HorseArmorItem) {
            cir.setReturnValue(5);
        }
    }

    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
    private void elden_horseArmorIsEnchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.isCancelled()) return;
        if ((Object) this instanceof HorseArmorItem) {
            cir.setReturnValue(true);
        }
    }
}
