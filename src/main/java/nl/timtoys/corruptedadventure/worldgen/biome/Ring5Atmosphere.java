package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The sound and particles of the ring 5 biomes in datagen (stream A, package 98, V98-5 A): the inner band is almost
 * silent (now and then a short glitch, more often in {@code glitch_fields}, with glitch pixels in the air; ring 1's
 * distorted cave sound underground), {@code false_meadow} is silent, and the outer band hums with the void (a low drone
 * as the loop, void motes drifting up, the most in {@code shattered_expanse}, no cave sound). No music anywhere in ring
 * 5: {@link BiomeAmbience#SILENT_MUSIC}. The colours stay W-biomes' ({@code Ring5Biomes}); its bootstrap passes them
 * through {@link BiomeAmbience#ring5}. The sky, the stars and the fog per band are client code ({@code Ring5Sky},
 * {@code Ring5Fog}).
 */
final class Ring5Atmosphere {
    private static final Map<ResourceKey<Biome>, Look> LOOKS = Map.of(
            ModBiomes.CORRUPTED_WASTES, new Look(null, 0.0F, false, 0.002, true),
            ModBiomes.GLITCH_FIELDS, new Look(ModParticleTypes.GLITCH_PIXEL, 0.0008F, false, 0.008, true),
            ModBiomes.FALSE_MEADOW, new Look(null, 0.0F, false, 0.0, false),
            ModBiomes.VOID_SCARS, new Look(ModParticleTypes.VOID_MOTE, 0.002F, true, 0.0, false),
            ModBiomes.INVERTED_HILLS, new Look(ModParticleTypes.VOID_MOTE, 0.002F, true, 0.0, false),
            ModBiomes.SHATTERED_EXPANSE, new Look(ModParticleTypes.VOID_MOTE, 0.004F, true, 0.0, false));

    private Ring5Atmosphere() {
    }

    /** @throws IllegalArgumentException for a biome without ring 5 atmosphere */
    static BiomeSpecialEffects.Builder effects(ResourceKey<Biome> biome, BiomeSpecialEffects.Builder colours) {
        Look look = look(biome);
        colours.backgroundMusic(BiomeAmbience.SILENT_MUSIC);
        if (look.particle() != null) {
            colours.ambientParticle(new AmbientParticleSettings(look.particle().get(), look.particleChance()));
        }
        if (look.drone()) {
            colours.ambientLoopSound(ModSoundEvents.RING_5_VOID_DRONE);
        }
        if (look.glitchChance() > 0) {
            colours.ambientAdditionsSound(new AmbientAdditionsSettings(ModSoundEvents.RING_5_GLITCH, look.glitchChance()));
        }
        if (look.strangeCaves()) {
            BiomeAmbience.STRANGE.ifPresent(colours::ambientMoodSound);
        }
        return colours;
    }

    /** @throws IllegalArgumentException for a biome without ring 5 atmosphere */
    static Look look(ResourceKey<Biome> biome) {
        Look look = LOOKS.get(biome);
        if (look == null) {
            throw new IllegalArgumentException("no ring 5 atmosphere for " + biome.location());
        }
        return look;
    }

    /**
     * The sound and particles of one biome.
     *
     * @param particle       the ambient particle, or {@code null}
     * @param particleChance its probability per tried spot
     * @param drone          the void drone as the loop
     * @param glitchChance   the glitch one-shot's chance per tick (additions), 0 for none
     * @param strangeCaves   ring 1's distorted cave sound as the mood (otherwise none)
     */
    record Look(@Nullable Supplier<? extends ParticleOptions> particle, float particleChance, boolean drone,
                double glitchChance, boolean strangeCaves) {
    }
}
