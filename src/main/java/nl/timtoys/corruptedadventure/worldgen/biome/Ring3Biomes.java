package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.biome.Biome;

/**
 * Datagen bootstrap for the ring 3 overworld biomes, {@code steam_fens} included ({@code docs/plans/ring-3/contract.md} §2).
 * Owned by stream W-biomes; empty until change 39b. More ring 3 biome classes are called from here, so
 * {@code DataGenerators} stays untouched.
 */
public final class Ring3Biomes {
    private Ring3Biomes() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
    }
}
