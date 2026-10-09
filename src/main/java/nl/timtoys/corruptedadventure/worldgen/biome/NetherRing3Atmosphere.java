package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

import java.util.Map;

/**
 * The atmosphere of the Nether ring 3 biomes in datagen (stream A, package 56): blue-white fog per biome, drifting ice
 * crystals ({@code ice_glint}), the cold wind as a loop and creaking ice as additions, vanilla's cave mood. Only effects:
 * the biome bootstrap (stream W-nether) reaches this through {@link BiomeAmbience#netherRing3} and adds only the music.
 * Fog distance is not biome data (G2-A5 = A: no extra freezing fog); the breath clouds are client code.
 */
final class NetherRing3Atmosphere {
    /** Blue-white, cold fog; no sky in the Nether. */
    static final BiomeAmbience.Palette NETHER_RING_3 = new BiomeAmbience.Palette(0x000000, 0x9CB0C4, 0x3D6E9C, 0x0A1E33);
    /** How often the ice creaks (vanilla's basalt deltas additions: 0.0111 per tick). */
    static final double ICE_CREAK_CHANCE = 0.004;

    private static final Map<ResourceKey<Biome>, Look> LOOKS = Map.of(
            // The base: the frozen lava sea.
            ModBiomes.FROZEN_LAVA_SEA, new Look(NETHER_RING_3, 0.004F),
            // A touch of violet, a few more crystals among the rime caps.
            ModBiomes.RIME_FOREST, new Look(NETHER_RING_3.withFog(0xA8A4C4), 0.006F),
            // Colder and bluer.
            ModBiomes.GLACIER_RIFTS, new Look(NETHER_RING_3.withFog(0x88A8CC), 0.005F));

    private NetherRing3Atmosphere() {
    }

    /** @throws IllegalArgumentException for a biome without Nether ring 3 atmosphere */
    static BiomeSpecialEffects.Builder effects(ResourceKey<Biome> biome) {
        Look look = look(biome);
        return BiomeAmbience.colours(look.colours())
                .ambientParticle(new AmbientParticleSettings(ModParticleTypes.ICE_GLINT.get(), look.glints()))
                .ambientLoopSound(ModSoundEvents.NETHER_RING_3_WIND)
                .ambientAdditionsSound(new AmbientAdditionsSettings(ModSoundEvents.NETHER_RING_3_ICE_CREAK, ICE_CREAK_CHANCE))
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS);
    }

    /** @throws IllegalArgumentException for a biome without Nether ring 3 atmosphere */
    static Look look(ResourceKey<Biome> biome) {
        Look look = LOOKS.get(biome);
        if (look == null) {
            throw new IllegalArgumentException("no Nether ring 3 atmosphere for " + biome.location());
        }
        return look;
    }

    /** Fog and water colours and the ice crystals' probability per tried spot (G2-A3 = A: subtle). */
    record Look(BiomeAmbience.Palette colours, float glints) {
    }
}
