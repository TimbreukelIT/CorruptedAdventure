package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;

import java.util.List;
import java.util.Set;

/**
 * Datagen bootstraps (configured + placed features) of ring 4 and its one global feature order. Owned by stream
 * W-biomes ({@code docs/plans/ring-4/waves.md}); the feature classes per biome ({@link VerdantFallsFeatures}, …) are
 * called from here, so {@code DataGenerators} stays untouched.
 * <p>
 * {@link #ORDER} lists every biome-own ring 4 feature per generation step in one order, as {@code Ring3Features.ORDER}
 * does for ring 3; each biome gets the subset that names it, in that order ({@code Ring4Biomes.generation}), which keeps
 * vanilla's feature sorter free of cycles.
 */
public final class Ring4Features {
    /** A biome-own feature: its step and the ring 4 biomes that place it. */
    public record Entry(ResourceKey<PlacedFeature> feature, GenerationStep.Decoration step, Set<ResourceKey<Biome>> biomes) {
        public Entry {
            biomes = Set.copyOf(biomes);
        }
    }

    /** Every biome-own ring 4 feature in the global order (within a step, list order = placement order). */
    public static final List<Entry> ORDER = List.of(
            // verdant_falls: the moss carpets on the treads, the climbing strands under the ledges.
            new Entry(VerdantFallsFeatures.VERDANT_FALLS_MOSS_CARPETS, GenerationStep.Decoration.VEGETAL_DECORATION, Set.of(ModBiomes.VERDANT_FALLS)),
            new Entry(VerdantFallsFeatures.VERDANT_FALLS_LEDGE_STRANDS, GenerationStep.Decoration.VEGETAL_DECORATION, Set.of(ModBiomes.VERDANT_FALLS)));

    private Ring4Features() {
    }

    /** The biome-own features of {@code biome} in {@code step}, in the global order. */
    public static List<ResourceKey<PlacedFeature>> features(ResourceKey<Biome> biome, GenerationStep.Decoration step) {
        return ORDER.stream().filter(entry -> entry.step() == step && entry.biomes().contains(biome)).map(Entry::feature).toList();
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        VerdantFallsFeatures.configured(context);
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        VerdantFallsFeatures.placed(context);
    }
}
