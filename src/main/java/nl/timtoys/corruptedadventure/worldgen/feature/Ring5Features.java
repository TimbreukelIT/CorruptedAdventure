package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Datagen bootstrap of the ring 5 features (keys {@code corruptedadventure:ring_5/<name>}, like
 * {@link Ring4Features}). Owned by stream W-biomes ({@code docs/plans/ring-5/waves.md}); empty until the stream adds features.
 */
public final class Ring5Features {
    private Ring5Features() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
    }
}
