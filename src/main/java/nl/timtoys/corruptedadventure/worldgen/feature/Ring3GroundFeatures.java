package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicOutput;

import java.util.List;
import java.util.Optional;

/**
 * The ground filling and subzones of ring 3 (G2, sparring 2026-10-25: decisions 1 = A, 2 = A, 4 = A), after the ring 4
 * decor proef: no plain stays empty, and every rugged biome has two kinds of patch with their own ground and features.
 * <ul>
 *   <li><b>Ground filling</b> in the rugged biomes (ash, peaks, basalt, seabed, titans, sulfur; not the ember woods, the crater or the steam fens): small stones of the biome's rock (1–2 blocks, grid 8),
 *   boulders (≤ 1 per chunk, also on slopes up to ~45°) and debris fans of gravel, tuff and blackstone at the foot of
 *   cliffs and cone flanks; per biome more (dead trees in groups with fallen trunks, ridge vents, basalt columns, loose
 *   bones and coral, soul fire on the plain).</li>
 *   <li><b>Subzones</b> from one ring noise {@link #PATCHES} (waves of ~150–300 blocks): below −{@link #PATCH} the A patch,
 *   above +{@link #PATCH} the B patch, about 20 % each (the surface script {@code ring3_surface.py} paints their ground
 *   from the same noise and threshold): ash "burnt forest" ↔ "pumice field", peaks "fresh lava" ↔ "scree", seabed
 *   "clay flats" ↔ "shell bank", basalt "ropy lava" ↔ "aa lava", sulfur "yellow crust" ↔ "sinter", titans "bone field"
 *   ↔ "soul soil plain".</li>
 *   <li><b>Fire flora</b>: the charred shrub in small clumps on the bare plains of the ash, peaks, basalt and titans.</li>
 * </ul>
 * Determinism as every ring 3 feature: {@link JitteredGridPlacement} cells with a margin of at least the feature's reach
 * (the rubble, patches and fallen trunks also move or clip themselves into their own chunk), the frozen
 * {@code WORLD_SURFACE_WG} heightmap, the pure terrain ({@link VolcanicFilter}) and the seeded noise
 * ({@link ClusterDensity}, read at the cell's centre). Rocks and trunks come before the trees in the order, so a tree never
 * roots in a rock. Amounts are starting values for the tuning loop.
 */
public final class Ring3GroundFeatures {
    /** The subzone noise of ring 3: {@code ring_3_patches.json} (firstOctave −8), written by {@code ring3_surface.py}. */
    static final ResourceKey<NormalNoise.NoiseParameters> PATCHES = ResourceKey.create(Registries.NOISE, CorruptedAdventure.id("ring_3_patches"));
    /**
     * Beyond ±this the noise is a subzone; with its spread (σ ≈ 0,30) about 20 % of a biome on each side, 40 % "other".
     * The same value as {@code PATCH} in {@code ring3_surface.py}.
     */
    static final double PATCH = 0.26;
    /** Half the width of a subzone's ramp in the features' density (the surface switches at {@link #PATCH} exactly). */
    private static final double EDGE = 0.04;

    // Configured.
    private static final ResourceKey<ConfiguredFeature<?, ?>> CHARRED_FALLEN_TREE = configured("charred_fallen_tree");
    private static final ResourceKey<ConfiguredFeature<?, ?>> CHARRED_STAKES = configured("charred_stakes");
    private static final ResourceKey<ConfiguredFeature<?, ?>> PEAK_RIDGE_VENT = configured("peak_ridge_vent");
    private static final ResourceKey<ConfiguredFeature<?, ?>> PEAK_SCREE = configured("peak_scree");
    private static final ResourceKey<ConfiguredFeature<?, ?>> BONE = configured("loose_bone");
    private static final ResourceKey<ConfiguredFeature<?, ?>> SHELLS = configured("shell_bank_shells");
    private static final ResourceKey<ConfiguredFeature<?, ?>> BASALT_COLUMN = configured("basalt_column");
    private static final ResourceKey<ConfiguredFeature<?, ?>> ROPY_MOUND = configured("ropy_lava_mound");
    private static final ResourceKey<ConfiguredFeature<?, ?>> AA_RUBBLE = configured("aa_lava_rubble");
    private static final ResourceKey<ConfiguredFeature<?, ?>> SINTER_MOUND = configured("sinter_mound");
    private static final ResourceKey<ConfiguredFeature<?, ?>> CHARRED_SHRUB = configured("charred_shrub");

    // The ground filling per rugged biome: its debris fans (they change the ground), boulders and small stones, in this
    // order in Ring3Features.ORDER.
    private static final Filling ASH = new Filling(ModBiomes.ASHEN_WASTES, "ashen_wastes", 0.7F);
    private static final Filling PEAKS = new Filling(ModBiomes.VOLCANIC_PEAKS, "volcanic_peaks", 0.9F);
    private static final Filling FLOWS = new Filling(ModBiomes.BASALT_FLOWS, "basalt_flows", 0.7F);
    private static final Filling SEABED = new Filling(ModBiomes.DRIED_SEABED, "dried_seabed", 0.6F);
    private static final Filling TITANS = new Filling(ModBiomes.TITAN_FIELDS, "titan_fields", 0.6F);
    private static final Filling SULFUR = new Filling(ModBiomes.SULFUR_FIELDS, "sulfur_fields", 0.5F);
    private static final List<Filling> FILLING = List.of(ASH, PEAKS, FLOWS, SEABED, TITANS, SULFUR);
    public static final ResourceKey<PlacedFeature> ASHEN_WASTES_DEBRIS_FANS = ASH.fans();
    public static final ResourceKey<PlacedFeature> ASHEN_WASTES_GROUND_BOULDERS = ASH.boulders();
    public static final ResourceKey<PlacedFeature> ASHEN_WASTES_GROUND_STONES = ASH.stones();
    public static final ResourceKey<PlacedFeature> VOLCANIC_PEAKS_DEBRIS_FANS = PEAKS.fans();
    public static final ResourceKey<PlacedFeature> VOLCANIC_PEAKS_GROUND_BOULDERS = PEAKS.boulders();
    public static final ResourceKey<PlacedFeature> VOLCANIC_PEAKS_GROUND_STONES = PEAKS.stones();
    public static final ResourceKey<PlacedFeature> BASALT_FLOWS_DEBRIS_FANS = FLOWS.fans();
    public static final ResourceKey<PlacedFeature> BASALT_FLOWS_GROUND_BOULDERS = FLOWS.boulders();
    public static final ResourceKey<PlacedFeature> BASALT_FLOWS_GROUND_STONES = FLOWS.stones();
    public static final ResourceKey<PlacedFeature> DRIED_SEABED_DEBRIS_FANS = SEABED.fans();
    public static final ResourceKey<PlacedFeature> DRIED_SEABED_GROUND_BOULDERS = SEABED.boulders();
    public static final ResourceKey<PlacedFeature> DRIED_SEABED_GROUND_STONES = SEABED.stones();
    public static final ResourceKey<PlacedFeature> TITAN_FIELDS_DEBRIS_FANS = TITANS.fans();
    public static final ResourceKey<PlacedFeature> TITAN_FIELDS_GROUND_BOULDERS = TITANS.boulders();
    public static final ResourceKey<PlacedFeature> TITAN_FIELDS_GROUND_STONES = TITANS.stones();
    public static final ResourceKey<PlacedFeature> SULFUR_FIELDS_DEBRIS_FANS = SULFUR.fans();
    public static final ResourceKey<PlacedFeature> SULFUR_FIELDS_GROUND_BOULDERS = SULFUR.boulders();
    public static final ResourceKey<PlacedFeature> SULFUR_FIELDS_GROUND_STONES = SULFUR.stones();
    // Placed: the extras per biome, LOCAL_MODIFICATIONS (before the ash drifts).
    public static final ResourceKey<PlacedFeature> ASHEN_WASTES_FALLEN_TREES = placed("ashen_wastes_fallen_trees");
    public static final ResourceKey<PlacedFeature> ASHEN_WASTES_BURNT_FOREST_TRUNKS = placed("ashen_wastes_burnt_forest_trunks");
    public static final ResourceKey<PlacedFeature> ASHEN_WASTES_PUMICE_FIELD_BOULDERS = placed("ashen_wastes_pumice_field_boulders");
    public static final ResourceKey<PlacedFeature> VOLCANIC_PEAKS_RIDGE_VENTS = placed("volcanic_peaks_ridge_vents");
    public static final ResourceKey<PlacedFeature> VOLCANIC_PEAKS_FRESH_LAVA_VENTS = placed("volcanic_peaks_fresh_lava_vents");
    public static final ResourceKey<PlacedFeature> VOLCANIC_PEAKS_SCREE = placed("volcanic_peaks_scree");
    public static final ResourceKey<PlacedFeature> DRIED_SEABED_LOOSE_BONES = placed("dried_seabed_loose_bones");
    public static final ResourceKey<PlacedFeature> DRIED_SEABED_SHELL_BANKS = placed("dried_seabed_shell_banks");
    public static final ResourceKey<PlacedFeature> BASALT_FLOWS_ROPY_MOUNDS = placed("basalt_flows_ropy_mounds");
    public static final ResourceKey<PlacedFeature> BASALT_FLOWS_AA_RUBBLE = placed("basalt_flows_aa_rubble");
    public static final ResourceKey<PlacedFeature> BASALT_FLOWS_COLUMNS = placed("basalt_flows_columns");
    public static final ResourceKey<PlacedFeature> TITAN_FIELDS_BONE_FIELD_BONES = placed("titan_fields_bone_field_bones");
    public static final ResourceKey<PlacedFeature> TITAN_FIELDS_LOOSE_BONES = placed("titan_fields_loose_bones");
    public static final ResourceKey<PlacedFeature> TITAN_FIELDS_PLAIN_SOUL_FIRE = placed("titan_fields_plain_soul_fire");
    public static final ResourceKey<PlacedFeature> SULFUR_FIELDS_YELLOW_CRUST_FUMAROLES = placed("sulfur_fields_yellow_crust_fumaroles");
    public static final ResourceKey<PlacedFeature> SULFUR_FIELDS_SINTER_MOUNDS = placed("sulfur_fields_sinter_mounds");
    /** The trees last of the ash extras: they never root in a rock or a trunk placed before them. */
    public static final ResourceKey<PlacedFeature> ASHEN_WASTES_DEAD_TREE_GROUPS = placed("ashen_wastes_dead_tree_groups");
    public static final ResourceKey<PlacedFeature> ASHEN_WASTES_BURNT_FOREST = placed("ashen_wastes_burnt_forest");
    // Placed: VEGETAL_DECORATION.
    public static final ResourceKey<PlacedFeature> DRIED_SEABED_LOOSE_CORAL = placed("dried_seabed_loose_coral");
    public static final ResourceKey<PlacedFeature> DRIED_SEABED_CLAY_FLAT_BUSHES = placed("dried_seabed_clay_flat_bushes");
    public static final ResourceKey<PlacedFeature> CHARRED_SHRUBS = placed("charred_shrubs");
    /** The biomes of the charred shrub (decision 4 = A). */
    static final List<ResourceKey<Biome>> SHRUB_BIOMES = List.of(ModBiomes.ASHEN_WASTES, ModBiomes.VOLCANIC_PEAKS, ModBiomes.BASALT_FLOWS,
            ModBiomes.TITAN_FIELDS);

    /** Groups of dead trees (and their fallen trunks): small blobs of the patch noise, 3–8 trees on the grid of 4. */
    private static final ClusterDensity TREE_GROUPS = clumps(1, 12.0, 0.42, 0.52);
    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);
    private static final Vec3i BELOW = Direction.DOWN.getNormal();

    /** The ground filling of one rugged biome: its keys and how often a debris fan is tried per chunk. */
    private record Filling(ResourceKey<Biome> biome, String name, float fanChance) {
        ResourceKey<ConfiguredFeature<?, ?>> stone() {
            return configured(name + "_stone");
        }

        ResourceKey<ConfiguredFeature<?, ?>> boulder() {
            return configured(name + "_boulder");
        }

        ResourceKey<ConfiguredFeature<?, ?>> fan() {
            return configured(name + "_debris_fan");
        }

        ResourceKey<PlacedFeature> fans() {
            return placed(name + "_debris_fans");
        }

        ResourceKey<PlacedFeature> boulders() {
            return placed(name + "_ground_boulders");
        }

        ResourceKey<PlacedFeature> stones() {
            return placed(name + "_ground_stones");
        }
    }

    private Ring3GroundFeatures() {
    }

    // ------------------------------------------------------------------------------------------------ densities
    /** The A patch of every biome (the noise below −{@link #PATCH}). */
    static ClusterDensity zoneA() {
        return new ClusterDensity(PATCHES, 1.0, 0, -PATCH + EDGE, -PATCH - EDGE, 0.0);
    }

    /** The B patch of every biome (the noise above +{@link #PATCH}). */
    static ClusterDensity zoneB() {
        return ClusterDensity.of(PATCHES, PATCH - EDGE, PATCH + EDGE);
    }

    /** Clumps: the same noise {@code scale} times finer, a field of its own per {@code salt}, kept from {@code low} up. */
    static ClusterDensity clumps(int salt, double scale, double low, double high) {
        return new ClusterDensity(PATCHES, scale, salt, low, high, 0.0);
    }

    // ------------------------------------------------------------------------------------------------ ground per biome
    /** The open ground of a rugged biome, also the tops of its subzones (never lava, magma or the travertine of a pool). */
    private static Block[] ground(ResourceKey<Biome> biome) {
        if (biome == ModBiomes.ASHEN_WASTES) {
            return new Block[] {ModBlocks.ASH_BLOCK.get(), ModBlocks.PUMICE.get(), Blocks.GRAVEL, Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.TUFF,
                    ModBlocks.SMOLDERING_ASH.get(), Blocks.COARSE_DIRT};
        } else if (biome == ModBiomes.VOLCANIC_PEAKS) {
            return new Block[] {ModBlocks.ASH_BLOCK.get(), ModBlocks.PUMICE.get(), Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.BLACKSTONE,
                    Blocks.TUFF, Blocks.GRAVEL};
        } else if (biome == ModBiomes.BASALT_FLOWS) {
            return new Block[] {Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.POLISHED_BASALT, Blocks.BLACKSTONE, ModBlocks.ASH_BLOCK.get()};
        } else if (biome == ModBiomes.DRIED_SEABED) {
            return new Block[] {Blocks.SAND, Blocks.GRAVEL, Blocks.CLAY, ModBlocks.CRACKED_CLAY.get(), Blocks.TERRACOTTA, Blocks.CALCITE,
                    ModBlocks.ASH_BLOCK.get(), ModBlocks.PUMICE.get()};
        } else if (biome == ModBiomes.TITAN_FIELDS) {
            return new Block[] {ModBlocks.ASH_BLOCK.get(), Blocks.SOUL_SOIL, Blocks.SOUL_SAND, Blocks.BASALT, Blocks.SMOOTH_BASALT,
                    ModBlocks.PUMICE.get()};
        }
        // Sulfur fields: not calcite or terracotta, the travertine of the coloured pools and the sinter.
        return new Block[] {ModBlocks.SULFUR_CRUST.get(), Blocks.SMOOTH_BASALT, ModBlocks.ASH_BLOCK.get()};
    }

    /** The rock of the small stones and boulders (one block kind per rock). */
    private static BlockStateProvider rock(ResourceKey<Biome> biome) {
        if (biome == ModBiomes.ASHEN_WASTES) {
            return weighted(ModBlocks.PUMICE.get(), 3, Blocks.SMOOTH_BASALT, 1, Blocks.BASALT, 1, Blocks.TUFF, 1);
        } else if (biome == ModBiomes.VOLCANIC_PEAKS) {
            return weighted(Blocks.BLACKSTONE, 2, Blocks.BASALT, 2, Blocks.TUFF, 1, ModBlocks.PUMICE.get(), 1);
        } else if (biome == ModBiomes.BASALT_FLOWS) {
            return weighted(Blocks.BASALT, 3, Blocks.BLACKSTONE, 2, Blocks.SMOOTH_BASALT, 1);
        } else if (biome == ModBiomes.DRIED_SEABED) {
            return weighted(ModBlocks.PUMICE.get(), 2, Blocks.TUFF, 2, Blocks.SANDSTONE, 1, Blocks.CALCITE, 1);
        } else if (biome == ModBiomes.TITAN_FIELDS) {
            return weighted(Blocks.BASALT, 2, Blocks.SMOOTH_BASALT, 1, Blocks.BLACKSTONE, 1, Blocks.TUFF, 1);
        }
        // Sulfur fields: light rock, the dark basalt read as litter on the yellow crust (G2 after-walk).
        return weighted(Blocks.TUFF, 3, ModBlocks.PUMICE.get(), 2, Blocks.CALCITE, 1);
    }

    /** The scree of a debris fan. */
    private static BlockStateProvider scree(ResourceKey<Biome> biome) {
        if (biome == ModBiomes.VOLCANIC_PEAKS) {
            return weighted(Blocks.GRAVEL, 2, Blocks.TUFF, 2, Blocks.BLACKSTONE, 2);
        } else if (biome == ModBiomes.BASALT_FLOWS) {
            return weighted(Blocks.BLACKSTONE, 2, Blocks.BASALT, 2, Blocks.GRAVEL, 1);
        } else if (biome == ModBiomes.DRIED_SEABED) {
            return weighted(Blocks.GRAVEL, 2, Blocks.TUFF, 1, Blocks.SAND, 1);
        } else if (biome == ModBiomes.SULFUR_FIELDS) {
            return weighted(Blocks.TUFF, 2, Blocks.GRAVEL, 1, Blocks.SMOOTH_BASALT, 1);
        }
        return weighted(Blocks.GRAVEL, 3, Blocks.TUFF, 2, Blocks.BLACKSTONE, 1);
    }

    // ------------------------------------------------------------------------------------------------ configured
    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        for (Filling filling : FILLING) {
            BlockStateProvider rock = rock(filling.biome());
            FeatureUtils.register(context, filling.stone(), ModFeatures.RUBBLE.get(), new RubbleConfiguration(rock, UniformInt.of(1, 2)));
            FeatureUtils.register(context, filling.boulder(), ModFeatures.RUBBLE.get(), new RubbleConfiguration(rock, UniformInt.of(2, 3)));
            // A ragged tongue of scree down the slope's foot, a block of rubble on its core here and there.
            FeatureUtils.register(context, filling.fan(), ModFeatures.GROUND_PATCH.get(), new GroundPatchConfiguration(
                    scree(filling.biome()), blocks(ground(filling.biome())), UniformInt.of(2, 4), 1.6F, ConstantInt.of(1),
                    UniformInt.of(0, 1), scree(filling.biome())));
        }

        // Ash plain: a charred stump with its fallen trunk (the ring 4 ground object), ash on the wood; stakes of the burnt
        // forest (a bare trunk or a low stump).
        FeatureUtils.register(context, CHARRED_FALLEN_TREE, ModFeatures.FALLEN_TREE.get(), new FallenTreeConfiguration(
                ModBlocks.CHARRED_LOG.get().defaultBlockState(), UniformInt.of(1, 2), UniformInt.of(3, 5), 0.25F, 0.35F,
                BlockStateProvider.simple(ModBlocks.ASH_LAYER.get()), 0.3F));
        FeatureUtils.register(context, CHARRED_STAKES, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(configured.getOrThrow(Ring3DressingFeatures.CHARRED_STUMP)), 0.4F)),
                PlacementUtils.inlinePlaced(configured.getOrThrow(Ring3DressingFeatures.CHARRED_DEAD_TREE))));

        // Peaks: a magma vent in a blackstone collar on the ridges; loose scree on the scree slopes.
        HolderSet<Block> peaks = blocks(ground(ModBiomes.VOLCANIC_PEAKS));
        FeatureUtils.register(context, PEAK_RIDGE_VENT, ModFeatures.GROUND_VENT.get(), new GroundVentConfiguration(
                Blocks.MAGMA_BLOCK.defaultBlockState(), weighted(Blocks.BLACKSTONE, 2, ModBlocks.SMOLDERING_STONE.get(), 1), UniformInt.of(1, 2),
                0.6F, peaks, List.of(
                        OreConfiguration.target(new BlockMatchTest(Blocks.BASALT), Blocks.MAGMA_BLOCK.defaultBlockState()),
                        OreConfiguration.target(new BlockMatchTest(Blocks.BLACKSTONE), Blocks.MAGMA_BLOCK.defaultBlockState())),
                UniformInt.of(1, 2), Optional.empty()));
        FeatureUtils.register(context, PEAK_SCREE, ModFeatures.RUBBLE.get(), new RubbleConfiguration(
                weighted(Blocks.TUFF, 3, Blocks.GRAVEL, 1, ModBlocks.PUMICE.get(), 1), UniformInt.of(1, 2)));

        // Seabed and titans: a loose bone lying in the sand or ash; the shells of the shell banks.
        FeatureUtils.register(context, BONE, ModFeatures.RUBBLE.get(), new RubbleConfiguration(bones(), ConstantInt.of(1)));
        FeatureUtils.register(context, SHELLS, ModFeatures.RUBBLE.get(), new RubbleConfiguration(
                new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.CALCITE.defaultBlockState(), 3)
                        .add(Blocks.BONE_BLOCK.defaultBlockState(), 1)
                        .add(Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), 1)),
                UniformInt.of(1, 2)));

        // Basalt flows: small columns (2–4 basalt, now and then a polished cap), the ropy lava's low mounds, aa rubble.
        FeatureUtils.register(context, BASALT_COLUMN, Feature.BLOCK_COLUMN, new BlockColumnConfiguration(List.of(
                BlockColumnConfiguration.layer(UniformInt.of(2, 4), BlockStateProvider.simple(Blocks.BASALT)),
                BlockColumnConfiguration.layer(UniformInt.of(0, 1), BlockStateProvider.simple(Blocks.POLISHED_BASALT))),
                Direction.UP, BlockPredicate.ONLY_IN_AIR_PREDICATE, false));
        HolderSet<Block> basalt = blocks(ground(ModBiomes.BASALT_FLOWS));
        FeatureUtils.register(context, ROPY_MOUND, ModFeatures.GROUND_PATCH.get(), new GroundPatchConfiguration(
                BlockStateProvider.simple(Blocks.SMOOTH_BASALT), basalt, UniformInt.of(2, 3), 2.0F, ConstantInt.of(1), ConstantInt.of(1),
                weighted(Blocks.SMOOTH_BASALT, 2, Blocks.POLISHED_BASALT, 1)));
        FeatureUtils.register(context, AA_RUBBLE, ModFeatures.RUBBLE.get(), new RubbleConfiguration(
                weighted(Blocks.BLACKSTONE, 3, Blocks.BASALT, 1), UniformInt.of(1, 3)));

        // Sulfur fields: a low sinter mound of calcite.
        FeatureUtils.register(context, SINTER_MOUND, ModFeatures.GROUND_PATCH.get(), new GroundPatchConfiguration(
                BlockStateProvider.simple(Blocks.CALCITE), blocks(ModBlocks.SULFUR_CRUST.get(), Blocks.SMOOTH_BASALT, ModBlocks.ASH_BLOCK.get(),
                Blocks.CALCITE), UniformInt.of(1, 3), 1.3F, ConstantInt.of(1), UniformInt.of(0, 1), BlockStateProvider.simple(Blocks.CALCITE)));

        FeatureUtils.register(context, CHARRED_SHRUB, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(
                BlockStateProvider.simple(ModBlocks.CHARRED_SHRUB.get())));
    }

    // ------------------------------------------------------------------------------------------------ placed
    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementModifier ground = VolcanicFilter.builder().ground().build();
        PlacementModifier slopes = slopes();
        PlacementModifier upTo45 = SlopeFilter.flat(2, 2);

        for (Filling filling : FILLING) {
            PlacementModifier onGround = onBlocks(ground(filling.biome()));
            // The foot of a cliff or a cone's flank: a wall at least 5 high within 6 (the patch clips to its chunk).
            PlacementUtils.register(context, filling.fans(), features.getOrThrow(filling.fan()),
                    JitteredGridPlacement.of(16, 5, filling.fanChance()), SURFACE, SlopeFilter.wallFoot(6, 5), slopes, onGround,
                    BiomeFilter.biome());
            // Sparser on the sulfur crust, where every stone stands out against the yellow (G2 after-walk).
            boolean sulfur = filling.biome() == ModBiomes.SULFUR_FIELDS;
            PlacementUtils.register(context, filling.boulders(), features.getOrThrow(filling.boulder()),
                    JitteredGridPlacement.of(16, 2, sulfur ? 0.2F : 0.45F), SURFACE, upTo45, slopes, onGround, BiomeFilter.biome());
            PlacementUtils.register(context, filling.stones(), features.getOrThrow(filling.stone()),
                    JitteredGridPlacement.of(8, 1, sulfur ? 0.15F : 0.4F), SURFACE, upTo45, slopes, onGround, BiomeFilter.biome());
        }

        // Ash plain. Fallen trunks in the tree groups and the burnt forest (FallenTreeFeature keeps them in the chunk).
        PlacementModifier ashGround = onBlocks(ground(ModBiomes.ASHEN_WASTES));
        PlacementUtils.register(context, ASHEN_WASTES_FALLEN_TREES, features.getOrThrow(CHARRED_FALLEN_TREE),
                JitteredGridPlacement.of(8, 1, 0.5F, TREE_GROUPS.withFloor(0.05)), SURFACE, ground, ashGround, BiomeFilter.biome());
        PlacementUtils.register(context, ASHEN_WASTES_BURNT_FOREST_TRUNKS, features.getOrThrow(CHARRED_FALLEN_TREE),
                JitteredGridPlacement.of(8, 1, 0.45F, zoneA()), SURFACE, ground, ashGround, BiomeFilter.biome());
        // Pumice field: many more pumice boulders.
        PlacementUtils.register(context, ASHEN_WASTES_PUMICE_FIELD_BOULDERS, features.getOrThrow(Ring3DressingFeatures.PUMICE_BOULDER),
                JitteredGridPlacement.of(8, 1, 0.4F, zoneB()), SURFACE, upTo45, slopes, ashGround, BiomeFilter.biome());

        // Peaks: magma vents on the ridges (nothing more than 1 higher within 4, a drop of 3 beside), smouldering vents in
        // the fresh lava, loose scree on the scree slopes.
        PlacementModifier peakGround = onBlocks(ground(ModBiomes.VOLCANIC_PEAKS));
        PlacementUtils.register(context, VOLCANIC_PEAKS_RIDGE_VENTS, features.getOrThrow(PEAK_RIDGE_VENT),
                JitteredGridPlacement.of(16, GroundVentFeature.MAX_COLLAR, 0.6F), SURFACE, new SlopeFilter(4, SlopeFilter.Measure.RISE,
                        Integer.MIN_VALUE, 1), SlopeFilter.rimTop(4, 3), slopes, peakGround, BiomeFilter.biome());
        PlacementUtils.register(context, VOLCANIC_PEAKS_FRESH_LAVA_VENTS, features.getOrThrow(Ring3DressingFeatures.SMOLDERING_VENT),
                JitteredGridPlacement.of(8, 2, 0.35F, zoneA()), SURFACE, SlopeFilter.flat(2, 1), slopes, BiomeFilter.biome());
        PlacementUtils.register(context, VOLCANIC_PEAKS_SCREE, features.getOrThrow(PEAK_SCREE),
                JitteredGridPlacement.of(4, 1, 0.25F, zoneB()), SURFACE, upTo45, slopes, peakGround, BiomeFilter.biome());

        // Dried seabed: loose bones; shells and bones in the shell banks.
        PlacementModifier seaGround = onBlocks(ground(ModBiomes.DRIED_SEABED));
        PlacementUtils.register(context, DRIED_SEABED_LOOSE_BONES, features.getOrThrow(BONE),
                JitteredGridPlacement.of(8, 1, 0.15F), SURFACE, ground, seaGround, BiomeFilter.biome());
        PlacementUtils.register(context, DRIED_SEABED_SHELL_BANKS, features.getOrThrow(SHELLS),
                JitteredGridPlacement.of(4, 1, 0.22F, zoneB()), SURFACE, ground, seaGround, BiomeFilter.biome());

        // Basalt flows: the ropy lava's mounds, the aa lava's rubble; small basalt columns in groups.
        PlacementModifier basaltGround = onBlocks(ground(ModBiomes.BASALT_FLOWS));
        PlacementUtils.register(context, BASALT_FLOWS_ROPY_MOUNDS, features.getOrThrow(ROPY_MOUND),
                JitteredGridPlacement.of(16, 5, 0.7F, zoneA()), SURFACE, ground, basaltGround, BiomeFilter.biome());
        PlacementUtils.register(context, BASALT_FLOWS_AA_RUBBLE, features.getOrThrow(AA_RUBBLE),
                JitteredGridPlacement.of(8, 1, 0.5F, zoneB()), SURFACE, upTo45, slopes, basaltGround, BiomeFilter.biome());
        PlacementUtils.register(context, BASALT_FLOWS_COLUMNS, features.getOrThrow(BASALT_COLUMN),
                JitteredGridPlacement.of(4, 1, 0.55F, clumps(4, 12.0, 0.4, 0.5)), SURFACE, SlopeFilter.flat(1, 1), ground,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(BELOW, ground(ModBiomes.BASALT_FLOWS)))),
                BiomeFilter.biome());

        // Titan fields: bone fields (the titan bones and loose bones), loose bones in clumps elsewhere, soul fire on the
        // soul soil plain.
        PlacementModifier titanGround = onBlocks(ground(ModBiomes.TITAN_FIELDS));
        PlacementUtils.register(context, TITAN_FIELDS_BONE_FIELD_BONES, features.getOrThrow(TitanFieldsFeatures.TITAN_BONES),
                JitteredGridPlacement.of(16, 1, 0.55F, zoneA()), SURFACE, SlopeFilter.flat(3, 2), ground, BiomeFilter.biome());
        PlacementUtils.register(context, TITAN_FIELDS_LOOSE_BONES, features.getOrThrow(BONE),
                JitteredGridPlacement.of(8, 1, 0.4F, clumps(5, 4.0, 0.15, 0.35).withFloor(0.2)), SURFACE, ground, titanGround,
                BiomeFilter.biome());
        PlacementUtils.register(context, TITAN_FIELDS_PLAIN_SOUL_FIRE, features.getOrThrow(TitanFieldsFeatures.SOUL_FIRE_PATCH),
                JitteredGridPlacement.of(16, 3, 0.6F, zoneB().withFloor(0.3)), SURFACE, ground, titanGround, BiomeFilter.biome());

        // Sulfur fields: more fumaroles in the yellow crust, low sinter mounds in the sinter.
        PlacementUtils.register(context, SULFUR_FIELDS_YELLOW_CRUST_FUMAROLES, features.getOrThrow(Ring3DressingFeatures.FUMAROLE),
                JitteredGridPlacement.of(8, 2, 0.15F, zoneA()), SURFACE, SlopeFilter.flat(2, 1), ground, BiomeFilter.biome());
        PlacementUtils.register(context, SULFUR_FIELDS_SINTER_MOUNDS, features.getOrThrow(SINTER_MOUND),
                JitteredGridPlacement.of(16, 4, 0.6F, zoneB()), SURFACE, SlopeFilter.flat(3, 1), ground, BiomeFilter.biome());

        // Ash plain, last: the dead trees in groups (also in the lee of the dunes, not only at the gully walls) and the
        // dense stakes of the burnt forest. Straight trunks only: a forked one leans further than the margin of 1.
        PlacementModifier treeSpot = BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.matchesBlocks(BELOW, ModBlocks.ASH_BLOCK.get(), Blocks.GRAVEL, ModBlocks.PUMICE.get(), ModBlocks.SMOLDERING_ASH.get(),
                        Blocks.COARSE_DIRT)));
        PlacementUtils.register(context, ASHEN_WASTES_DEAD_TREE_GROUPS, features.getOrThrow(Ring3DressingFeatures.CHARRED_DEAD_TREE),
                JitteredGridPlacement.of(4, 1, 0.5F, TREE_GROUPS), SURFACE, SlopeFilter.flat(2, 2), ground, treeSpot, BiomeFilter.biome());
        // Grid 8 (a 6 x 6 jitter), not 4: on a grid of 4 the stakes stood in visible rows (G2 after-walk).
        PlacementUtils.register(context, ASHEN_WASTES_BURNT_FOREST, features.getOrThrow(CHARRED_STAKES),
                JitteredGridPlacement.of(8, 1, 0.6F, zoneA()), SURFACE, SlopeFilter.flat(2, 2), ground, treeSpot, BiomeFilter.biome());

        // VEGETAL_DECORATION. Loose dead coral in the hollows of the seabed (higher ground around), dead bushes in the
        // cracks of the clay flats.
        PlacementUtils.register(context, DRIED_SEABED_LOOSE_CORAL, features.getOrThrow(Ring3DressingFeatures.DEAD_CORAL),
                JitteredGridPlacement.of(4, 0, 0.12F), SURFACE, SlopeFilter.wallFoot(4, 2), SlopeFilter.flat(1, 1), ground,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.matchesBlocks(BELOW, Blocks.SAND, Blocks.GRAVEL, Blocks.CLAY, ModBlocks.CRACKED_CLAY.get(), Blocks.TERRACOTTA))),
                BiomeFilter.biome());
        PlacementUtils.register(context, DRIED_SEABED_CLAY_FLAT_BUSHES, features.getOrThrow(Ring3DressingFeatures.DEAD_BUSH),
                JitteredGridPlacement.of(4, 0, 0.15F, zoneA()), SURFACE, ground,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.wouldSurvive(Blocks.DEAD_BUSH.defaultBlockState(), BlockPos.ZERO))),
                BiomeFilter.biome());
        // The charred shrub in small clumps (blobs of a few blocks, ~1–2 per chunk) on the bare plains (smoke test 1:
        // 0,3–1,0 per chunk at chance 0,3 and 0,3 → 0,42, so a little more).
        PlacementUtils.register(context, CHARRED_SHRUBS, features.getOrThrow(CHARRED_SHRUB),
                JitteredGridPlacement.of(2, 0, 0.5F, clumps(6, 16.0, 0.25, 0.4)), SURFACE, ground,
                BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                        BlockPredicate.wouldSurvive(ModBlocks.CHARRED_SHRUB.get().defaultBlockState(), BlockPos.ZERO))),
                BiomeFilter.biome());
    }

    /**
     * Ground and slopes up to the cinder cones: as {@link VolcanicFilter.Builder#ground()}, but on volcano flanks up to
     * 45 blocks above the plain (all cinder cones, the feet of the great volcanoes); never on a lava bank, a cliff, in a
     * crater or a collapse, nor on a needle, arch or island (the found surface on the terrain itself).
     */
    static PlacementModifier slopes() {
        return VolcanicFilter.builder()
                .below(VolcanicOutput.Output.LAVA_BANK, 0.1)
                .below(VolcanicOutput.Output.CLIFF, 0.3)
                .below(VolcanicOutput.Output.VOLCANO_RISE, 45.0)
                .below(VolcanicOutput.Output.CALDERA_MASK, 0.001)
                .below(VolcanicOutput.Output.COLLAPSE, 0.001)
                .groundOnly(VolcanicFilter.DEFAULT_TOLERANCE)
                .build();
    }

    /** Air here, one of {@code ground} below. */
    private static PlacementModifier onBlocks(Block... ground) {
        return BlockPredicateFilter.forPredicate(BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE,
                BlockPredicate.matchesBlocks(BELOW, ground)));
    }

    /** A bone block lying any way. */
    private static BlockStateProvider bones() {
        SimpleWeightedRandomList.Builder<BlockState> bones = SimpleWeightedRandomList.builder();
        for (Direction.Axis axis : Direction.Axis.values()) {
            bones.add(Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis), 1);
        }
        return new WeightedStateProvider(bones);
    }

    /** Block, weight, block, weight, ... */
    private static BlockStateProvider weighted(Object... blockWeightPairs) {
        SimpleWeightedRandomList.Builder<BlockState> list = SimpleWeightedRandomList.builder();
        for (int i = 0; i < blockWeightPairs.length; i += 2) {
            list.add(((Block) blockWeightPairs[i]).defaultBlockState(), (Integer) blockWeightPairs[i + 1]);
        }
        return new WeightedStateProvider(list);
    }

    private static HolderSet<Block> blocks(Block... blocks) {
        return HolderSet.direct(Block::builtInRegistryHolder, blocks);
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }
}
