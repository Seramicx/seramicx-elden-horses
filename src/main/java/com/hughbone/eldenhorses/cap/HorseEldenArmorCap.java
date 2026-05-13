package com.hughbone.eldenhorses.cap;

import com.hughbone.eldenhorses.enchantment.ModEnchantments;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AutoRegisterCapability
public interface HorseEldenArmorCap {
    Capability<HorseEldenArmorCap> CAP = CapabilityManager.get(new CapabilityToken<>() {});

    boolean hasSpectralSteed();
    boolean hasSpectralLeap();
    default boolean hasAny() { return hasSpectralSteed() || hasSpectralLeap(); }
    void updateFromHorse(AbstractHorse horse);

    static HorseEldenArmorCap of(AbstractHorse horse) {
        return horse.getCapability(CAP).orElse(EMPTY);
    }

    HorseEldenArmorCap EMPTY = new HorseEldenArmorCap() {
        @Override public boolean hasSpectralSteed() { return false; }
        @Override public boolean hasSpectralLeap() { return false; }
        @Override public void updateFromHorse(AbstractHorse h) {}
    };

    final class Impl implements HorseEldenArmorCap {
        private boolean hasSteed;
        private boolean hasLeap;

        @Override public boolean hasSpectralSteed() { return hasSteed; }
        @Override public boolean hasSpectralLeap() { return hasLeap; }

        @Override public void updateFromHorse(AbstractHorse horse) {
            if (!(horse instanceof Horse h)) {
                hasSteed = false;
                hasLeap = false;
                return;
            }
            ItemStack armor = h.getArmor();
            if (armor.isEmpty()) {
                hasSteed = false;
                hasLeap = false;
                return;
            }
            hasSteed = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SPECTRAL_STEED.get(), armor) > 0;
            hasLeap = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SPECTRAL_LEAP.get(), armor) > 0;
        }
    }

    final class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final Impl impl = new Impl();
        private LazyOptional<HorseEldenArmorCap> opt = LazyOptional.of(() -> impl);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap != CAP) return LazyOptional.empty();
            // Forge's reviveCaps doesn't restore LazyOptionals
            if (!opt.isPresent()) {
                opt = LazyOptional.of(() -> impl);
            }
            return opt.cast();
        }
        @Override public CompoundTag serializeNBT() {
            CompoundTag t = new CompoundTag();
            t.putBoolean("spectralSteed", impl.hasSteed);
            t.putBoolean("spectralLeap", impl.hasLeap);
            return t;
        }
        @Override public void deserializeNBT(CompoundTag t) {
            impl.hasSteed = t.getBoolean("spectralSteed");
            impl.hasLeap = t.getBoolean("spectralLeap");
        }
        public void invalidate() { opt.invalidate(); }
    }
}
