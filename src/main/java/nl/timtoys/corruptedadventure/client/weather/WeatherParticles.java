package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Weather particles in the open air around the camera (ring 3): random spots in a box, only above the
 * {@code MOTION_BLOCKING} heightmap, so never in houses or caves. {@code addParticle} honours vanilla's "Particles"
 * setting (Minimal shows none).
 */
final class WeatherParticles {
    private WeatherParticles() {
    }

    /**
     * About {@code expected} particles this tick (the fraction is rounded at random) with a velocity of
     * {@code velocity} ± {@code spread} per axis.
     *
     * @param radius half the width of the box around the camera
     * @param below  how far below the camera the box starts
     * @param above  how far above the camera the box ends
     */
    static void spawn(ClientLevel level, Vec3 camera, ParticleOptions particle, float expected, double radius,
                      double below, double above, Vec3 velocity, double spread) {
        RandomSource random = level.getRandom();
        int count = (int) expected + (random.nextFloat() < expected - (int) expected ? 1 : 0);
        for (int i = 0; i < count; i++) {
            double x = camera.x + (random.nextDouble() * 2.0 - 1.0) * radius;
            double z = camera.z + (random.nextDouble() * 2.0 - 1.0) * radius;
            double y = camera.y - below + random.nextDouble() * (below + above);
            if (y < level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z))) {
                continue;
            }
            level.addParticle(particle, x, y, z,
                    velocity.x + random.nextGaussian() * spread,
                    velocity.y + random.nextGaussian() * spread,
                    velocity.z + random.nextGaussian() * spread);
        }
    }
}
