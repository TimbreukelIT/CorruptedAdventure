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
 * Datagen bootstraps (configured + placed features) of ring 3 and its one global feature order. Owned by stream
 * W-biomes; the feature classes per package ({@link Ring3OreFeatures}, …) are called from here, so
 * {@code DataGenerators} stays untouched.
 * <p>
 * {@link #ORDER} lists every biome-own ring 3 feature per generation step in one order; each biome gets the subset that
 * names it, in that order ({@code Ring3Biomes.generation}). One list keeps vanilla's feature sorter free of cycles
 * ("Feature order cycle found"). Features a biome modifier adds ({@code add_ring_3_ores}) come after the biome's own in
 * their step.
 */
public final class Ring3Features {
    /** A biome-own feature: its step and the ring 3 biomes that place it. */
    public record Entry(ResourceKey<PlacedFeature> feature, GenerationStep.Decoration step, Set<ResourceKey<Biome>> biomes) {
        public Entry {
            biomes = Set.copyOf(biomes);
        }
    }

    /** Every biome-own ring 3 feature in the global order (within a step, list order = placement order). */
    public static final List<Entry> ORDER = List.of(
            new Entry(Ring3OreFeatures.SULFUR_ORE_RICH, GenerationStep.Decoration.UNDERGROUND_ORES, Set.of(ModBiomes.SULFUR_FIELDS)));

    private Ring3Features() {
    }

    /** The biome-own features of {@code biome} in {@code step}, in the global order. */
    public static List<ResourceKey<PlacedFeature>> features(ResourceKey<Biome> biome, GenerationStep.Decoration step) {
        return ORDER.stream().filter(entry -> entry.step() == step && entry.biomes().contains(biome)).map(Entry::feature).toList();
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        Ring3OreFeatures.configured(context);
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        Ring3OreFeatures.placed(context);
    }
}
