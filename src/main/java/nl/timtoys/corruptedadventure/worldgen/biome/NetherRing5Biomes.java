package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
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

/**
 * Datagen bootstrap for the two biomes of Nether ring 5 (87; {@code docs/plans/ring-5/contract.md} §2, V10-A): the white
 * halls (the main biome: white plains with grids of columns under a flat ceiling, a mirror-stone floor) and the hollow
 * cathedral (a vast empty white space with one giant column). Owned by stream W-nether. Which lies where is decided by
 * the {@code multi_noise} source of Nether ring 5 in the world preset on the router's {@code temperature} (=
 * {@code ring_5/nether/region}, the cathedral mask of {@code nether_cathedrals}) at {@link #CATHEDRAL_THRESHOLD}.
 * <p>
 * Silent and empty: no spawns (contract §2), no carvers (nothing may open below the floor), no features yet (96), no
 * music, no ambient sound or particles. The effects are temporary (white fog, black sky) until the atmosphere's hook in
 * {@link BiomeAmbience} (stream A, 98).
 */
public final class NetherRing5Biomes {
    /** Every biome of Nether ring 5, the only Java list of them (tags {@code is_nether_ring_5}, {@code silent}, {@code c:}). */
    public static final List<ResourceKey<Biome>> ALL = List.of(ModBiomes.WHITE_HALLS, ModBiomes.HOLLOW_CATHEDRAL);

    /** The cathedral mask at and above which the biome is {@code hollow_cathedral} (the middle of its rim). */
    public static final double CATHEDRAL_THRESHOLD = 0.5;
    /** Neither hot nor cold, no precipitation: a still, white world. */
    static final float TEMPERATURE = 0.5F;
    /** Temporary fog (until 98): white in the halls, whiter in a cathedral. */
    static final int WHITE_HALLS_FOG = 0xE8E8EC;
    static final int HOLLOW_CATHEDRAL_FOG = 0xF4F4F6;
    private static final int SKY = 0x000000;
    // Vanilla's water colours: Nether ring 5 has no water, but the builder needs them.
    private static final int WATER = 0x3F76E4;
    private static final int WATER_FOG = 0x050533;

    private NetherRing5Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);
        for (ResourceKey<Biome> key : ALL) {
            context.register(key, new Biome.BiomeBuilder()
                    .hasPrecipitation(false)
                    .temperature(TEMPERATURE)
                    .downfall(0.0F)
                    .specialEffects(effects(key))
                    // No spawns and no carvers; the features of 96 (the white core, still light) come later.
                    .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                    .generationSettings(new BiomeGenerationSettings.Builder(features, carvers).build())
                    .build());
        }
    }

    /** White fog and a black sky; the silent music and the rest from the atmosphere's hook (98). */
    static BiomeSpecialEffects effects(ResourceKey<Biome> biome) {
        int fog = biome == ModBiomes.HOLLOW_CATHEDRAL ? HOLLOW_CATHEDRAL_FOG : WHITE_HALLS_FOG;
        return BiomeAmbience.netherRing5(biome, new BiomeSpecialEffects.Builder()
                .fogColor(fog)
                .skyColor(SKY)
                .waterColor(WATER)
                .waterFogColor(WATER_FOG))
                .build();
    }
}
