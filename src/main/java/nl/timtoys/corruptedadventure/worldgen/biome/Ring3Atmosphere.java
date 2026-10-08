package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import nl.timtoys.corruptedadventure.registry.ModBiomes;

import java.util.Map;

/**
 * The atmosphere of the ring 3 overworld biomes in datagen (stream A, packages 50-0 and 50): per biome its colours and
 * later its particles and sounds. Only effects: climate, spawns and generation belong to the biome bootstrap (stream
 * W-biomes), which reaches this through {@link BiomeAmbience#ring3}.
 */
final class Ring3Atmosphere {
    /** Ash-red base of the ring (sky, fog, water, water fog); each biome a nuance of it. Provisional until package 50. */
    static final BiomeAmbience.Palette RING_3 = new BiomeAmbience.Palette(0xA8705C, 0x8A6A5A, 0x5E6A66, 0x1E2422);
    /** Grass and foliage of the ash: grey olive. */
    private static final int ASH_GRASS = 0x8A8060;
    private static final int ASH_FOLIAGE = 0x7A7050;

    /** Per biome (change 39b's colours, moved here unchanged in 50-0): the dormant caldera is green, the steam fens in between. */
    private static final Map<ResourceKey<Biome>, Look> LOOKS = Map.of(
            ModBiomes.ASHEN_WASTES, ash(RING_3.withSky(0x9A8C86).withFog(0x7E7470)),
            ModBiomes.VOLCANIC_PEAKS, ash(RING_3.withSky(0xB4553C).withFog(0x8C4A3A)),
            ModBiomes.EMBER_WOODS, ash(RING_3.withSky(0xA8644A).withFog(0x7A4636)),
            ModBiomes.BASALT_FLOWS, ash(RING_3.withSky(0x8E6A60).withFog(0x5E4C48)),
            ModBiomes.SULFUR_FIELDS, ash(RING_3.withSky(0xB4A060).withFog(0xA89A62)),
            ModBiomes.DRIED_SEABED, ash(RING_3.withSky(0xB89A84).withFog(0xA89484)),
            ModBiomes.TITAN_FIELDS, ash(RING_3.withSky(0x8C7A90).withFog(0x6E6478)),
            ModBiomes.DORMANT_CALDERA, new Look(RING_3.withSky(0x9CB0A0).withFog(0xA4B49C), 0x6FA040, 0x5E9032),
            // The transition 3→4 (package 48): misty grey-green, warm murky water.
            ModBiomes.STEAM_FENS, new Look(RING_3.withSky(0x94A098).withFog(0xA0A8A2).withWater(0x4E7A70, 0x1A2E2A), 0x7C8C58, 0x6C7C48));

    private Ring3Atmosphere() {
    }

    /** @throws IllegalArgumentException for a biome without ring 3 atmosphere */
    static BiomeSpecialEffects.Builder effects(ResourceKey<Biome> biome) {
        Look look = look(biome);
        return BiomeAmbience.colours(look.colours())
                .grassColorOverride(look.grass())
                .foliageColorOverride(look.foliage())
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS);
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

    /** Sky, fog and water colours plus grass and foliage of one biome. */
    record Look(BiomeAmbience.Palette colours, int grass, int foliage) {
    }
}
