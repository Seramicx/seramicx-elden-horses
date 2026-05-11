package com.hughbone.eldenhorses;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class EldenHorsesConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    private static final ForgeConfigSpec.DoubleValue TRANSPARENCY_ALPHA;
    private static final ForgeConfigSpec.DoubleValue BODY_TRANSPARENCY_ALPHA;
    private static final ForgeConfigSpec.DoubleValue CHEST_DROP_CHANCE;
    private static final ForgeConfigSpec.DoubleValue BOOK_TO_ARMOR_RATIO;
    private static final ForgeConfigSpec.IntValue SPECTRAL_STEED_DEATH_COOLDOWN_SECONDS;

    public static volatile double transparencyAlpha = 0.3;
    public static volatile double bodyTransparencyAlpha = 0.15;
    public static volatile double chestDropChance = 0.05;
    public static volatile double bookToArmorRatio = 0.7;
    public static volatile int spectralSteedDeathCooldownSeconds = 300;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("rendering");
        TRANSPARENCY_ALPHA = b
                .comment(
                        "Alpha applied to the horse ARMOR in 1st person while riding a horse",
                        "with the Spectral Steed enchantment. 0 = invisible, 1 = fully opaque."
                )
                .defineInRange("transparency_alpha", 0.3, 0.0, 1.0);
        BODY_TRANSPARENCY_ALPHA = b
                .comment(
                        "Alpha applied to the horse BODY in 1st person while riding a horse",
                        "with the Spectral Steed enchantment. 0.15 matches vanilla translucent.",
                        "Lower for a more ghostly silhouette (e.g. 0.08 hides white spots more)."
                )
                .defineInRange("body_transparency_alpha", 0.15, 0.0, 1.0);
        b.pop();

        b.push("loot");
        CHEST_DROP_CHANCE = b
                .comment(
                        "Probability per chest opening that a Spectral Steed/Leap drop is added.",
                        "Targets any chest tagged forge:chests/* (vanilla and modded)."
                )
                .defineInRange("chest_drop_chance", 0.05, 0.0, 1.0);
        BOOK_TO_ARMOR_RATIO = b
                .comment(
                        "When a drop fires, ratio of enchanted books vs pre-enchanted horse armor.",
                        "1.0 = always book, 0.0 = always pre-enchanted armor."
                )
                .defineInRange("book_to_armor_ratio", 0.7, 0.0, 1.0);
        b.pop();

        b.push("spectral_steed");
        SPECTRAL_STEED_DEATH_COOLDOWN_SECONDS = b
                .comment(
                        "When the bonded horse would die, it auto-unsummons instead and goes",
                        "into recovery for this many seconds before it can be summoned again."
                )
                .defineInRange("death_cooldown_seconds", 300, 0, 3600);
        b.pop();

        COMMON_SPEC = b.build();
    }

    private EldenHorsesConfig() {}

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) refresh();
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) refresh();
    }

    private static void refresh() {
        transparencyAlpha = TRANSPARENCY_ALPHA.get();
        bodyTransparencyAlpha = BODY_TRANSPARENCY_ALPHA.get();
        chestDropChance = CHEST_DROP_CHANCE.get();
        bookToArmorRatio = BOOK_TO_ARMOR_RATIO.get();
        spectralSteedDeathCooldownSeconds = SPECTRAL_STEED_DEATH_COOLDOWN_SECONDS.get();
    }
}
