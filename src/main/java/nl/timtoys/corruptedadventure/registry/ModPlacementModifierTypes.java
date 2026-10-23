package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5Filter;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5VoidFacePlacement;
import nl.timtoys.corruptedadventure.worldgen.feature.BeforeSplitFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.GiantTreeClearance;
import nl.timtoys.corruptedadventure.worldgen.feature.GiantTreePlacement;
import nl.timtoys.corruptedadventure.worldgen.feature.HeightBandFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.InRingFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.JitteredGridPlacement;
import nl.timtoys.corruptedadventure.worldgen.feature.LandmarkClearanceFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.NoiseBandFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring4Filter;
import nl.timtoys.corruptedadventure.worldgen.feature.RingChanceFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.SlopeFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.TerrainHeightPlacement;
import nl.timtoys.corruptedadventure.worldgen.feature.TerrainLandmarkPlacement;
import nl.timtoys.corruptedadventure.worldgen.feature.TerrainLavaFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.VolcanicFilter;

public final class ModPlacementModifierTypes {
    private static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIER_TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, CorruptedAdventure.MOD_ID);

    /** {@code corruptedadventure:ring_chance} */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<RingChanceFilter>> RING_CHANCE =
            PLACEMENT_MODIFIER_TYPES.register("ring_chance", () -> () -> RingChanceFilter.CODEC);
    /** {@code corruptedadventure:jittered_grid} */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<JitteredGridPlacement>> JITTERED_GRID =
            PLACEMENT_MODIFIER_TYPES.register("jittered_grid", () -> () -> JitteredGridPlacement.CODEC);
    /** {@code corruptedadventure:noise_band} */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<NoiseBandFilter>> NOISE_BAND =
            PLACEMENT_MODIFIER_TYPES.register("noise_band", () -> () -> NoiseBandFilter.CODEC);
    /** {@code corruptedadventure:slope} */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<SlopeFilter>> SLOPE =
            PLACEMENT_MODIFIER_TYPES.register("slope", () -> () -> SlopeFilter.CODEC);
    /** {@code corruptedadventure:height_band} */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<HeightBandFilter>> HEIGHT_BAND =
            PLACEMENT_MODIFIER_TYPES.register("height_band", () -> () -> HeightBandFilter.CODEC);

    // ring 3 — W-biomes g2
    /** {@code corruptedadventure:terrain_lava} (change 41) */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<TerrainLavaFilter>> TERRAIN_LAVA =
            PLACEMENT_MODIFIER_TYPES.register("terrain_lava", () -> () -> TerrainLavaFilter.CODEC);
    /** {@code corruptedadventure:terrain_height} (change 41) */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<TerrainHeightPlacement>> TERRAIN_HEIGHT =
            PLACEMENT_MODIFIER_TYPES.register("terrain_height", () -> () -> TerrainHeightPlacement.CODEC);
    /** {@code corruptedadventure:volcanic_filter} (change 47) */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<VolcanicFilter>> VOLCANIC_FILTER =
            PLACEMENT_MODIFIER_TYPES.register("volcanic_filter", () -> () -> VolcanicFilter.CODEC);
    /** {@code corruptedadventure:terrain_landmark} (changes 47, 47b; the 39f accessors) */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<TerrainLandmarkPlacement>> TERRAIN_LANDMARK =
            PLACEMENT_MODIFIER_TYPES.register("terrain_landmark", () -> () -> TerrainLandmarkPlacement.CODEC);
    /** {@code corruptedadventure:in_ring} (change 47d) */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<InRingFilter>> IN_RING =
            PLACEMENT_MODIFIER_TYPES.register("in_ring", () -> () -> InRingFilter.CODEC);
    /** {@code corruptedadventure:before_split} (verdant_falls polish: steam_fens' features off the descent) */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<BeforeSplitFilter>> BEFORE_SPLIT =
            PLACEMENT_MODIFIER_TYPES.register("before_split", () -> () -> BeforeSplitFilter.CODEC);

    // ring 4 — W-biomes g2
    /** {@code corruptedadventure:ring4_filter} (75: the pure ring 4 terrain per column, ground only, clear of falls and knobs) */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<Ring4Filter>> RING4_FILTER =
            PLACEMENT_MODIFIER_TYPES.register("ring4_filter", () -> () -> Ring4Filter.CODEC);
    /** {@code corruptedadventure:giant_tree} (decor D: the trunk of each giant verdant tree beside a gorge in this chunk) */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<GiantTreePlacement>> GIANT_TREE =
            PLACEMENT_MODIFIER_TYPES.register("giant_tree", () -> () -> GiantTreePlacement.CODEC);
    /** {@code corruptedadventure:giant_tree_clearance} (decor D: other trees keep clear of the giant verdant trees) */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<GiantTreeClearance>> GIANT_TREE_CLEARANCE =
            PLACEMENT_MODIFIER_TYPES.register("giant_tree_clearance", () -> () -> GiantTreeClearance.CODEC);

    // ring 4 — decor B
    /** {@code corruptedadventure:landmark_clearance} (decor B: trees, stumps and puddles keep off the blighted landmarks) */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<LandmarkClearanceFilter>> LANDMARK_CLEARANCE =
            PLACEMENT_MODIFIER_TYPES.register("landmark_clearance", () -> () -> LandmarkClearanceFilter.CODEC);

    // ring 5 — W-biomes
    /** {@code corruptedadventure:ring5_filter} (94/95: ring 5's own ground, clear of the void, the walls and the memories) */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<Ring5Filter>> RING5_FILTER =
            PLACEMENT_MODIFIER_TYPES.register("ring5_filter", () -> () -> Ring5Filter.CODEC);
    /** {@code corruptedadventure:ring5_void_face} (95: the outer band's nullite onto rock facing the void) */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<Ring5VoidFacePlacement>> RING5_VOID_FACE =
            PLACEMENT_MODIFIER_TYPES.register("ring5_void_face", () -> () -> Ring5VoidFacePlacement.CODEC);

    private ModPlacementModifierTypes() {
    }

    public static void register(IEventBus modBus) {
        PLACEMENT_MODIFIER_TYPES.register(modBus);
    }
}
