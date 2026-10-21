package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Datagen bootstrap for the ring 5 overworld biomes ({@code docs/plans/ring-5/contract.md} §2, package 86): three in the
 * inner (dead) band, three in the outer band that falls away into the void. Owned by stream W-biomes.
 * <p>
 * Where they lie is decided by the {@code multi_noise} source of ring 5 in the world preset (V12-A, no Java hook): the
 * router's {@code weirdness} (= {@code ring_5/overworld/band}) splits the inner band (below 0) from the outer one (0 and
 * above, as {@code Ring5Band.outer}); inside, {@code humidity} (= {@code region_glitch}) and {@code temperature}
 * (= {@code region_rot}) pick the inner biome, {@code erosion} (= {@code breakup}) and {@code temperature} the outer one.
 * The zone 4→5 keeps ring 4's {@code sundered_reach}; there is no transition biome of ring 5's own.
 * <p>
 * Golf 1 is a skeleton: colours, climate, spawns. No carvers (caves are 93), no features (94/95), no biome modifiers;
 * the sound, particles and the sky beyond the biome colours are the atmosphere's (stream A, 98).
 */
public final class Ring5Biomes {
    /** The inner band (dead), in the order of the preset. */
    public static final List<ResourceKey<Biome>> INNER = List.of(ModBiomes.CORRUPTED_WASTES, ModBiomes.GLITCH_FIELDS, ModBiomes.FALSE_MEADOW);
    /** The outer band (gone), in the order of the preset. */
    public static final List<ResourceKey<Biome>> OUTER = List.of(ModBiomes.VOID_SCARS, ModBiomes.INVERTED_HILLS, ModBiomes.SHATTERED_EXPANSE);
    /** Every ring 5 biome: exactly the preset's ring 5 {@code multi_noise}. */
    public static final List<ResourceKey<Biome>> ALL = Stream.concat(INNER.stream(), OUTER.stream()).toList();
    /** The ring 5 biomes without ambience, music or mobs (tag {@code silent}; the Nether-5 biomes are W-nether's). */
    public static final List<ResourceKey<Biome>> SILENT = List.of(ModBiomes.FALSE_MEADOW);

    /** Never snows; never rains either: the ring weather renders its own precipitation (stream A). */
    static final float TEMPERATURE = 0.5F;
    static final float DOWNFALL = 0.0F;

    /**
     * Colours per biome: the inner band grey-violet, the outer one going to black (contract §6). {@code false_meadow} is
     * vanilla plains exactly, grass and foliage of plains' colour map included (its own climate would give other ones).
     * Stars, the thick fog and the black day sky are client atmosphere (A, 98), not biome data.
     */
    private static final Map<ResourceKey<Biome>, Look> LOOKS = Map.of(
            ModBiomes.CORRUPTED_WASTES, new Look(new BiomeAmbience.Palette(0x5E5868, 0x8C8A92, 0x3E3A48, 0x1A1820), 0x7E7C80, 0x6E6C72),
            ModBiomes.GLITCH_FIELDS, new Look(new BiomeAmbience.Palette(0x5A4E6E, 0x857C94, 0x4A3A66, 0x1E1430), 0x7C7488, 0x6C6478),
            ModBiomes.FALSE_MEADOW, new Look(BiomeAmbience.Palette.RING_1, 0x91BD59, 0x77AB2F),
            ModBiomes.VOID_SCARS, new Look(new BiomeAmbience.Palette(0x1E1A26, 0x3A3640, 0x2A2632, 0x0A080E), 0x5E5C62, 0x54525A),
            ModBiomes.INVERTED_HILLS, new Look(new BiomeAmbience.Palette(0x221C2E, 0x403A4A, 0x2E2838, 0x0C0A10), 0x66606E, 0x5A5462),
            ModBiomes.SHATTERED_EXPANSE, new Look(new BiomeAmbience.Palette(0x08060C, 0x141218, 0x1A1820, 0x040306), 0x4E4C52, 0x44424A));

    private Ring5Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        for (ResourceKey<Biome> key : ALL) {
            context.register(key, new Biome.BiomeBuilder()
                    .hasPrecipitation(false)
                    .temperature(TEMPERATURE)
                    .downfall(DOWNFALL)
                    .specialEffects(effects(key).build())
                    .mobSpawnSettings(spawns(key))
                    // Nothing generates yet: no carvers (93), no features (94/95).
                    .generationSettings(new BiomeGenerationSettings.Builder(features, carvers).build())
                    .build());
        }
    }

    /** The look of a ring 5 biome; throws for any other biome. */
    static Look look(ResourceKey<Biome> biome) {
        Look look = LOOKS.get(biome);
        if (look == null) {
            throw new IllegalArgumentException("no ring 5 look for " + biome);
        }
        return look;
    }

    /** The colours of a ring 5 biome: sky, fog, water, grass and foliage. */
    static BiomeSpecialEffects.Builder effects(ResourceKey<Biome> biome) {
        Look look = look(biome);
        return BiomeAmbience.colours(look.colours())
                .grassColorOverride(look.grass())
                .foliageColorOverride(look.foliage());
    }

    /**
     * Contract §2: no animals (dead); vanilla's monsters until phase 4, without bats and glow squids (nothing lives here,
     * and ring 5 is dry); {@code false_meadow} nothing at all. Throws for any other biome.
     */
    static MobSpawnSettings spawns(ResourceKey<Biome> biome) {
        if (!ALL.contains(biome)) {
            throw new IllegalArgumentException("no ring 5 spawns for " + biome);
        }
        MobSpawnSettings.Builder spawns = new MobSpawnSettings.Builder();
        if (!SILENT.contains(biome)) {
            // The monster weights of vanilla's commonSpawns, without its cave spawns.
            BiomeDefaultFeatures.monsters(spawns, 95, 5, 100, false);
        }
        return spawns.build();
    }

    /** The colours of one biome: the four of the palette, grass and foliage. */
    record Look(BiomeAmbience.Palette colours, int grass, int foliage) {
    }
}
