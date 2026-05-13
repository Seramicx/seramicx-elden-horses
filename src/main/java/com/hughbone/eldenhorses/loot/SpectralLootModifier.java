package com.hughbone.eldenhorses.loot;

import com.google.common.base.Suppliers;
import com.hughbone.eldenhorses.EldenHorses;
import com.hughbone.eldenhorses.EldenHorsesConfig;
import com.hughbone.eldenhorses.enchantment.ModEnchantments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SpectralLootModifier extends LootModifier {

    public static final Supplier<Codec<SpectralLootModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, SpectralLootModifier::new))
    );

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, EldenHorses.MODID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> SPECTRAL_CHEST_LOOT =
            SERIALIZERS.register("spectral_chest_loot", CODEC);

    public SpectralLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    @NotNull
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generated, LootContext ctx) {
        ResourceLocation table = ctx.getQueriedLootTableId();
        if (table == null || !table.getPath().contains("chests/")) return generated;

        RandomSource rand = ctx.getRandom();
        if (rand.nextDouble() >= EldenHorsesConfig.chestDropChance) return generated;
        ItemStack drop = rollDrop(rand);
        if (!drop.isEmpty()) generated.add(drop);
        return generated;
    }

    private static ItemStack rollDrop(RandomSource rand) {
        Enchantment ench = (rand.nextDouble() < 0.7)
                ? ModEnchantments.SPECTRAL_STEED.get()
                : ModEnchantments.SPECTRAL_LEAP.get();
        boolean asBook = rand.nextDouble() < EldenHorsesConfig.bookToArmorRatio;

        if (asBook) {
            ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
            EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(ench, 1));
            return stack;
        }

        Item armor = pickArmor(rand);
        ItemStack stack = new ItemStack(armor);
        stack.enchant(ench, 1);
        return stack;
    }

    private static Item pickArmor(RandomSource rand) {
        double r = rand.nextDouble();
        if (r < 0.50) return Items.IRON_HORSE_ARMOR;
        if (r < 0.85) return Items.GOLDEN_HORSE_ARMOR;
        return Items.DIAMOND_HORSE_ARMOR;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
