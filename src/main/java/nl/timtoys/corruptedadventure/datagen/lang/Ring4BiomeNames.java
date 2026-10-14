package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModBiomes;

/**
 * Names of the ring 4 overworld biomes, {@code sundered_reach} included (change 64). Owned by stream W-biomes
 * ({@code docs/plans/ring-4/waves.md}); English is mandatory, Dutch welcome.
 */
public final class Ring4BiomeNames {
    private Ring4BiomeNames() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.biome(ModBiomes.CASCADE_VALE, "Cascade Vale");
        lang.biome(ModBiomes.CRYSTAL_LAGOON, "Crystal Lagoon");
        lang.biome(ModBiomes.LOTUS_SHALLOWS, "Lotus Shallows");
        lang.biome(ModBiomes.GLOWROOT_SWAMP, "Glowroot Swamp");
        lang.biome(ModBiomes.MIST_FJORDS, "Mist Fjords");
        lang.biome(ModBiomes.DROWNED_FOREST, "Drowned Forest");
        lang.biome(ModBiomes.PEAT_MOORS, "Peat Moors");
        lang.biome(ModBiomes.REED_MARSH, "Reed Marsh");
        lang.biome(ModBiomes.SKY_GARDENS, "Sky Gardens");
        lang.biome(ModBiomes.WITHERED_GARDENS, "Withered Gardens");
        lang.biome(ModBiomes.SUNDERED_REACH, "Sundered Reach");
    }

    public static void addDutch(LangOutput lang) {
        lang.biome(ModBiomes.CASCADE_VALE, "Watervallendal");
        lang.biome(ModBiomes.CRYSTAL_LAGOON, "Kristallagune");
        lang.biome(ModBiomes.LOTUS_SHALLOWS, "Lotusondiepten");
        lang.biome(ModBiomes.GLOWROOT_SWAMP, "Gloeiwortelmoeras");
        lang.biome(ModBiomes.MIST_FJORDS, "Nevelfjorden");
        lang.biome(ModBiomes.DROWNED_FOREST, "Verdronken woud");
        lang.biome(ModBiomes.PEAT_MOORS, "Hoogveen");
        lang.biome(ModBiomes.REED_MARSH, "Rietmoeras");
        lang.biome(ModBiomes.SKY_GARDENS, "Hemeltuinen");
        lang.biome(ModBiomes.WITHERED_GARDENS, "Verdorde tuinen");
        lang.biome(ModBiomes.SUNDERED_REACH, "Verscheurde rand");
    }
}
