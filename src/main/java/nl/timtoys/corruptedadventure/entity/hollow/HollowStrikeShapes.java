package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.util.Mth;

/**
 * Where The Hollow's area attacks hit: the band the ground slam's wave sweeps over in one tick, and the arc of the
 * sweep in front of it. Pure, so they can be unit tested.
 */
final class HollowStrikeShapes {
    /** How far (blocks) above or below the wave's ground the feet of whoever it hits may be. */
    static final double WAVE_HEIGHT = 1.0;

    private HollowStrikeShapes() {
    }

    /**
     * Whether the wave, growing from radius {@code before} to {@code now} this tick, hits something whose nearest edge
     * is {@code edge} blocks from its centre and whose feet are {@code dy} above the wave's ground. Only on the ground:
     * a jump lets it pass underneath.
     */
    static boolean waveHits(double before, double now, double edge, double dy, boolean onGround) {
        return onGround && Math.abs(dy) <= WAVE_HEIGHT && edge >= before && edge < now;
    }

    /**
     * Whether the direction {@code dx, dz} lies within the arc of {@code arcDegrees} round the facing {@code yaw}
     * (Minecraft's yaw: 0 = south, +z).
     */
    static boolean inArc(float yaw, double dx, double dz, double arcDegrees) {
        if (dx == 0.0 && dz == 0.0) {
            return true;
        }
        float toward = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        return Math.abs(Mth.wrapDegrees(toward - yaw)) <= arcDegrees * 0.5;
    }
}
