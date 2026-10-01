package nl.timtoys.corruptedadventure.ring;

import net.minecraft.server.MinecraftServer;
import nl.timtoys.corruptedadventure.worldgen.chunk.RingChunkGenerator;

/**
 * The one check for "does this world use the rings". Ring systems (border, layout sync, conditions, ring weather)
 * only run in such a world, so a world of another preset with the mod installed stays vanilla.
 */
public final class RingWorld {
    private RingWorld() {
    }

    /** True for a world of the {@code corrupted_world} preset: its overworld generates with {@code ring_noise}. */
    public static boolean isActive(MinecraftServer server) {
        return server.overworld().getChunkSource().getGenerator() instanceof RingChunkGenerator;
    }
}
