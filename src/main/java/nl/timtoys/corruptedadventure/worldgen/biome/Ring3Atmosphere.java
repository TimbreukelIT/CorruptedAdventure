package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
 * The atmosphere of the ring 3 overworld biomes in datagen (stream A, package 50): per biome its colours, ambient
 * particle and sounds. Only effects: climate, spawns and generation belong to the biome bootstrap (stream W-biomes),
 * which reaches this through {@link BiomeAmbience#ring3}.
 * <p>
 * Colours (G2-A2 = A): one ash-red ring with a clear tint per biome, so each is recognisable and the ring stays one.
 * Particles (G2-A3 = A): subtle to medium, denser in the ember woods and the steam fens. A biome's ambient particle is
 * tried at about 1,300 random spots around the player per tick, so a probability of 0.005 is some 7 particles per tick
 * (vanilla: soul sand valley 0.00625, basalt deltas 0.118). Sounds: a loop where the biome has a constant voice, lava
 * additions where lava is near, vanilla's cave mood everywhere; the rumble outside the weather is client code.
 */
final class Ring3Atmosphere {
    /** Ash-red base of the ring (sky, fog, water, water fog); each biome a nuance of it. */
    static final BiomeAmbience.Palette RING_3 = new BiomeAmbience.Palette(0xA8705C, 0x8A6A5A, 0x5E6A66, 0x1E2422);
    /** Grass and foliage of the ash: grey olive. */
    private static final int ASH_GRASS = 0x8A8060;
    private static final int ASH_FOLIAGE = 0x7A7050;
    /** How often lava bubbles where it is near (vanilla's basalt deltas additions: 0.0111 per tick). */
    static final double PEAKS_LAVA_CHANCE = 0.006;
    static final double FLOWS_LAVA_CHANCE = 0.004;

    private static final Map<ResourceKey<Biome>, Look> LOOKS = Map.of(
            // Grey-beige ash under a dusty sky; falling ash.
            ModBiomes.ASHEN_WASTES, ash(RING_3.withSky(0x9A8E88).withFog(0x847A74))
                    .particle(ModParticleTypes.DRIFTING_ASH::get, 0.006F),
            // Red-orange, the hottest; ash (sparks come from the client) and bubbling lava.
            ModBiomes.VOLCANIC_PEAKS, ash(RING_3.withSky(0xBC5838).withFog(0x94482F))
                    .particle(ModParticleTypes.DRIFTING_ASH::get, 0.004F)
                    .additions(ModSoundEvents.RING_3_LAVA, PEAKS_LAVA_CHANCE),
            // Warm amber; rising sparks and the crackle of smouldering wood.
            ModBiomes.EMBER_WOODS, ash(RING_3.withSky(0xB07440).withFog(0x84522E))
                    .particle(ModParticleTypes.RISING_EMBER::get, 0.008F)
                    .loop(ModSoundEvents.RING_3_EMBERS),
            // Dark red-brown; a spark now and then, lava nearby.
            ModBiomes.BASALT_FLOWS, ash(RING_3.withSky(0x7C5448).withFog(0x563C34))
                    .particle(ModParticleTypes.RISING_EMBER::get, 0.0015F)
                    .additions(ModSoundEvents.RING_3_LAVA, FLOWS_LAVA_CHANCE),
            // Yellow-green; drifting sulphurous steam.
            ModBiomes.SULFUR_FIELDS, ash(RING_3.withSky(0xAAAE62).withFog(0xA2A45E))
                    .particle(ModParticleTypes.SULFUR_STEAM::get, 0.004F),
            // Pale sand-blue sky over a sandy haze; dust.
            ModBiomes.DRIED_SEABED, ash(RING_3.withSky(0xA6B2BA).withFog(0xB4AA96))
                    .particle(() -> ParticleTypes.WHITE_ASH, 0.004F),
            // Cool violet, the soul fire's contrast; soul sparks and the titans' hum.
            ModBiomes.TITAN_FIELDS, ash(RING_3.withSky(0x8A7CA8).withFog(0x6A6290))
                    .particle(() -> ParticleTypes.SOUL, 0.0025F)
                    .loop(ModSoundEvents.RING_3_TITAN_HUM),
            // Brighter and greener, the resting place: soft pollen and quiet.
            ModBiomes.DORMANT_CALDERA, new Look(RING_3.withSky(0x98C0AA).withFog(0xAAC6A2), 0x6FA040, 0x5E9032)
                    .particle(() -> ParticleTypes.SPORE_BLOSSOM_AIR, 0.002F)
                    .loop(ModSoundEvents.RING_3_CALDERA),
            // The transition 3→4 (package 48): misty grey-green, warm murky water, thick steam.
            ModBiomes.STEAM_FENS, new Look(RING_3.withSky(0x94A49C).withFog(0xA6B0AA).withWater(0x4E7A70, 0x1A2E2A), 0x7C8C58, 0x6C7C48)
                    .particle(ModParticleTypes.STEAM_PUFF::get, 0.01F));

    private Ring3Atmosphere() {
    }

    /** @throws IllegalArgumentException for a biome without ring 3 atmosphere */
    static BiomeSpecialEffects.Builder effects(ResourceKey<Biome> biome) {
        Look look = look(biome);
        BiomeSpecialEffects.Builder effects = BiomeAmbience.colours(look.colours())
                .grassColorOverride(look.grass())
                .foliageColorOverride(look.foliage())
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS);
        look.particle().ifPresent(particle -> effects.ambientParticle(new AmbientParticleSettings(particle.type().get(), particle.probability())));
        look.loop().ifPresent(effects::ambientLoopSound);
        look.additions().ifPresent(effects::ambientAdditionsSound);
        return effects;
    }

    /** @throws IllegalArgumentException for a biome without ring 3 atmosphere */
    static Look look(ResourceKey<Biome> biome) {
        Look look = LOOKS.get(biome);
        if (look == null) {
            throw new IllegalArgumentException("no ring 3 atmosphere for " + biome.location());
        }
        return look;
    }

    private static Look ash(BiomeAmbience.Palette colours) {
        return new Look(colours, ASH_GRASS, ASH_FOLIAGE);
    }

    /**
     * Everything of one biome's atmosphere. The particle type is a supplier: the registry entries exist only once the
     * mod is constructed, after this class may have loaded.
     */
    record Look(BiomeAmbience.Palette colours, int grass, int foliage, Optional<Particle> particle,
                Optional<Holder<SoundEvent>> loop, Optional<AmbientAdditionsSettings> additions) {
        Look(BiomeAmbience.Palette colours, int grass, int foliage) {
            this(colours, grass, foliage, Optional.empty(), Optional.empty(), Optional.empty());
        }

        Look particle(Supplier<? extends ParticleOptions> type, float probability) {
            return new Look(colours, grass, foliage, Optional.of(new Particle(type, probability)), loop, additions);
        }

        Look loop(Holder<SoundEvent> sound) {
            return new Look(colours, grass, foliage, particle, Optional.of(sound), additions);
        }

        Look additions(Holder<SoundEvent> sound, double tickChance) {
            return new Look(colours, grass, foliage, particle, loop, Optional.of(new AmbientAdditionsSettings(sound, tickChance)));
        }
    }

    /** An ambient particle and its probability per tried spot. */
    record Particle(Supplier<? extends ParticleOptions> type, float probability) {
    }
}
