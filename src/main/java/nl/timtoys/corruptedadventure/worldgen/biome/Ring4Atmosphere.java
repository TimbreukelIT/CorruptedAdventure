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
 * The atmosphere of the ring 4 overworld biomes in datagen (stream A, change 80), reached through
 * {@link BiomeAmbience#ring4}: every effect of a ring 4 biome (colours, grass and foliage, ambient particle, loop,
 * additions, cave mood); the bootstrap ({@code Ring4Biomes}) only adds climate, spawns and generation.
 * <p>
 * Mythical band: deep, saturated turquoise skies over a pale aqua fog (sparring 2026-10-20, C1-1: the near-white fog
 * made the horizon read flat white) and the lapping of water; {@code mist_fjords} grey-white with dark steel-blue
 * water and a drift of low mist; blossom drifts down in {@code cascade_vale} and the sky gardens. Blighted band: poison
 * green under a greenish sky in a sickly yellow-green fog, quieter (no loop), bubbles from the deep,
 * will-o'-the-wisps over the peat. Dusk, clouds, the blighted haze, the low mist and the distance haze are client
 * hooks ({@code Ring4Sky}, {@code Ring4Fog}): biome data knows neither time of day nor camera height. {@code sundered_reach} carries the blighted look plus its drone: the client fades it to grey across the zone
 * ({@code SunderedFade}), so the zone drains gradually instead of at its edge. Birdsong by day, the night choir, the
 * night glow, the rustling reeds and the glitch specks are client controllers ({@code Ring4Ambience}), since biome data
 * knows no time of day. {@code verdant_falls}: the approved tropical look of the descent, spray, falls and wildlife.
 * <p>
 * A biome particle is tried at about 1,300 random spots around the player per tick, so a probability of 0.001 is some
 * 1.3 a tick.
 */
final class Ring4Atmosphere {
    /** Probability of {@code waterfall_mist} per tried spot in {@code verdant_falls}. */
    static final float WATERFALL_MIST_PROBABILITY = 0.002F;
    /** How often a bird or frog calls in {@code verdant_falls}: every 8 s or so (the oasis birds of ring 2: 0.004). */
    static final double WILDLIFE_CHANCE = 0.006;
    /** Low mist in the fjords: lighter than the falls' spray. */
    static final float FJORD_MIST_PROBABILITY = 0.001F;
    /** Petals in the air under the {@code moonpetal} crowns. */
    static final float BLOSSOM_PROBABILITY = 0.0012F;
    /** Will-o'-the-wisps: here and there, never a swarm (each lives 4–8 s). */
    static final float WISP_PROBABILITY = 0.0001F;
    /** Bubbles from the deep, about every 25 s. */
    static final double BUBBLES_CHANCE = 0.002;

    /** The blighted band's base: poison green water under a greenish sky. */
    private static final BiomeAmbience.Palette BLIGHTED = new BiomeAmbience.Palette(0x9EB27A, 0xBEC276, 0x6FC23A, 0x1E3A12);

    private static final Map<ResourceKey<Biome>, Look> LOOKS = Map.ofEntries(
            Map.entry(ModBiomes.CASCADE_VALE, new Look(new BiomeAmbience.Palette(0x4FC3E8, 0xBFE8EC, 0x8FD8F0, 0x2C6E86), 0x6FCB6A, 0x7FD66E)
                    .particle(ModParticleTypes.BLOSSOM::get, BLOSSOM_PROBABILITY)
                    .loop(ModSoundEvents.RING_4_WATER)),
            Map.entry(ModBiomes.CRYSTAL_LAGOON, new Look(new BiomeAmbience.Palette(0x46C6EC, 0xB6EAEC, 0x2FD3C8, 0x0F5A66), 0x7ED67A, 0x6CCB64)
                    .loop(ModSoundEvents.RING_4_WATER)
                    .additions(ModSoundEvents.RING_4_BUBBLES, BUBBLES_CHANCE)),
            Map.entry(ModBiomes.LOTUS_SHALLOWS, new Look(new BiomeAmbience.Palette(0x58C4E6, 0xCAE8E4, 0x9ADFF2, 0x356F82), 0x86D47A, 0x7AC96C)
                    .loop(ModSoundEvents.RING_4_WATER)),
            Map.entry(ModBiomes.GLOWROOT_SWAMP, new Look(new BiomeAmbience.Palette(0x52BAD0, 0xB2DECE, 0x4CC08A, 0x16503A), 0x5FAE5A, 0x4E9E4C)
                    .loop(ModSoundEvents.RING_4_WATER)),
            Map.entry(ModBiomes.MIST_FJORDS, new Look(new BiomeAmbience.Palette(0x86ACC4, 0xC2D0D8, 0x2F5A7A, 0x0E2234), 0x4F8F4A, 0x3F7A44)
                    .particle(ModParticleTypes.WATERFALL_MIST::get, FJORD_MIST_PROBABILITY)
                    .loop(ModSoundEvents.RING_4_WATER)
                    .additions(ModSoundEvents.RING_4_BUBBLES, BUBBLES_CHANCE)),
            Map.entry(ModBiomes.DROWNED_FOREST, new Look(new BiomeAmbience.Palette(0x92AE7E, 0xAEBC74, 0x3E8A3A, 0x0E2A10), 0x7A8A5A, 0x6A7A4A)
                    .particle(ModParticleTypes.WILL_O_WISP::get, WISP_PROBABILITY / 2)
                    .additions(ModSoundEvents.RING_4_BUBBLES, BUBBLES_CHANCE)),
            Map.entry(ModBiomes.PEAT_MOORS, new Look(BLIGHTED, 0x8A8E5A, 0x7A7E4E)
                    .particle(ModParticleTypes.WILL_O_WISP::get, WISP_PROBABILITY)
                    .additions(ModSoundEvents.RING_4_BUBBLES, BUBBLES_CHANCE)),
            Map.entry(ModBiomes.REED_MARSH, new Look(new BiomeAmbience.Palette(0x9AB482, 0xB8C27A, 0x5DB03A, 0x1A3614), 0x8E9A5C, 0x7C8A50)),
            Map.entry(ModBiomes.SKY_GARDENS, new Look(new BiomeAmbience.Palette(0x48C2F0, 0xC2E8F2, 0x8FE0F4, 0x2E7890), 0x7CDA74, 0x88E07A)
                    .particle(ModParticleTypes.BLOSSOM::get, BLOSSOM_PROBABILITY)
                    .loop(ModSoundEvents.RING_4_WATER)),
            Map.entry(ModBiomes.WITHERED_GARDENS, new Look(new BiomeAmbience.Palette(0xA4B488, 0xC4C88E, 0x6FA048, 0x22381A), 0x9A9466, 0x8A8458)),
            // The blighted look, a little darker and fainter; the client greys it across the zone (SunderedFade).
            Map.entry(ModBiomes.SUNDERED_REACH, new Look(new BiomeAmbience.Palette(0x9AA884, 0xB2B684, 0x4E8A3A, 0x16300F),
                    BiomeAmbience.desaturate(0x8A8E5A, 0.4F), BiomeAmbience.desaturate(0x7A7E4E, 0.4F))
                    .loop(ModSoundEvents.RING_4_SUNDERED_DRONE)),
            // Start values of the plan (verdant-falls.md §4): a brighter, greener turquoise than cascade_vale's.
            Map.entry(ModBiomes.VERDANT_FALLS, new Look(new BiomeAmbience.Palette(0x64CCE2, 0xBCEAD8, 0x3FD0C0, 0x0F4F4A), 0x6CC24A, 0x55B03A)
                    .particle(ModParticleTypes.WATERFALL_MIST::get, WATERFALL_MIST_PROBABILITY)
                    .loop(ModSoundEvents.VERDANT_FALLS_FALLS)
                    .additions(ModSoundEvents.VERDANT_FALLS_WILDLIFE, WILDLIFE_CHANCE)));

    private Ring4Atmosphere() {
    }

    /** @throws IllegalArgumentException for a biome without ring 4 atmosphere */
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

    /** @throws IllegalArgumentException for a biome without ring 4 atmosphere */
    static Look look(ResourceKey<Biome> biome) {
        Look look = LOOKS.get(biome);
        if (look == null) {
            throw new IllegalArgumentException("no ring 4 atmosphere for " + biome.location());
        }
        return look;
    }

    /**
     * A biome's atmosphere: colours, grass, foliage and the optional particle and sounds. The particle type is a
     * supplier: the registry entries exist only once the mod is constructed, after this class may have loaded.
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
