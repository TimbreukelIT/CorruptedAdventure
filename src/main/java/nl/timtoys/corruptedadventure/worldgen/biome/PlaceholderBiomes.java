package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import nl.timtoys.corruptedadventure.registry.ModBiomes;

/**
 * Datagen bootstrap for the skeleton biomes: one placeholder per ring and a void edge, per dimension. Ring 1 (overworld
 * and Nether) no longer has one: it uses {@link VanillaBiomeCopies}; overworld ring 2 neither: {@link Ring2Biomes}.
 * Only colours and weather (precipitation, temperature) differ; no features and no mob spawns, and no
 * {@code has_structure/*} tags, so no vanilla structures (strongholds included) can generate in them.
 */
public final class PlaceholderBiomes {
    private static final int DEFAULT_WATER = 0x3F76E4;
    private static final int DEFAULT_WATER_FOG = 0x050533;
    private static final int NETHER_SKY = 0x000000;

    private PlaceholderBiomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        // Ring 3: volcanic, reddish ash sky.
        context.register(ModBiomes.PLACEHOLDER_RING_3, overworld(false, 2.0F, 0.0F,
                0xB0705A, 0x8A6A5A, 0x6B6B6B, 0x2A2A2A, 0x6B5A3A, 0x5A4A2E));
        // Ring 4: swamp, murky water; vanilla rain.
        context.register(ModBiomes.PLACEHOLDER_RING_4, overworld(true, 0.8F, 0.9F,
                0x8BA08A, 0x9DAA8A, 0x617B64, 0x232317, 0x6A7039, 0x6A7039));
        // Ring 5: corrupted, colourless.
        context.register(ModBiomes.PLACEHOLDER_RING_5, overworld(false, 0.5F, 0.0F,
                0x3A3A48, 0x5A5A66, 0x2B2B3A, 0x0A0A12, 0x8A8A8A, 0x777777));
        context.register(ModBiomes.VOID_EDGE, overworld(false, 0.5F, 0.0F,
                0x000000, 0x14141C, 0x1A1A24, 0x050508, 0x3A3A3A, 0x333333));

        // Nether counterparts (see fase-1-wereld.md §1.12); fog is what sets them apart. Nether ring 1 has none:
        // it uses VanillaBiomeCopies.
        context.register(ModBiomes.PLACEHOLDER_NETHER_RING_2, nether(0x2E4A3A, 0x3D8C7A));        // soggy fungal
        context.register(ModBiomes.PLACEHOLDER_NETHER_RING_3, nether(0x9CC4D8, 0x8FB8DE));        // frozen
        context.register(ModBiomes.PLACEHOLDER_NETHER_RING_4, nether(0xD8CBA6, DEFAULT_WATER));   // bone desert
        context.register(ModBiomes.PLACEHOLDER_NETHER_RING_5, nether(0xE8E8EC, DEFAULT_WATER));   // white, silent
        context.register(ModBiomes.NETHER_VOID_EDGE, nether(0x0A0A0A, DEFAULT_WATER));
    }

    private static Biome overworld(boolean precipitation, float temperature, float downfall, int sky, int fog,
                                   int water, int waterFog, int grass, int foliage) {
        return biome(precipitation, temperature, downfall, new BiomeSpecialEffects.Builder()
                .skyColor(sky)
                .fogColor(fog)
                .waterColor(water)
                .waterFogColor(waterFog)
                .grassColorOverride(grass)
                .foliageColorOverride(foliage));
    }

    private static Biome nether(int fog, int water) {
        return biome(false, 2.0F, 0.0F, new BiomeSpecialEffects.Builder()
                .skyColor(NETHER_SKY)
                .fogColor(fog)
                .waterColor(water)
                .waterFogColor(DEFAULT_WATER_FOG));
    }

    private static Biome biome(boolean precipitation, float temperature, float downfall, BiomeSpecialEffects.Builder effects) {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(precipitation)
                .temperature(temperature)
                .downfall(downfall)
                .specialEffects(effects.build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }
}
