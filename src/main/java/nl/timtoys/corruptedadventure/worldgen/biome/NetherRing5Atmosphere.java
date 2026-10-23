package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;

/**
 * The sound and particles of the Nether ring 5 biomes in datagen (stream A, package 98, V98-7 A): nothing. No ambient
 * loop, mood or additions; {@link BiomeAmbience#SILENT_MUSIC}, so vanilla's music never plays; and "almost no
 * particles": very rarely a white speck of ash. The white fog colours and the black sky stay W-nether's
 * ({@code NetherRing5Biomes}); the far white fog is client code ({@code NetherRing5Fog}).
 */
final class NetherRing5Atmosphere {
    /** Probability per tried spot of a white ash speck (vanilla's basalt deltas: 0.118). */
    static final float ASH_CHANCE = 0.0005F;

    private NetherRing5Atmosphere() {
    }

    /** @throws IllegalArgumentException for a biome outside Nether ring 5 */
    static BiomeSpecialEffects.Builder effects(ResourceKey<Biome> biome, BiomeSpecialEffects.Builder colours) {
        if (!NetherRing5Biomes.ALL.contains(biome)) {
            throw new IllegalArgumentException("no Nether ring 5 atmosphere for " + biome.location());
        }
        return colours.backgroundMusic(BiomeAmbience.SILENT_MUSIC)
                .ambientParticle(new AmbientParticleSettings(ParticleTypes.WHITE_ASH, ASH_CHANCE));
    }
}
