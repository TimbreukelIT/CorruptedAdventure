package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModBiomes;

/**
 * Names of the ring 3 overworld biomes ({@code steam_fens} follows in wave 2). Owned by stream W-biomes
 * ({@code docs/plans/ring-3/waves.md}); English is mandatory, Dutch welcome.
 */
public final class Ring3BiomeNames {
    private Ring3BiomeNames() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.biome(ModBiomes.ASHEN_WASTES, "Ashen Wastes");
        lang.biome(ModBiomes.VOLCANIC_PEAKS, "Volcanic Peaks");
        lang.biome(ModBiomes.EMBER_WOODS, "Ember Woods");
        lang.biome(ModBiomes.BASALT_FLOWS, "Basalt Flows");
        lang.biome(ModBiomes.SULFUR_FIELDS, "Sulfur Fields");
        lang.biome(ModBiomes.DRIED_SEABED, "Dried Seabed");
        lang.biome(ModBiomes.TITAN_FIELDS, "Titan Fields");
        lang.biome(ModBiomes.DORMANT_CALDERA, "Dormant Caldera");
    }

    public static void addDutch(LangOutput lang) {
        lang.biome(ModBiomes.ASHEN_WASTES, "Asvlakte");
        lang.biome(ModBiomes.VOLCANIC_PEAKS, "Vulkaanpieken");
        lang.biome(ModBiomes.EMBER_WOODS, "Smeulwoud");
        lang.biome(ModBiomes.BASALT_FLOWS, "Basaltstromen");
        lang.biome(ModBiomes.SULFUR_FIELDS, "Zwavelvelden");
        lang.biome(ModBiomes.DRIED_SEABED, "Drooggevallen zeebodem");
        lang.biome(ModBiomes.TITAN_FIELDS, "Titanenveld");
        lang.biome(ModBiomes.DORMANT_CALDERA, "Slapende krater");
    }
}
