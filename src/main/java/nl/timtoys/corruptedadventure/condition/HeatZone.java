package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingWorld;

/**
 * The heat zone for blocks: water that evaporates (44), ice that melts and sponges that dry (55b) ask the same
 * questions here. Players go through {@link ConditionContext}, which has the ring at their feet already.
 */
final class HeatZone {
    private HeatZone() {
    }

    /**
     * Under the open sky in the zone: where water evaporates and a wet sponge dries. The cheap block checks come
     * first; the ring is sampled last.
     */
    static boolean exposed(ServerLevel level, BlockPos pos) {
        return underOpenSky(level, pos) && contains(level, pos);
    }

    /**
     * Ring 3 of a ring world's overworld ({@link HeatRule#inZone}). The dimension first: no ring lookup for blocks in
     * the Nether or the End (frozen oceans elsewhere random-tick a lot of ice).
     */
    static boolean contains(ServerLevel level, BlockPos pos) {
        return level.dimension() == Level.OVERWORLD && RingWorld.isActive(level.getServer())
                && HeatRule.inZone(level.dimension(), RingLayout.ringAt(level.dimension(), pos.getX() + 0.5, pos.getZ() + 0.5));
    }

    /**
     * No solid block or other fluid above {@code pos} (the heightmap, exact at once, so a roof just built counts before
     * the light engine catches up) and full sky light just above it (so leaves shade it).
     */
    static boolean underOpenSky(ServerLevel level, BlockPos pos) {
        return pos.getY() + 1 >= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ())
                && level.canSeeSky(pos.above());
    }
}
