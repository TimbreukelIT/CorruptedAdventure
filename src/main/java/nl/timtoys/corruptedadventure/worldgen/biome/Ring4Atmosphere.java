package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The atmosphere of the ring 4 overworld biomes in datagen (stream A), reached through {@link BiomeAmbience#ring4}. The
 * base for change 80: for now only {@code verdant_falls} (the transition 3→4) has its own look; every other ring 4
 * biome keeps the colours of its bootstrap ({@code Ring4Biomes}) and only gets the cave mood, until 80 moves their
 * colours here as well.
 * <p>
 * {@code verdant_falls} (Q8 = A, only biome data): clear turquoise sky and water under a soft white-green haze, fine
 * spray drifting everywhere ({@code waterfall_mist}), the roar of distant waterfalls as its loop and tropical birds and
 * frogs as its additions. A biome particle is tried at about 1,300 random spots around the player per tick, so 0.005
 * is some 6 a tick: a light haze, below the steam fens' 0.01.
 */
final class Ring4Atmosphere {
    /** Probability of {@code waterfall_mist} per tried spot. */
    static final float WATERFALL_MIST_PROBABILITY = 0.005F;
    /** How often a bird or frog calls: every 8 s or so (the oasis birds of ring 2: 0.004). */
    static final double WILDLIFE_CHANCE = 0.006;

    private static final Map<ResourceKey<Biome>, Look> LOOKS = Map.of(
            // Start values of the plan (verdant-falls.md §4): a brighter, greener turquoise than cascade_vale's.
            ModBiomes.VERDANT_FALLS, new Look(new BiomeAmbience.Palette(0x86D6E0, 0xD2F0E4, 0x3FD0C0, 0x0F4F4A))
                    .particle(ModParticleTypes.WATERFALL_MIST::get, WATERFALL_MIST_PROBABILITY)
                    .loop(ModSoundEvents.VERDANT_FALLS_FALLS)
                    .additions(ModSoundEvents.VERDANT_FALLS_WILDLIFE, WILDLIFE_CHANCE));

    private Ring4Atmosphere() {
    }

    /**
     * {@code effects} (the bootstrap's colours, grass and foliage) with the ring 4 atmosphere on top: vanilla's cave mood
     * for every ring 4 biome, and for a biome with its own look its colours, particle and sounds.
     *
     * @throws IllegalArgumentException for a biome without ring 4 worldgen or atmosphere
     */
    static BiomeSpecialEffects.Builder effects(ResourceKey<Biome> biome, BiomeSpecialEffects.Builder effects) {
        if (!Ring4Biomes.WITH_TRANSITION.contains(biome) && !LOOKS.containsKey(biome)) {
            throw new IllegalArgumentException("no ring 4 atmosphere for " + biome.location());
        }
        effects.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS);
        Look look = LOOKS.get(biome);
        if (look == null) {
            return effects;
        }
        effects.skyColor(look.colours().sky())
                .fogColor(look.colours().fog())
                .waterColor(look.colours().water())
                .waterFogColor(look.colours().waterFog());
        look.particle().ifPresent(particle -> effects.ambientParticle(new AmbientParticleSettings(particle.type().get(), particle.probability())));
        look.loop().ifPresent(effects::ambientLoopSound);
        look.additions().ifPresent(effects::ambientAdditionsSound);
        return effects;
    }

    /** The own look of a ring 4 biome, empty while it has none yet (until change 80). */
    static Optional<Look> look(ResourceKey<Biome> biome) {
        return Optional.ofNullable(LOOKS.get(biome));
    }

    /**
     * A biome's own atmosphere. The particle type is a supplier: the registry entries exist only once the mod is
     * constructed, after this class may have loaded.
     */
    record Look(BiomeAmbience.Palette colours, Optional<Particle> particle, Optional<Holder<SoundEvent>> loop,
                Optional<AmbientAdditionsSettings> additions) {
        Look(BiomeAmbience.Palette colours) {
            this(colours, Optional.empty(), Optional.empty(), Optional.empty());
        }

        Look particle(Supplier<? extends ParticleOptions> type, float probability) {
            return new Look(colours, Optional.of(new Particle(type, probability)), loop, additions);
        }

        Look loop(Holder<SoundEvent> sound) {
            return new Look(colours, particle, Optional.of(sound), additions);
        }

        Look additions(Holder<SoundEvent> sound, double tickChance) {
            return new Look(colours, particle, loop, Optional.of(new AmbientAdditionsSettings(sound, tickChance)));
        }
    }

    /** An ambient particle and its probability per tried spot. */
    record Particle(Supplier<? extends ParticleOptions> type, float probability) {
    }
}
