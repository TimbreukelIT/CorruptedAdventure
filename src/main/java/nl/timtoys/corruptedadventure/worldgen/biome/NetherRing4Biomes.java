package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.Biome;

/**
 * Datagen bootstrap for the Nether ring 4 biomes ({@code docs/plans/ring-4/contract.md} §2).
 * Owned by stream W-nether; empty until change 65. More classes of this ring are called from here, so
 * {@code DataGenerators} stays untouched.
 */
public final class NetherRing4Biomes {
    private NetherRing4Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
    }
}
