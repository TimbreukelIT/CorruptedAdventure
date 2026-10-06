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
    private NetherRing3Features() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
    }
}
