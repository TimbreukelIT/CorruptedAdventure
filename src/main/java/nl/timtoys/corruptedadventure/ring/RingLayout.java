package nl.timtoys.corruptedadventure.ring;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.config.RingSettingsSnapshot;

import java.util.List;

/**
 * The single source of truth for "which ring is this position in". Worldgen, conditions, borders and the compass
 * all go through {@link #ringAt}.
 * <p>
 * Boundaries are the snapshot radii, shifted by seeded noise of at most {@code boundaryNoise} blocks. The noise is
 * sampled in overworld coordinates, so Nether boundaries line up exactly with the overworld.
 */
public final class RingLayout {
    /** Must match {@code coordinate_scale} of the {@code corrupted_nether} dimension type. */
    public static final int NETHER_SCALE = 4;

    // Two octaves: long waves so no boundary reads as a circle, short ripples so it is never a smooth curve.
    private static final double WAVE_FREQUENCY = 1.0 / 600.0;
    private static final double RIPPLE_FREQUENCY = 1.0 / 150.0;
    private static final double WAVE_WEIGHT = 0.75;
    private static final double RIPPLE_WEIGHT = 0.25;

    // Written on the server thread, read by worldgen worker threads.
    private static volatile RingLayout current;

    private final List<Integer> radii;
    private final int boundaryNoise;
    private final double halfTransition;
    private final SimplexNoise waves;
    private final SimplexNoise ripples;

    public RingLayout(RingSettings settings, long seed) {
        this.radii = settings.ringOuterRadii();
        this.boundaryNoise = settings.boundaryNoise();
        this.halfTransition = settings.transitionWidth() / 2.0;
        RandomSource random = new XoroshiroRandomSource(seed).forkPositional().fromHashOf(CorruptedAdventure.id("ring_boundary"));
        this.waves = new SimplexNoise(random);
        this.ripples = new SimplexNoise(random);
    }

    public static void register(IEventBus gameBus) {
        // LOW: runs after RingSettingsSnapshot has loaded the world's snapshot.
        gameBus.addListener(EventPriority.LOW, RingLayout::onServerAboutToStart);
        gameBus.addListener(RingLayout::onServerStopped);
    }

    /** Ring layout of the running server's world. */
    public static RingLayout current() {
        RingLayout layout = current;
        if (layout == null) {
            throw new IllegalStateException("Ring layout is only available while a server is running");
        }
        return layout;
    }

    /** Ring at a position in the running server's world. */
    public static RingPosition ringAt(ResourceKey<Level> dimension, double x, double z) {
        return current().sample(dimension, x, z);
    }

    public RingPosition sample(ResourceKey<Level> dimension, double x, double z) {
        int scale = scale(dimension);
        return sampleOverworld(x * scale, z * scale);
    }

    /** Overworld blocks per block of {@code dimension}. */
    public static int scale(ResourceKey<Level> dimension) {
        return Level.NETHER.equals(dimension) ? NETHER_SCALE : 1;
    }

    /** @param x overworld x (Nether positions already multiplied by {@link #NETHER_SCALE}) */
    public RingPosition sampleOverworld(double x, double z) {
        double distance = effectiveDistance(x, z);
        for (int i = 0; i < radii.size(); i++) {
            double zoneStart = radii.get(i) - halfTransition;
            if (distance < zoneStart) {
                return RingPosition.within(Ring.byIndex(i));
            }
            if (distance < radii.get(i) + halfTransition) {
                return new RingPosition(Ring.byIndex(i), Ring.byIndex(i + 1), (distance - zoneStart) / (2 * halfTransition));
            }
        }
        return RingPosition.within(Ring.VOID);
    }

    /**
     * Distance from (0,0) shifted by the boundary noise; this is what gets compared against the ring radii, so a
     * position lies exactly on a boundary when this equals that boundary's radius.
     *
     * @param x overworld x (Nether positions already multiplied by {@link #NETHER_SCALE})
     */
    public double effectiveDistance(double x, double z) {
        return Math.sqrt(x * x + z * z) + boundaryOffset(x, z);
    }

    /** Clamped so a boundary never moves more than {@code boundaryNoise}; {@link RingSettings#validate()} relies on that. */
    private double boundaryOffset(double x, double z) {
        double noise = WAVE_WEIGHT * waves.getValue(x * WAVE_FREQUENCY, z * WAVE_FREQUENCY)
                + RIPPLE_WEIGHT * ripples.getValue(x * RIPPLE_FREQUENCY, z * RIPPLE_FREQUENCY);
        return Mth.clamp(noise, -1.0, 1.0) * boundaryNoise;
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        long seed = event.getServer().getWorldData().worldGenOptions().seed();
        current = new RingLayout(RingSettingsSnapshot.current(), seed);
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        current = null;
    }
}
