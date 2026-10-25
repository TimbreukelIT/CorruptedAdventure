package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring3Biomes;

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
            // Change 47d: the coloured pools first (they shape the ground), the moss hummocks of the crater.
            local(SulfurFieldsFeatures.SULFUR_FIELDS_TERRACE_POOLS, ModBiomes.SULFUR_FIELDS),
            local(DormantCalderaFeatures.DORMANT_CALDERA_MOSS_HUMMOCKS, ModBiomes.DORMANT_CALDERA),
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
            // G2 (ground filling + subzones, Ring3GroundFeatures): per rugged biome debris fans, boulders and small
            // stones, then the biome's extras; the ash plain's trees last (they never root in a rock or a trunk).
            local(Ring3GroundFeatures.ASHEN_WASTES_DEBRIS_FANS, ModBiomes.ASHEN_WASTES),
            local(Ring3GroundFeatures.ASHEN_WASTES_GROUND_BOULDERS, ModBiomes.ASHEN_WASTES),
            local(Ring3GroundFeatures.ASHEN_WASTES_GROUND_STONES, ModBiomes.ASHEN_WASTES),
            local(Ring3GroundFeatures.VOLCANIC_PEAKS_DEBRIS_FANS, ModBiomes.VOLCANIC_PEAKS),
            local(Ring3GroundFeatures.VOLCANIC_PEAKS_GROUND_BOULDERS, ModBiomes.VOLCANIC_PEAKS),
            local(Ring3GroundFeatures.VOLCANIC_PEAKS_GROUND_STONES, ModBiomes.VOLCANIC_PEAKS),
            local(Ring3GroundFeatures.BASALT_FLOWS_DEBRIS_FANS, ModBiomes.BASALT_FLOWS),
            local(Ring3GroundFeatures.BASALT_FLOWS_GROUND_BOULDERS, ModBiomes.BASALT_FLOWS),
            local(Ring3GroundFeatures.BASALT_FLOWS_GROUND_STONES, ModBiomes.BASALT_FLOWS),
            local(Ring3GroundFeatures.DRIED_SEABED_DEBRIS_FANS, ModBiomes.DRIED_SEABED),
            local(Ring3GroundFeatures.DRIED_SEABED_GROUND_BOULDERS, ModBiomes.DRIED_SEABED),
            local(Ring3GroundFeatures.DRIED_SEABED_GROUND_STONES, ModBiomes.DRIED_SEABED),
            local(Ring3GroundFeatures.TITAN_FIELDS_DEBRIS_FANS, ModBiomes.TITAN_FIELDS),
            local(Ring3GroundFeatures.TITAN_FIELDS_GROUND_BOULDERS, ModBiomes.TITAN_FIELDS),
            local(Ring3GroundFeatures.TITAN_FIELDS_GROUND_STONES, ModBiomes.TITAN_FIELDS),
            local(Ring3GroundFeatures.SULFUR_FIELDS_DEBRIS_FANS, ModBiomes.SULFUR_FIELDS),
            local(Ring3GroundFeatures.SULFUR_FIELDS_GROUND_BOULDERS, ModBiomes.SULFUR_FIELDS),
            local(Ring3GroundFeatures.SULFUR_FIELDS_GROUND_STONES, ModBiomes.SULFUR_FIELDS),
            local(Ring3GroundFeatures.ASHEN_WASTES_FALLEN_TREES, ModBiomes.ASHEN_WASTES),
            local(Ring3GroundFeatures.ASHEN_WASTES_BURNT_FOREST_TRUNKS, ModBiomes.ASHEN_WASTES),
            local(Ring3GroundFeatures.ASHEN_WASTES_PUMICE_FIELD_BOULDERS, ModBiomes.ASHEN_WASTES),
            local(Ring3GroundFeatures.VOLCANIC_PEAKS_RIDGE_VENTS, ModBiomes.VOLCANIC_PEAKS),
            local(Ring3GroundFeatures.VOLCANIC_PEAKS_FRESH_LAVA_VENTS, ModBiomes.VOLCANIC_PEAKS),
            local(Ring3GroundFeatures.VOLCANIC_PEAKS_SCREE, ModBiomes.VOLCANIC_PEAKS),
            local(Ring3GroundFeatures.DRIED_SEABED_LOOSE_BONES, ModBiomes.DRIED_SEABED),
            local(Ring3GroundFeatures.DRIED_SEABED_SHELL_BANKS, ModBiomes.DRIED_SEABED),
            local(Ring3GroundFeatures.BASALT_FLOWS_ROPY_MOUNDS, ModBiomes.BASALT_FLOWS),
            local(Ring3GroundFeatures.BASALT_FLOWS_AA_RUBBLE, ModBiomes.BASALT_FLOWS),
            local(Ring3GroundFeatures.BASALT_FLOWS_COLUMNS, ModBiomes.BASALT_FLOWS),
            local(Ring3GroundFeatures.TITAN_FIELDS_BONE_FIELD_BONES, ModBiomes.TITAN_FIELDS),
            local(Ring3GroundFeatures.TITAN_FIELDS_LOOSE_BONES, ModBiomes.TITAN_FIELDS),
            local(Ring3GroundFeatures.TITAN_FIELDS_PLAIN_SOUL_FIRE, ModBiomes.TITAN_FIELDS),
            local(Ring3GroundFeatures.SULFUR_FIELDS_YELLOW_CRUST_FUMAROLES, ModBiomes.SULFUR_FIELDS),
            local(Ring3GroundFeatures.SULFUR_FIELDS_SINTER_MOUNDS, ModBiomes.SULFUR_FIELDS),
            local(Ring3GroundFeatures.ASHEN_WASTES_DEAD_TREE_GROUPS, ModBiomes.ASHEN_WASTES),
            local(Ring3GroundFeatures.ASHEN_WASTES_BURNT_FOREST, ModBiomes.ASHEN_WASTES),
            // Change 47b: the titan fields (the rocks before the drifts, which then lie around their feet).
            local(TitanFieldsFeatures.TITAN_FIELDS_SKULLS, ModBiomes.TITAN_FIELDS),
            local(TitanFieldsFeatures.TITAN_FIELDS_BONES, ModBiomes.TITAN_FIELDS),
            local(TitanFieldsFeatures.TITAN_FIELDS_SOUL_FIRE, ModBiomes.TITAN_FIELDS),
            local(TitanFieldsFeatures.TITAN_FIELDS_SOUL_FIRE_AROUND, ModBiomes.TITAN_FIELDS),
            local(TitanFieldsFeatures.TITAN_FIELDS_ROCKS, ModBiomes.TITAN_FIELDS),
            // Change 49: the casts stand before the drifts; the footprints dent them.
            local(Ring3HorrorFeatures.ASHEN_WASTES_ASH_CASTS, ModBiomes.ASHEN_WASTES),
            local(Ring3HorrorFeatures.RARE_ASH_CASTS, ModBiomes.EMBER_WOODS, ModBiomes.TITAN_FIELDS),
            // G2: the biome-own landmarks, before the drifts (which then lie around them) and the trees (which keep off).
            new Entry(VolcanicLandmarkFeatures.LANDMARKS, GenerationStep.Decoration.LOCAL_MODIFICATIONS, VolcanicLandmarkFeatures.BIOMES),
            local(Ring3DressingFeatures.ASH_DRIFTS, ModBiomes.ASHEN_WASTES, ModBiomes.TITAN_FIELDS, ModBiomes.VOLCANIC_PEAKS,
                    ModBiomes.DRIED_SEABED),
            new Entry(Ring3HorrorFeatures.ASH_FOOTPRINT_TRAILS, GenerationStep.Decoration.LOCAL_MODIFICATIONS, Set.copyOf(Ring3HorrorFeatures.TRAIL_BIOMES)),
            // Change 48: the steam fens, the pools first.
            local(SteamFensFeatures.STEAM_FENS_BOILING_POOLS, ModBiomes.STEAM_FENS),
            local(SteamFensFeatures.STEAM_FENS_MUD_POOLS, ModBiomes.STEAM_FENS),
            local(SteamFensFeatures.STEAM_FENS_GEYSERS, ModBiomes.STEAM_FENS),
            local(SteamFensFeatures.STEAM_FENS_STEAM_VENTS, ModBiomes.STEAM_FENS),
            // Change 47c: lava falls (the ring 3 core only) and the floating islands (over every ring 3 biome).
            new Entry(Ring3LavaFallFeatures.LAVA_FALLS, GenerationStep.Decoration.LOCAL_MODIFICATIONS, Set.copyOf(Ring3Biomes.ALL)),
            everywhere(Ring3IslandFeatures.ISLAND_EMBER_PATCHES, GenerationStep.Decoration.LOCAL_MODIFICATIONS),
            everywhere(Ring3IslandFeatures.ISLAND_HANGING_CRYSTALS, GenerationStep.Decoration.LOCAL_MODIFICATIONS),
            new Entry(Ring3OreFeatures.SULFUR_ORE_RICH, GenerationStep.Decoration.UNDERGROUND_ORES, Set.of(ModBiomes.SULFUR_FIELDS)),
            // Change 41b: the lining of the fire geodes (after the ores, which never touch the lining).
            new Entry(Ring3GeodeFeatures.FIRE_GEODES, GenerationStep.Decoration.UNDERGROUND_DECORATION, Set.copyOf(Ring3Biomes.ALL)),
            new Entry(Ring3DressingFeatures.DRIED_SEABED_DEAD_CORAL, GenerationStep.Decoration.VEGETAL_DECORATION, Set.of(ModBiomes.DRIED_SEABED)),
            new Entry(Ring3DressingFeatures.DRIED_SEABED_DEAD_BUSHES, GenerationStep.Decoration.VEGETAL_DECORATION, Set.of(ModBiomes.DRIED_SEABED)),
            // G2 (Ring3GroundFeatures): loose coral and the clay flats' bushes; the charred shrub of the bare plains.
            vegetal(Ring3GroundFeatures.DRIED_SEABED_LOOSE_CORAL, ModBiomes.DRIED_SEABED),
            vegetal(Ring3GroundFeatures.DRIED_SEABED_CLAY_FLAT_BUSHES, ModBiomes.DRIED_SEABED),
            vegetal(Ring3GroundFeatures.CHARRED_SHRUBS, ModBiomes.ASHEN_WASTES, ModBiomes.VOLCANIC_PEAKS, ModBiomes.BASALT_FLOWS,
                    ModBiomes.TITAN_FIELDS),
            // Change 47d: sulfur crystals on the crusts.
            vegetal(SulfurFieldsFeatures.SULFUR_FIELDS_CRUST_CRYSTALS, ModBiomes.SULFUR_FIELDS),
            // Change 47c: island trees, the ember woods (the ash last, around what stands), the dormant caldera.
            everywhere(Ring3IslandFeatures.ISLAND_EMBER_TREES, GenerationStep.Decoration.VEGETAL_DECORATION),
            vegetal(EmberWoodsFeatures.EMBER_WOODS_TREES, ModBiomes.EMBER_WOODS),
            vegetal(EmberWoodsFeatures.EMBER_WOODS_FALLEN_LOGS, ModBiomes.EMBER_WOODS),
            vegetal(EmberWoodsFeatures.EMBER_WOODS_FIRE_FLORA, ModBiomes.EMBER_WOODS),
            vegetal(EmberWoodsFeatures.EMBER_WOODS_BERRY_BUSHES, ModBiomes.EMBER_WOODS),
            // G2 ring 3: the burn patches of the ember woods (after the trees and the clearings' logs).
            vegetal(EmberWoodsFeatures.EMBER_WOODS_BURN_STUMPS, ModBiomes.EMBER_WOODS),
            vegetal(EmberWoodsFeatures.EMBER_WOODS_BURN_FLORA, ModBiomes.EMBER_WOODS),
            vegetal(EmberWoodsFeatures.EMBER_WOODS_ASH, ModBiomes.EMBER_WOODS),
            vegetal(DormantCalderaFeatures.DORMANT_CALDERA_AZALEA_TREES, ModBiomes.DORMANT_CALDERA),
            vegetal(DormantCalderaFeatures.DORMANT_CALDERA_AZALEA_SHRUBS, ModBiomes.DORMANT_CALDERA),
            vegetal(DormantCalderaFeatures.DORMANT_CALDERA_FALLEN_LOGS, ModBiomes.DORMANT_CALDERA),
            vegetal(DormantCalderaFeatures.DORMANT_CALDERA_AZALEA_BUSHES, ModBiomes.DORMANT_CALDERA),
            vegetal(DormantCalderaFeatures.DORMANT_CALDERA_DRIPLEAF, ModBiomes.DORMANT_CALDERA),
            vegetal(DormantCalderaFeatures.DORMANT_CALDERA_FERNS, ModBiomes.DORMANT_CALDERA),
            vegetal(DormantCalderaFeatures.DORMANT_CALDERA_BERRY_BUSHES, ModBiomes.DORMANT_CALDERA),
            vegetal(DormantCalderaFeatures.DORMANT_CALDERA_MOSS_CARPETS, ModBiomes.DORMANT_CALDERA),
            vegetal(DormantCalderaFeatures.DORMANT_CALDERA_WALL_DRAPES, ModBiomes.DORMANT_CALDERA),
            vegetal(DormantCalderaFeatures.DORMANT_CALDERA_GLOW_LICHEN, ModBiomes.DORMANT_CALDERA));

    private Ring3Features() {
    }

    @SafeVarargs
    private static Entry vegetal(ResourceKey<PlacedFeature> feature, ResourceKey<Biome>... biomes) {
        return new Entry(feature, GenerationStep.Decoration.VEGETAL_DECORATION, Set.of(biomes));
    }

    /** In every ring 3 biome and the steam fens: it finds its places through the terrain, whatever the biome below. */
    private static Entry everywhere(ResourceKey<PlacedFeature> feature, GenerationStep.Decoration step) {
        return new Entry(feature, step, Set.copyOf(Ring3Biomes.WITH_TRANSITION));
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
        EmberWoodsFeatures.configured(context);
        DormantCalderaFeatures.configured(context);
        Ring3IslandFeatures.configured(context);
        Ring3LavaFallFeatures.configured(context);
        SteamFensFeatures.configured(context);
        Ring3GeodeFeatures.configured(context);
        SulfurFieldsFeatures.configured(context);
        Ring3GroundFeatures.configured(context);
        VolcanicLandmarkFeatures.configured(context);
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        Ring3OreFeatures.placed(context);
        Ring3DressingFeatures.placed(context);
        TitanFieldsFeatures.placed(context);
        Ring3HorrorFeatures.placed(context);
        EmberWoodsFeatures.placed(context);
        DormantCalderaFeatures.placed(context);
        Ring3IslandFeatures.placed(context);
        Ring3LavaFallFeatures.placed(context);
        SteamFensFeatures.placed(context);
        Ring3GeodeFeatures.placed(context);
        SulfurFieldsFeatures.placed(context);
        Ring3GroundFeatures.placed(context);
        VolcanicLandmarkFeatures.placed(context);
    }
}
