package com.hughbone.eldenhorses.sound;

import com.hughbone.eldenhorses.EldenHorses;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EldenHorses.MODID);

    public static final RegistryObject<SoundEvent> WHISTLE = SOUNDS.register(
            "whistle",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(EldenHorses.MODID, "whistle")));

    private ModSounds() {}
}
