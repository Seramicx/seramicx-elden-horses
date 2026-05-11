package com.hughbone.eldenhorses.mixin;

import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes any HorseArmorItem enchantable at the enchanting table.
 *
 * <p>Vanilla {@code Item.getEnchantmentValue} returns 0 for HorseArmorItem and
 * vanilla {@code Item.isEnchantable} requires both stack-size 1 AND
 * canBeDepleted, which horse armor fails. So the table never offers our
 * Spectral enchants on raw horse armor without help.
 *
 * <p>Priority 900 (lower than default 1000) and a {@code cir.isCancelled()}
 * short-circuit at HEAD so we coexist with Shiny Horses Forge, which does the
 * exact same trick at default priority. With Shiny Horses installed: theirs
 * runs first, sets value=1, cancels; ours sees cancelled and returns. Without
 * Shiny Horses: ours runs and provides table support.
 */
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
