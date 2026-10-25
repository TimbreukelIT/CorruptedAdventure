package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformFloat;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModFeatures;

import java.util.List;

/**
 * The titan fields (change 47b, V1/V2, G2-WB6 = A): the ribs and rib cages of the terrain (39d) stay the picture; this
 * adds titan skulls near them, loose vertebrae and limb bones, soul fire on soul soil in and around the rib cages, and
 * the rare titan rocks (basalt giants, {@link TitanRockFeature}). Amounts are starting values, tuned at test stop 4b.
 * <p>
 * Determinism as in {@link Ring3DressingFeatures}: everything but the titan rocks lies in its own chunk (placement
 * margin ≥ reach); the ribcages are the pure terrain's ({@link TerrainLandmarkPlacement}, 39f accessor
 * {@code ribcageNear}). The titan rocks are a pure function of seed and cell, each chunk writing its own part.
 */
public final class TitanFieldsFeatures {
    // Configured.
    static final ResourceKey<ConfiguredFeature<?, ?>> TITAN_SKULL = configured("titan_skull");
    static final ResourceKey<ConfiguredFeature<?, ?>> TITAN_BONES = configured("titan_bones");
    static final ResourceKey<ConfiguredFeature<?, ?>> SOUL_FIRE_PATCH = configured("soul_fire_patch");
    static final ResourceKey<ConfiguredFeature<?, ?>> TITAN_ROCK = configured("titan_rock");

    // Placed, in the global order of their step (Ring3Features.ORDER).
    public static final ResourceKey<PlacedFeature> TITAN_FIELDS_SKULLS = placed("titan_fields_skulls");
    public static final ResourceKey<PlacedFeature> TITAN_FIELDS_BONES = placed("titan_fields_bones");
    public static final ResourceKey<PlacedFeature> TITAN_FIELDS_SOUL_FIRE = placed("titan_fields_soul_fire");
    public static final ResourceKey<PlacedFeature> TITAN_FIELDS_SOUL_FIRE_AROUND = placed("titan_fields_soul_fire_around");
    public static final ResourceKey<PlacedFeature> TITAN_FIELDS_ROCKS = placed("titan_fields_rocks");

    /** The grid of the titan rocks (blocks): at most one rock per cell. */
    static final int ROCK_CELL = 256;
    /** A soul patch reaches radius + 1 (stretch 1); near a chunk edge it is cut there (it writes only its own chunk). */
    private static final int SOUL_PATCH_RADIUS = 2;

    private static final PlacementModifier SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);

    private TitanFieldsFeatures() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderSet<Block> ground = ground();
        FeatureUtils.register(context, TITAN_SKULL, ModFeatures.SURFACE_FOSSIL.get(), new SurfaceFossilConfiguration(
                List.of(template("titan_skull_1"), template("titan_skull_2")), UniformFloat.of(0.15F, 0.35F), 0.9F, ground));
        FeatureUtils.register(context, TITAN_BONES, ModFeatures.SURFACE_FOSSIL.get(), new SurfaceFossilConfiguration(
                List.of(template("titan_vertebra_1"), template("titan_vertebra_2"), template("titan_vertebra_3"), template("titan_bone_1"),
                        template("titan_bone_2")), UniformFloat.of(0.2F, 0.5F), 0.85F, ground));
        // Soul soil with a soul fire here and there on it (V2: vanilla, no heat source; it burns for ever on soul soil).
        FeatureUtils.register(context, SOUL_FIRE_PATCH, ModFeatures.GROUND_PATCH.get(), new GroundPatchConfiguration(
                BlockStateProvider.simple(Blocks.SOUL_SOIL), ground, UniformInt.of(1, SOUL_PATCH_RADIUS), 1.0F, ConstantInt.of(1),
                ConstantInt.of(1), new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                        .add(Blocks.AIR.defaultBlockState(), 1)
                        .add(Blocks.SOUL_FIRE.defaultBlockState(), 1))));
        FeatureUtils.register(context, TITAN_ROCK, ModFeatures.TITAN_ROCK.get(), new TitanRockConfiguration(
                List.of(template("titan_rock_kneeling"), template("titan_rock_hunched"), template("titan_rock_bust")), ROCK_CELL, 0.4F,
                ModBiomes.TITAN_FIELDS, 2, HolderSet.direct(Block::builtInRegistryHolder, Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.BLACKSTONE)));
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);
        PlacementModifier ground = VolcanicFilter.builder().ground().build();

        // Skulls lie around the ribcages (up to 12 from a rib foot or vertebra), where the soul fire burns too; loose
        // bones anywhere on level ground.
        PlacementUtils.register(context, TITAN_FIELDS_SKULLS, features.getOrThrow(TITAN_SKULL),
                TerrainLandmarkPlacement.of(TerrainLandmarkPlacement.Landmark.RIBCAGE, 0.1F, 12, 1, 3), SURFACE, SlopeFilter.flat(3, 2), ground,
                BiomeFilter.biome());
        // G2: the bones in clumps (the loose bones of Ring3GroundFeatures share the clumps' salt).
        PlacementUtils.register(context, TITAN_FIELDS_BONES, features.getOrThrow(TITAN_BONES),
                JitteredGridPlacement.of(16, 1, 0.35F, Ring3GroundFeatures.clumps(5, 4.0, 0.15, 0.35).withFloor(0.2)), SURFACE,
                SlopeFilter.flat(3, 2), ground, BiomeFilter.biome());
        // Soul patches around the feet of the ribs and the spine (47d: twice per point, moved off the bone itself, where
        // the ground filter finds no plain ground, and down to 1 from the chunk edge; INT2 found only two).
        PlacementUtils.register(context, TITAN_FIELDS_SOUL_FIRE, features.getOrThrow(SOUL_FIRE_PATCH),
                TerrainLandmarkPlacement.of(TerrainLandmarkPlacement.Landmark.RIBCAGE, 1.0F, 5, 1, 4), SURFACE, ground,
                BiomeFilter.biome());
        PlacementUtils.register(context, TITAN_FIELDS_SOUL_FIRE_AROUND, features.getOrThrow(SOUL_FIRE_PATCH),
                TerrainLandmarkPlacement.of(TerrainLandmarkPlacement.Landmark.RIBCAGE, 1.0F, 9, 1, 5), SURFACE, ground,
                BiomeFilter.biome());
        // Once per chunk without a biome filter: a rock is a function of its cell, and every chunk it covers writes its
        // part (the rock's centre must lie in the titan fields, TitanRockFeature checks that).
        PlacementUtils.register(context, TITAN_FIELDS_ROCKS, features.getOrThrow(TITAN_ROCK));
    }

    /** The open ground of the titan fields (its surface without the bone and calcite of the ribs). */
    private static HolderSet<Block> ground() {
        return HolderSet.direct(Block::builtInRegistryHolder, ModBlocks.ASH_BLOCK.get(), Blocks.SOUL_SOIL, Blocks.BASALT, Blocks.SMOOTH_BASALT,
                ModBlocks.PUMICE.get(), Blocks.BLACKSTONE);
    }

    private static ResourceLocation template(String name) {
        return CorruptedAdventure.id("ring_3/" + name);
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, CorruptedAdventure.id("ring_3/" + name));
    }
}
