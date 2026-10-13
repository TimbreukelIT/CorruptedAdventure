package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Datagen bootstraps (configured + placed features) of ring 4. Owned by stream W-biomes ({@code docs/plans/ring-4/waves.md}); empty until the
 * first ring 4 feature. More ring 4 feature classes are called from here, so {@code DataGenerators} stays untouched.
 */
public final class Ring4Features {
    private Ring4Features() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
    }
}
