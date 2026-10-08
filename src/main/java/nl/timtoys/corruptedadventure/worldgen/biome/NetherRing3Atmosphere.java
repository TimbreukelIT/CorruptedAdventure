package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import nl.timtoys.corruptedadventure.registry.ModBiomes;

import java.util.Map;

/**
 * The atmosphere of the Nether ring 3 biomes in datagen (stream A, packages 50-0 and 56): fog and water colours and
 * later particles and sounds. Only effects: the biome bootstrap (stream W-nether) reaches this through
 * {@link BiomeAmbience#netherRing3} and adds only the music. Fog distance is not biome data.
 */
final class NetherRing3Atmosphere {
    /** Blue-white, cold fog; no sky in the Nether. Provisional until package 56. */
    static final BiomeAmbience.Palette NETHER_RING_3 = new BiomeAmbience.Palette(0x000000, 0x9CB0C4, 0x3D6E9C, 0x0A1E33);

    private static final Map<ResourceKey<Biome>, BiomeAmbience.Palette> PALETTES = Map.of(
            ModBiomes.FROZEN_LAVA_SEA, NETHER_RING_3,
            // A touch of violet.
            ModBiomes.RIME_FOREST, NETHER_RING_3.withFog(0xA8A4C4),
            // Colder and bluer.
            ModBiomes.GLACIER_RIFTS, NETHER_RING_3.withFog(0x88A8CC));

    private NetherRing3Atmosphere() {
    }

    /** @throws IllegalArgumentException for a biome without Nether ring 3 atmosphere */
    static BiomeSpecialEffects.Builder effects(ResourceKey<Biome> biome) {
        return BiomeAmbience.colours(palette(biome)).ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS);
    }

    /** @throws IllegalArgumentException for a biome without Nether ring 3 atmosphere */
    static BiomeAmbience.Palette palette(ResourceKey<Biome> biome) {
        BiomeAmbience.Palette palette = PALETTES.get(biome);
        if (palette == null) {
            throw new IllegalArgumentException("no Nether ring 3 atmosphere for " + biome.location());
        }
        return palette;
    }
}
