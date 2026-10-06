package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Datagen bootstraps (configured + placed features) of ring 3. Owned by stream W-biomes; empty until the first ring 3
 * feature. More ring 3 feature classes are called from here, so {@code DataGenerators} stays untouched.
 */
public final class Ring3Features {
    private Ring3Features() {
    }

    public static void configured(BootstrapContext<ConfiguredFeature<?, ?>> context) {
    }

    public static void placed(BootstrapContext<PlacedFeature> context) {
    }
}
