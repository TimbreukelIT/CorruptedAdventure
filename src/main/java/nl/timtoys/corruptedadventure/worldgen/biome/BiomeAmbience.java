package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.neoforged.neoforge.common.world.BiomeSpecialEffectsBuilder;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

import java.util.Optional;

/**
 * Atmosphere of biomes in datagen: cave mood profiles, rebuilding a biome with another mood, and faded colours.
 * <p>
 * The mood sound is vanilla's cave sound mechanism ({@code BiomeAmbientSoundsHandler}, client): every tick a random
 * block within {@code block_search_extent} without sky light adds about {@code 1 / tick_delay}, at 1 the sound plays.
 * So it only builds up underground, and a biome without a mood is only different there.
 */
public final class BiomeAmbience {
    /** No cave sound at all. */
    public static final Optional<AmbientMoodSettings> SILENT = Optional.empty();
    /**
     * Distorted cave sound, about every 1.5 minutes in the dark (vanilla: 5 minutes), sampled and played further away
     * than vanilla's (extent 8, offset 2).
     */
    public static final Optional<AmbientMoodSettings> STRANGE =
            Optional.of(new AmbientMoodSettings(ModSoundEvents.RING_1_STRANGE_CAVE, 1800, 12, 3.0));

    private BiomeAmbience() {
    }

    /**
     * Effects of {@code dripshroom_forest}: grey-green mist, a constant drip loop, single drops and slowly drifting
     * spores. No mood sound and no music: the biome bootstrap sets those. Fog distance is vanilla Nether's (not biome data).
     */
    public static BiomeSpecialEffects.Builder dripshroomForestEffects() {
        return netherRing2Effects(Palette.DRIPSHROOM_FOREST, 0.01, ParticleTypes.SPORE_BLOSSOM_AIR, 0.0005F);
    }

    /** Effects of {@code spore_caves}: darker mist, wetter (twice the drops) and glowing spores. */
    public static BiomeSpecialEffects.Builder sporeCavesEffects() {
        return netherRing2Effects(Palette.SPORE_CAVES, 0.02, ParticleTypes.GLOW, 0.004F);
    }

    private static BiomeSpecialEffects.Builder netherRing2Effects(Palette palette, double dropChance, ParticleOptions particle, float probability) {
        return new BiomeSpecialEffects.Builder()
                .skyColor(palette.sky())
                .fogColor(palette.fog())
                .waterColor(palette.water())
                .waterFogColor(palette.waterFog())
                .ambientLoopSound(ModSoundEvents.NETHER_RING_2_DRIP_LOOP)
                .ambientAdditionsSound(new AmbientAdditionsSettings(ModSoundEvents.NETHER_RING_2_DRIPS, dropChance))
                .ambientParticle(new AmbientParticleSettings(particle, probability));
    }

    // Ring 3 and Nether ring 3 (stream A, packages 50 and 56): the biome bootstraps take every effect from these hooks,
    // so the atmosphere changes in Ring3Atmosphere / NetherRing3Atmosphere without touching the biome classes.

    /**
     * Effects of a ring 3 overworld biome ({@code Ring3Biomes.ALL} and the transition {@code steam_fens}): sky, fog,
     * water, grass and foliage and the cave mood; particles and sounds follow in package 50. Only effects: the biome
     * bootstrap sets climate (a wet {@code steam_fens} its own), spawns and generation.
     *
     * @throws IllegalArgumentException for a biome without ring 3 atmosphere
     */
    public static BiomeSpecialEffects.Builder ring3(ResourceKey<Biome> biome) {
        return Ring3Atmosphere.effects(biome);
    }

    /**
     * Effects of a Nether ring 3 biome ({@code frozen_lava_sea}, {@code rime_forest}, {@code glacier_rifts}): fog and
     * water colours and the cave mood; particles and sounds follow in package 56. The biome bootstrap only adds the
     * music. Fog distance is not biome data.
     *
     * @throws IllegalArgumentException for a biome without Nether ring 3 atmosphere
     */
    public static BiomeSpecialEffects.Builder netherRing3(ResourceKey<Biome> biome) {
        return NetherRing3Atmosphere.effects(biome);
    }

    /** A builder with only the four colours of {@code palette}. */
    static BiomeSpecialEffects.Builder colours(Palette palette) {
        return new BiomeSpecialEffects.Builder()
                .skyColor(palette.sky())
                .fogColor(palette.fog())
                .waterColor(palette.water())
                .waterFogColor(palette.waterFog());
    }

    /** The same biome with only its mood sound replaced: climate, generation, spawns and every other effect are kept. */
    public static Biome withMood(Biome biome, Optional<AmbientMoodSettings> mood) {
        Biome.ClimateSettings climate = biome.getModifiedClimateSettings();
        return new Biome.BiomeBuilder()
                .hasPrecipitation(climate.hasPrecipitation())
                .temperature(climate.temperature())
                .temperatureAdjustment(climate.temperatureModifier())
                .downfall(climate.downfall())
                .specialEffects(new MoodEffectsBuilder(biome.getSpecialEffects(), mood).build())
                .mobSpawnSettings(biome.getMobSettings())
                .generationSettings(biome.getGenerationSettings())
                .build();
    }

    /** Mixes each channel of {@code rgb} towards its grey value (luma): 0 keeps the colour, 1 gives grey. */
    public static int desaturate(int rgb, float amount) {
        int red = rgb >> 16 & 0xFF;
        int green = rgb >> 8 & 0xFF;
        int blue = rgb & 0xFF;
        float grey = 0.299F * red + 0.587F * green + 0.114F * blue;
        return mix(red, grey, amount) << 16 | mix(green, grey, amount) << 8 | mix(blue, grey, amount);
    }

    /** Mixes each channel of {@code rgb} towards black: 0 keeps the colour, 1 gives black. */
    public static int darken(int rgb, float amount) {
        return Math.round((rgb >> 16 & 0xFF) * (1 - amount)) << 16 | Math.round((rgb >> 8 & 0xFF) * (1 - amount)) << 8
                | Math.round((rgb & 0xFF) * (1 - amount));
    }

    private static int mix(int channel, float grey, float amount) {
        return Math.round(channel + (grey - channel) * amount);
    }

    /** Sky, fog and water colours of a biome. */
    public record Palette(int sky, int fog, int water, int waterFog) {
        /** Vanilla plains (temperature 0.8, normal water): the reference colours of ring 1. */
        public static final Palette RING_1 = new Palette(0x78A7FF, 0xC0D8FF, 0x3F76E4, 0x050533);
        /** Ring 2 base: pale yellowish-blue sky, sandy fog, turquoise water. Every biome below is a nuance of it. */
        public static final Palette RING_2 = new Palette(0xAEC8E0, 0xE4D2A6, 0x32A598, 0x0B3A35);
        /** Red-orange dust of the canyon stone on the horizon. */
        public static final Palette CANYONLANDS = RING_2.withFog(0xE2BC93);
        /** Fresher and a touch bluer: the safe place. */
        public static final Palette OASIS_FLATS = RING_2.withSky(0x9EC5EC).withFog(0xE8DDBA);
        /** Dazzling, almost white; the brine pools are a milky, lighter turquoise against the crust. */
        public static final Palette SALT_FLATS = RING_2.withSky(0xBCD3E8).withFog(0xEEEAE0).withWater(0x5AC8B8, 0x1B5048);
        /** Ochre. */
        public static final Palette HOODOO_FIELDS = RING_2.withFog(0xDDC291);
        /** Golden yellow, the warmest. */
        public static final Palette DUNE_SEA = RING_2.withSky(0xB4CADA).withFog(0xEACF96);
        /** Ashen and scorched: the ring 2 palette 35 % towards grey and 10 % darker, a foretaste of ring 3. */
        public static final Palette DRY_RIVERBEDS = RING_2.desaturated(0.35F).darkened(0.1F);

        /** Nether ring 2 base: grey-green, damp, medium dark fog (reads as mist, not as darkness); no sky in the Nether. */
        public static final Palette NETHER_RING_2 = new Palette(0x000000, 0x56645A, 0x3D8C7A, 0x0F2A24);
        /** The main biome: the base fog. */
        public static final Palette DRIPSHROOM_FOREST = NETHER_RING_2;
        /** Darker and bluer, so the glowing spores stand out. */
        public static final Palette SPORE_CAVES = NETHER_RING_2.withFog(0x3E5A50);

        public Palette desaturated(float amount) {
            return new Palette(desaturate(sky, amount), desaturate(fog, amount), desaturate(water, amount), desaturate(waterFog, amount));
        }

        public Palette darkened(float amount) {
            return new Palette(darken(sky, amount), darken(fog, amount), darken(water, amount), darken(waterFog, amount));
        }

        public Palette withSky(int sky) {
            return new Palette(sky, fog, water, waterFog);
        }

        public Palette withFog(int fog) {
            return new Palette(sky, fog, water, waterFog);
        }

        public Palette withWater(int water, int waterFog) {
            return new Palette(sky, fog, water, waterFog);
        }
    }

    /** Copies every effect but the mood; vanilla's builder can set a mood but never clear one. */
    private static final class MoodEffectsBuilder extends BiomeSpecialEffectsBuilder {
        private MoodEffectsBuilder(BiomeSpecialEffects base, Optional<AmbientMoodSettings> mood) {
            super(base.getFogColor(), base.getWaterColor(), base.getWaterFogColor(), base.getSkyColor());
            foliageColorOverride = base.getFoliageColorOverride();
            grassColorOverride = base.getGrassColorOverride();
            grassColorModifier = base.getGrassColorModifier();
            ambientParticle = base.getAmbientParticleSettings();
            ambientLoopSoundEvent = base.getAmbientLoopSoundEvent();
            ambientMoodSettings = mood;
            ambientAdditionsSettings = base.getAmbientAdditionsSettings();
            backgroundMusic = base.getBackgroundMusic();
        }
    }
}
