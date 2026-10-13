package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModBiomes;

/**
 * Names of the Nether ring 4 biomes. Owned by stream W-nether ({@code docs/plans/ring-4/waves.md}); English is mandatory,
 * Dutch welcome.
 */
public final class NetherRing4BiomeNames {
    private NetherRing4BiomeNames() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.biome(ModBiomes.BONE_DUNES, "Bone Dunes");
        lang.biome(ModBiomes.CRYSTAL_SPIRES, "Crystal Spires");
        lang.biome(ModBiomes.GEODE_HOLLOWS, "Geode Hollows");
    }

    public static void addDutch(LangOutput lang) {
        lang.biome(ModBiomes.BONE_DUNES, "Bottenduinen");
        lang.biome(ModBiomes.CRYSTAL_SPIRES, "Kristalspitsen");
        lang.biome(ModBiomes.GEODE_HOLLOWS, "Geodeholten");
    }
}
