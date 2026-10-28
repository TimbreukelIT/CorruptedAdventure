package nl.timtoys.corruptedadventure.entity.nightmare.director;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import nl.timtoys.corruptedadventure.ring.Ring;

/**
 * How a virtual position wanders through its ring: a heading that turns a little each step, and a step that never leaves
 * the ring (checked with {@code ringAt}); where the way ahead leaves it, it turns aside or back, and with no way out it
 * stays. Server-side randomness, not worldgen: no seed needed.
 */
public final class NightmareRoaming {
    /** A position and heading (degrees, 0 = +z, like a yaw). */
    public record Spot(double x, double z, float heading) {
    }

    private static final float MAX_TURN = 45.0F;
    private static final float[] DETOURS = {0.0F, 60.0F, -60.0F, 120.0F, -120.0F, 180.0F};
    private static final int START_TRIES = 16;
    /** Yaw 0 points to +z (south), 90 to -x (west). */
    private static final String[] COMPASS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    private NightmareRoaming() {
    }

    /** Somewhere in the middle half of the ring's band, at a random angle; inside the ring if one of the tries is. */
    public static Spot start(Ring ring, NightmareRings rings, RandomSource random) {
        double inner = rings.innerRadius(ring);
        double outer = rings.outerRadius(ring);
        double width = outer - inner;
        Spot spot = null;
        for (int i = 0; i < START_TRIES; i++) {
            double radius = inner + width * (0.25 + 0.5 * random.nextDouble());
            double angle = random.nextDouble() * Math.PI * 2.0;
            spot = new Spot(Math.cos(angle) * radius, Math.sin(angle) * radius, random.nextFloat() * 360.0F);
            if (rings.ringAt(spot.x(), spot.z()) == ring) {
                return spot;
            }
        }
        return spot;
    }

    /** A heading as a compass point (S, SW, W, …), for the status. */
    public static String compass(float heading) {
        return COMPASS[Math.floorMod(Math.round(Mth.wrapDegrees(heading) / 45.0F), COMPASS.length)];
    }

    /**
     * One step of {@code blocks} from {@code from}: the turned heading if it stays in {@code ring}, else a detour. Outside
     * its ring (it followed a player out before it went back to virtual) it walks straight back towards the middle of
     * its band instead.
     */
    public static Spot step(Spot from, double blocks, Ring ring, NightmareRings rings, RandomSource random) {
        if (rings.ringAt(from.x(), from.z()) != ring) {
            return homeward(from, blocks, ring, rings);
        }
        float heading = from.heading() + (random.nextFloat() * 2.0F - 1.0F) * MAX_TURN;
        for (float detour : DETOURS) {
            float tried = Mth.wrapDegrees(heading + detour);
            float rad = tried * Mth.DEG_TO_RAD;
            double x = from.x() - Mth.sin(rad) * blocks;
            double z = from.z() + Mth.cos(rad) * blocks;
            if (rings.ringAt(x, z) == ring) {
                return new Spot(x, z, tried);
            }
        }
        return from;
    }

    private static Spot homeward(Spot from, double blocks, Ring ring, NightmareRings rings) {
        double middle = (rings.innerRadius(ring) + rings.outerRadius(ring)) / 2.0;
        double radius = Math.sqrt(from.x() * from.x() + from.z() * from.z());
        if (radius < 1.0e-6) {
            // At the centre: any way out is the way home.
            return new Spot(from.x(), from.z() + Math.min(blocks, middle), 0.0F);
        }
        double move = Math.min(blocks, Math.abs(middle - radius)) * Math.signum(middle - radius);
        double x = from.x() + from.x() / radius * move;
        double z = from.z() + from.z() / radius * move;
        float heading = (float) Math.toDegrees(Math.atan2(-(x - from.x()), z - from.z()));
        return new Spot(x, z, heading);
    }
}
