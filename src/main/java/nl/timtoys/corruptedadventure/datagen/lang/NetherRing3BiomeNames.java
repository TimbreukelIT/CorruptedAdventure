package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModBiomes;

/**
 * Names of the Nether ring 3 biomes. Owned by stream W-nether ({@code docs/plans/ring-3/waves.md}); English is
 * mandatory, Dutch welcome.
 */
public final class NetherRing3BiomeNames {
    private NetherRing3BiomeNames() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.biome(ModBiomes.FROZEN_LAVA_SEA, "Frozen Lava Sea");
        lang.biome(ModBiomes.RIME_FOREST, "Rime Forest");
        lang.biome(ModBiomes.GLACIER_RIFTS, "Glacier Rifts");
    }

    public static void addDutch(LangOutput lang) {
        lang.biome(ModBiomes.FROZEN_LAVA_SEA, "Bevroren lavazee");
        lang.biome(ModBiomes.RIME_FOREST, "Rijpwoud");
        lang.biome(ModBiomes.GLACIER_RIFTS, "Gletsjerkloven");
    }
}
