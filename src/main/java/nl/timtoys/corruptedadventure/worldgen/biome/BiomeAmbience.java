package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.world.level.biome.AmbientMoodSettings;
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

    private static int mix(int channel, float grey, float amount) {
        return Math.round(channel + (grey - channel) * amount);
    }

    /** Sky, fog and water colours of a biome. */
    public record Palette(int sky, int fog, int water, int waterFog) {
        /** Vanilla plains (temperature 0.8, normal water): the reference colours of ring 1. */
        public static final Palette RING_1 = new Palette(0x78A7FF, 0xC0D8FF, 0x3F76E4, 0x050533);

        public Palette desaturated(float amount) {
            return new Palette(desaturate(sky, amount), desaturate(fog, amount), desaturate(water, amount), desaturate(waterFog, amount));
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
