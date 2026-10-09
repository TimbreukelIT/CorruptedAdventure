package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.feature.HeightBandFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.JitteredGridPlacement;
import nl.timtoys.corruptedadventure.worldgen.feature.NoiseBandFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.RingChanceFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.SlopeFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.TerrainHeightPlacement;
import nl.timtoys.corruptedadventure.worldgen.feature.TerrainLavaFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.VolcanicFilter;
import nl.timtoys.corruptedadventure.worldgen.feature.VolcanicNearFilter;

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
    /** {@code corruptedadventure:volcanic_near} (change 47b) */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<VolcanicNearFilter>> VOLCANIC_NEAR =
            PLACEMENT_MODIFIER_TYPES.register("volcanic_near", () -> () -> VolcanicNearFilter.CODEC);

    private ModPlacementModifierTypes() {
    }

    public static void register(IEventBus modBus) {
        PLACEMENT_MODIFIER_TYPES.register(modBus);
    }
}
