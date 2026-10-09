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
 * W-biomes; the feature classes per package ({@link Ring3OreFeatures}, {@link Ring3DressingFeatures},
 * {@link TitanFieldsFeatures}, {@link Ring3HorrorFeatures}, …) are called from here, so {@code DataGenerators} stays
 * untouched.
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
            // Change 47: the dressing on the ground, then the ash drifts around what stands (never on it).
            local(Ring3DressingFeatures.SULFUR_FIELDS_MOUND_FUMAROLES, ModBiomes.SULFUR_FIELDS),
            local(Ring3DressingFeatures.SULFUR_FIELDS_FUMAROLES, ModBiomes.SULFUR_FIELDS),
            local(Ring3DressingFeatures.SULFUR_FIELDS_MUD_POOLS, ModBiomes.SULFUR_FIELDS),
            local(Ring3DressingFeatures.BASALT_FLOWS_LAVA_TONGUES, ModBiomes.BASALT_FLOWS),
            local(Ring3DressingFeatures.BASALT_FLOWS_HORNITOS, ModBiomes.BASALT_FLOWS),
            local(Ring3DressingFeatures.BASALT_FLOWS_VENTS, ModBiomes.BASALT_FLOWS),
            local(Ring3DressingFeatures.BASALT_FLOWS_BOULDERS, ModBiomes.BASALT_FLOWS),
            local(Ring3DressingFeatures.DRIED_SEABED_CORAL_REEFS, ModBiomes.DRIED_SEABED),
            local(Ring3DressingFeatures.DRIED_SEABED_KELP_BALES, ModBiomes.DRIED_SEABED),
            local(Ring3DressingFeatures.DRIED_SEABED_FOSSILS, ModBiomes.DRIED_SEABED),
            local(Ring3DressingFeatures.ASHEN_WASTES_PUMICE_BOULDERS, ModBiomes.ASHEN_WASTES),
            local(Ring3DressingFeatures.ASHEN_WASTES_DEAD_TREES, ModBiomes.ASHEN_WASTES),
            local(Ring3DressingFeatures.VOLCANIC_PEAKS_IMPACT_CRATERS, ModBiomes.VOLCANIC_PEAKS),
            local(Ring3DressingFeatures.VOLCANIC_PEAKS_BLACKSTONE_ROCKS, ModBiomes.VOLCANIC_PEAKS),
            // Change 47b: the titan fields (the rocks before the drifts, which then lie around their feet).
            local(TitanFieldsFeatures.TITAN_FIELDS_SKULLS, ModBiomes.TITAN_FIELDS),
            local(TitanFieldsFeatures.TITAN_FIELDS_BONES, ModBiomes.TITAN_FIELDS),
            local(TitanFieldsFeatures.TITAN_FIELDS_SOUL_FIRE, ModBiomes.TITAN_FIELDS),
            local(TitanFieldsFeatures.TITAN_FIELDS_ROCKS, ModBiomes.TITAN_FIELDS),
            // Change 49: the casts stand before the drifts; the footprints dent them.
            local(Ring3HorrorFeatures.ASHEN_WASTES_ASH_CASTS, ModBiomes.ASHEN_WASTES),
            local(Ring3HorrorFeatures.RARE_ASH_CASTS, ModBiomes.EMBER_WOODS, ModBiomes.TITAN_FIELDS),
            local(Ring3DressingFeatures.ASH_DRIFTS, ModBiomes.ASHEN_WASTES, ModBiomes.TITAN_FIELDS, ModBiomes.VOLCANIC_PEAKS,
                    ModBiomes.DRIED_SEABED),
            local(Ring3HorrorFeatures.ASH_FOOTPRINT_TRAILS, ModBiomes.ASHEN_WASTES, ModBiomes.TITAN_FIELDS, ModBiomes.VOLCANIC_PEAKS),
            new Entry(Ring3OreFeatures.SULFUR_ORE_RICH, GenerationStep.Decoration.UNDERGROUND_ORES, Set.of(ModBiomes.SULFUR_FIELDS)),
            new Entry(Ring3DressingFeatures.DRIED_SEABED_DEAD_CORAL, GenerationStep.Decoration.VEGETAL_DECORATION, Set.of(ModBiomes.DRIED_SEABED)),
            new Entry(Ring3DressingFeatures.DRIED_SEABED_DEAD_BUSHES, GenerationStep.Decoration.VEGETAL_DECORATION, Set.of(ModBiomes.DRIED_SEABED)));

    private Ring3Features() {
    }

    @SafeVarargs
    private static Entry local(ResourceKey<PlacedFeature> feature, ResourceKey<Biome>... biomes) {
        return new Entry(feature, GenerationStep.Decoration.LOCAL_MODIFICATIONS, Set.of(biomes));
    }

    /** The biome-own features of {@code biome} in {@code step}, in the global order. */
    public static List<ResourceKey<PlacedFeature>> features(ResourceKey<Biome> biome, GenerationStep.Decoration step) {
        return ORDER.stream().filter(entry -> entry.step() == step && entry.biomes().contains(biome)).map(Entry::feature).toList();
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        Ring3OreFeatures.configured(context);
        Ring3DressingFeatures.configured(context);
        TitanFieldsFeatures.configured(context);
        Ring3HorrorFeatures.configured(context);
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        Ring3OreFeatures.placed(context);
        Ring3DressingFeatures.placed(context);
        TitanFieldsFeatures.placed(context);
        Ring3HorrorFeatures.placed(context);
    }
}
