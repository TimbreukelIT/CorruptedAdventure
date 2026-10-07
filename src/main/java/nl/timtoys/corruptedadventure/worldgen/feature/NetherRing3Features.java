package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Datagen bootstraps (configured + placed features) of Nether ring 3. Owned by stream W-nether; empty until the first
 * Nether ring 3 feature. More Nether ring 3 feature classes are called from here, so {@code DataGenerators} stays
 * untouched.
 */
public final class NetherRing3Features {
    /**
     * Region noise ({@code ring_3/nether/region}) from which the rime forest's halls start; the frozen lava sea lies
     * between {@link #RIFTS_THRESHOLD} and this, the glacier rifts below. The terrain's region weights turn over here, and
     * so do the surface rule and (53b) the preset's {@code multi_noise}; {@code NetherRing3JsonTest} compares them.
     */
    public static final double RIME_THRESHOLD = 0.18;
    /** Region noise below which the glacier rifts lie. */
    public static final double RIFTS_THRESHOLD = -0.18;

    private NetherRing3Features() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
    }
}
