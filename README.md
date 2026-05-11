# Elden Horses (Forge 1.20.1 port)

A clean Forge 1.20.1 reimplementation of [HughBone's Elden Horses](https://github.com/HughBone/elden-horses) (Fabric, MC 1.19.2).

## Features

Equip a Netherite Horse Armor on a horse to unlock:

- **Double jump** — press jump again mid-air for a forward boost in look direction.
- **Summon hotkey** — bind a key in `Controls > Elden Horses > Summon Horse`. Press to teleport your last-mounted elden horse to your position. Cross-dimension safe.
- **Dismount-disappear** — your elden horse vanishes when you dismount and is recallable via the summon key. Persists across world reload, death/respawn, and dimension change.

Quality-of-life:
- Horse never bucks the rider (`isAngry` suppressed).
- Reduced fall damage (-4) for the horse itself.
- Block break speed ×5 while mounted.
- Horse model gets translucent in first-person while mounted.

## Recipes

- **Crafting:** 5 netherite ingots in horse-armor pattern (same as the original).
- **Smithing:** netherite upgrade template + diamond horse armor + netherite ingot (1.20.1 smithing format).

## Credits & License

Original Fabric implementation by **HughBone**, MIT License (see `LICENSE`).
This Forge port follows the same MIT License and preserves the `elden_horses` mod id and `elden_horses:netherite_horse_armor` item id for compatibility.

## Build

```
./gradlew build
```

Output: `build/libs/elden_horses-2.0.0+forge-1.20.1.jar`
