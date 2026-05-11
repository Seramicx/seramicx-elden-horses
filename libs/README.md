# libs/

Compile-only dependencies for optional Apotheosis tome support. These jars are NOT redistributable, so they are excluded from git via `.gitignore` and must be populated manually after cloning.

Drop the following files into this directory (matching versions exactly so the flatDir resolver in `build.gradle` finds them):

- `Apotheosis-1.20.1-7.4.8.jar`
- `Placebo-1.20.1-8.6.3.jar`

Both can be downloaded from CurseForge:

- https://www.curseforge.com/minecraft/mc-mods/apotheosis
- https://www.curseforge.com/minecraft/mc-mods/placebo

Without these, the build fails to compile `compat/apotheosis/HorseArmorTomeFactory.java`. At runtime the tome registration is gated on `ModList.get().isLoaded("apotheosis")`, so the published mod still works fine for end-users who don't have Apotheosis installed.
