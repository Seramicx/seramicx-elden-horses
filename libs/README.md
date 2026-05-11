# libs/

Compile-only dependencies for optional Apotheosis tome support. These jars are NOT redistributable, so they are excluded from git via `.gitignore` and must be populated manually after cloning.

Drop the following files into this directory (matching versions exactly so the flatDir resolver in `build.gradle` finds them):

- `Apotheosis-1.20.1-7.4.8.jar` (optional dep)
- `Placebo-1.20.1-8.6.3.jar` (optional dep, transitive of Apotheosis)
- `player-animation-lib-forge-1.0.2-rc1+1.20.jar` (required runtime dep)

Sources:

- https://www.curseforge.com/minecraft/mc-mods/apotheosis
- https://www.curseforge.com/minecraft/mc-mods/placebo
- https://www.curseforge.com/minecraft/mc-mods/playeranimator

Without these, the build fails to compile. At runtime the Apotheosis tome registration is gated on `ModList.get().isLoaded("apotheosis")`, so the published mod still works fine for end-users who don't have Apotheosis installed. PlayerAnimator is a hard dependency (listed in mods.toml as mandatory) because the horse summon whistle animation requires it.
