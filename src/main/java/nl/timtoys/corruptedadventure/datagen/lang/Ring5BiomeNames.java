package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModBiomes;

/**
 * Names of the ring 5 overworld biomes (package 86). Owned by stream W-biomes ({@code docs/plans/ring-5/waves.md});
 * English is mandatory, Dutch welcome.
 */
public final class Ring5BiomeNames {
    private Ring5BiomeNames() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.biome(ModBiomes.CORRUPTED_WASTES, "Corrupted Wastes");
        lang.biome(ModBiomes.GLITCH_FIELDS, "Glitch Fields");
        lang.biome(ModBiomes.FALSE_MEADOW, "False Meadow");
        lang.biome(ModBiomes.VOID_SCARS, "Void Scars");
        lang.biome(ModBiomes.INVERTED_HILLS, "Inverted Hills");
        lang.biome(ModBiomes.SHATTERED_EXPANSE, "Shattered Expanse");
    }

    public static void addDutch(LangOutput lang) {
        lang.biome(ModBiomes.CORRUPTED_WASTES, "Gecorrumpeerde woestenij");
        lang.biome(ModBiomes.GLITCH_FIELDS, "Glitchvelden");
        lang.biome(ModBiomes.FALSE_MEADOW, "Valse weide");
        lang.biome(ModBiomes.VOID_SCARS, "Voidlittekens");
        lang.biome(ModBiomes.INVERTED_HILLS, "Omgekeerde heuvels");
        lang.biome(ModBiomes.SHATTERED_EXPANSE, "Verbrijzelde vlakte");
    }
}
