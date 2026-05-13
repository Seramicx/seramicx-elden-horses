# Seramicx's Elden Horses

A Forge 1.20.1 port of [HughBone's Elden Horses](https://github.com/HughBone/elden-horses) (originally Fabric, MC 1.19.2), heavily expanded with Spectral enchantments, an Elden Ring style summon animation, and Apotheosis / Shiny Horses compat.

<p align="center">
  <a href="https://youtu.be/U2Gf7-xuLw0">
    <img src="https://i.ytimg.com/vi/U2Gf7-xuLw0/hqdefault.jpg" alt="Animation showcase" width="600">
  </a>
  <br>
  <em>Animation showcase (click to watch)</em>
</p>

## Spectral enchantments

Two new enchantments for any vanilla or modded horse armor:

<p align="center">
  <img src="docs/media/spectral-enchantments-tooltip.png" alt="Spectral Steed and Spectral Leap stacked on netherite horse armor" width="600">
</p>

**Spectral Steed**: bond a horse and summon or dismiss it on demand.
- 1st-person body and armor go translucent while riding
- 5x mining speed on horseback
- Reduced mount fall damage
- Fatal hits auto-unsummon the horse and start a configurable cooldown instead of killing it
- Once bound, the horse persists across world reload, death, and dimension change

<p align="center">
  <img src="docs/media/1st-person-translucency.png" alt="1st-person translucent horse while mining" width="400">
</p>

**Spectral Leap**: mid-air double jump, with a cosmic burst of particles and chime on activation.

Obtain via enchanting table, anvil + book, or chest loot.

## Summon and unsummon animation

Elden Ring style. Whistle, hop onto the materializing horse, ride off. Reverse on unsummon. i-frames during the transition; momentum carries over if you dismount mid-gallop.

## Tome of Horse Armor (Apotheosis)

When Apotheosis is installed, a Tome of Horse Armor item registers:

<p align="center">
  <img src="docs/media/apotheosis-tome.png" alt="Tome of Horse Armor tooltip" width="600">
</p>

- Appears in the Apotheosis Enchanting creative tab after the Boots Tome
- Crafted from 6 books and 1 blaze rod
- Accepts any horse-armor enchantment (Spectral Steed, Spectral Leap, plus Shiny Horses' Protection / Mending / etc.)

Without Apotheosis the tome simply doesn't register.

## Cross-mod compatibility

- **Shiny Horses Forge**: vanilla armor enchantments (Protection, Mending, Unbreaking, etc.) stack with Spectral Steed and Spectral Leap on the same armor
- **Any mod adding horse armor**: Spectral enchantments work on any item extending `HorseArmorItem`

## Loot drops

A Forge global loot modifier rolls extra drops into any chest table (vanilla or modded) whose path contains `chests/`:

- Spectral Steed / Leap enchanted books
- Pre-enchanted horse armor (iron / gold / diamond)

Drop rate and book-to-armor ratio are config-driven. Netherite horse armor is intentionally excluded from loot and must be smithed (template + diamond horse armor + netherite ingot, same as vanilla netherite armor upgrades).

## Configuration

Edit `config/elden_horses-common.toml`:

| Key | Default | Effect |
|-----|---------|--------|
| `transparency_alpha` | 0.3 | Horse armor alpha in 1st-person while riding |
| `body_transparency_alpha` | 0.15 | Horse body alpha in 1st-person while riding |
| `chest_drop_chance` | 0.05 | Per-chest chance of a Spectral drop |
| `book_to_armor_ratio` | 0.7 | Share of drops that are books vs pre-enchanted armor |
| `spectral_steed_death_cooldown_seconds` | 300 | Recovery time before re-summon after a fatal-hit save |

## Required dependencies

- Minecraft 1.20.1, Forge 47.4.4+
- [PlayerAnimator](https://www.curseforge.com/minecraft/mc-mods/playeranimator) by KosmX (whistle and mount animations)

## Optional dependencies

- [Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis): Tome of Horse Armor
- [Shiny Horses Forge](https://www.curseforge.com/minecraft/mc-mods/shiny-horses-forge): vanilla armor enchantments on horse armor
- [Better Mount Steering](https://www.curseforge.com/minecraft/mc-mods/better-mount-steering): decouples mount camera from horse direction so you can look around freely while riding. Strongly recommended for the full Elden Ring mounted feel

## Installation

Download the jar from [Releases](https://github.com/Seramicx/seramicx-elden-horses/releases), drop it in your `mods/` folder alongside PlayerAnimator.

## Credits

- **hughbone_** for the original Elden Horses mod ([CurseForge](https://www.curseforge.com/minecraft/mc-mods/elden-horses))
- **InspectorJ** for the original whistle SFX ([Freesound](https://freesound.org/people/InspectorJ/sounds/423285/)), modified for this mod
- **KosmX** for PlayerAnimator (runtime dependency, used under its own license)
- **Shadows-of-Fire** for Apotheosis (lang keys referenced for tome tooltips; jar must be sourced separately to build with compat)

## License

MIT (see `LICENSE`). The `elden_horses` mod ID is preserved for save compatibility with the original.

## Build

```
./gradlew build
```

Output: `build/libs/seramicx-elden-horses-1.0.0+forge-1.20.1.jar`

See `libs/README.md` for how to populate the compile-only deps directory.
