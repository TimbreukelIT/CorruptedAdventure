package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.Biome;

/**
 * Datagen bootstrap for the ring 4 overworld biomes, {@code sundered_reach} included ({@code docs/plans/ring-4/contract.md} §2).
 * Owned by stream W-biomes; empty until change 64. More classes of this ring are called from here, so
 * {@code DataGenerators} stays untouched.
 */
public final class Ring4Biomes {
    private Ring4Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
    }
}
