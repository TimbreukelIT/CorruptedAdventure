package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Datagen bootstraps (configured + placed features) of Nether ring 4. Owned by stream W-nether ({@code docs/plans/ring-4/waves.md}); empty until the
 * first Nether ring 4 feature (78). More Nether ring 4 feature classes are called from here, so {@code DataGenerators} stays untouched.
 */
public final class NetherRing4Features {
    /**
     * Region noise ({@code ring_4/nether/region}) from which the crystal spires start; the bone dunes lie between
     * {@link #HOLLOWS_THRESHOLD} and this, the geode hollows below. The terrain's region weights turn over here, and so do
     * the surface rule and the preset's {@code multi_noise}; {@code NetherRing4JsonTest} compares them.
     */
    public static final double SPIRES_THRESHOLD = 0.18;
    /** Region noise below which the geode hollows lie. */
    public static final double HOLLOWS_THRESHOLD = -0.18;

    private NetherRing4Features() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
    }
}
