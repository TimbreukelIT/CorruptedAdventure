package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Datagen bootstrap of the Nether ring 5 features (keys {@code corruptedadventure:nether_ring_5/<name>}, like
 * {@link NetherRing4Features}). Owned by stream W-nether ({@code docs/plans/ring-5/waves.md}); empty until the stream adds features.
 */
public final class NetherRing5Features {
    private NetherRing5Features() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
    }
}
