package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.Biome;

/**
 * Datagen bootstrap for the Nether ring 3 biomes ({@code docs/plans/ring-3/contract.md} §2). Owned by stream W-nether;
 * empty until the Nether ring 3 biomes exist. More Nether ring 3 biome classes are called from here, so
 * {@code DataGenerators} stays untouched.
 */
public final class NetherRing3Biomes {
    private NetherRing3Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
    }
}
