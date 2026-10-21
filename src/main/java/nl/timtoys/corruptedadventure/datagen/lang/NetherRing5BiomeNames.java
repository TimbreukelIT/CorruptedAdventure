package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModBiomes;

/**
 * Names of the Nether ring 5 biomes. Owned by stream W-nether
 * ({@code docs/plans/ring-5/waves.md}); English is mandatory, Dutch welcome.
 */
public final class NetherRing5BiomeNames {
    private NetherRing5BiomeNames() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.biome(ModBiomes.WHITE_HALLS, "White Halls");
        lang.biome(ModBiomes.HOLLOW_CATHEDRAL, "Hollow Cathedral");
    }

    public static void addDutch(LangOutput lang) {
        lang.biome(ModBiomes.WHITE_HALLS, "Witte Hallen");
        lang.biome(ModBiomes.HOLLOW_CATHEDRAL, "Holle Kathedraal");
    }
}
