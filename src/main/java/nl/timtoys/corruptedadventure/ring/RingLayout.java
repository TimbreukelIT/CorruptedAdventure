package nl.timtoys.corruptedadventure.ring;

import com.google.common.hash.Hashing;
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.OptionalDouble;

/**
 * The single source of truth for "which ring is this position in". Worldgen, conditions, borders and the compass
 * all go through {@link #ringAt}.
 * <p>
 * Boundaries are the snapshot radii, shifted by seeded noise of at most {@code boundaryNoise} blocks. The noise is
 * sampled in overworld coordinates, so Nether boundaries line up exactly with the overworld.
 * <p>
 * The noise is seeded with {@link #noiseSeed(long)}, a one-way hash of the world seed, so the layout (settings plus
 * noise seed) can be sent to clients without revealing the world seed. Clients build their own instance from that and
 * get exactly the same {@link #sample} results.
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

    private static final String NOISE_SEED_SALT = CorruptedAdventure.id("ring_boundary").toString();

    private final RingSettings settings;
    private final long noiseSeed;
    private final List<Integer> radii;
    private final int voidOuterRadius;
    private final int boundaryNoise;
    /** Half the transition width per boundary (index 0 = 1|2); each zone is centred on its boundary. */
    private final double[] halfTransitions;
    private final SimplexNoise waves;
    private final SimplexNoise ripples;
    // Ring 4's band axis of this layout (59), built on first use; two threads may both build it, the results are equal.
    private volatile Ring4Band ring4Band;
    // Ring 5's band axis of this layout (82), likewise.
    private volatile Ring5Band ring5Band;

    /** @param noiseSeed seed of the boundary noise; on a server always {@link #noiseSeed(long)} of the world seed */
    public RingLayout(RingSettings settings, long noiseSeed) {
        this.settings = settings;
        this.noiseSeed = noiseSeed;
        this.radii = settings.ringOuterRadii();
        this.voidOuterRadius = settings.voidOuterRadius();
        this.boundaryNoise = settings.boundaryNoise();
        this.halfTransitions = new double[RingSettings.BOUNDARY_COUNT];
        for (int boundary = 0; boundary < halfTransitions.length; boundary++) {
            halfTransitions[boundary] = settings.transitionWidth(boundary) / 2.0;
        }
        RandomSource random = new XoroshiroRandomSource(noiseSeed).forkPositional().fromHashOf(CorruptedAdventure.id("ring_boundary"));
        this.waves = new SimplexNoise(random);
        this.ripples = new SimplexNoise(random);
    }

    /**
     * Seed of the boundary noise for a world: the first 8 bytes (little-endian) of SHA-256(world seed, salt). Like
     * vanilla's {@code BiomeManager.obfuscateSeed} the world seed cannot be recovered from it, and the own salt keeps
     * it different from the hashed seed vanilla already sends to clients.
     */
    public static long noiseSeed(long worldSeed) {
        return Hashing.sha256().newHasher()
                .putLong(worldSeed)
                .putString(NOISE_SEED_SALT, StandardCharsets.UTF_8)
                .hash().asLong();
    }

    public RingSettings settings() {
        return settings;
    }

    public long noiseSeed() {
        return noiseSeed;
    }

    /** The band axis of ring 4 for this layout ({@link Ring4Band#of}). */
    Ring4Band ring4Band() {
        Ring4Band band = ring4Band;
        if (band == null) {
            band = new Ring4Band(this);
            ring4Band = band;
        }
        return band;
    }

    /** The band axis of ring 5 for this layout ({@link Ring5Band#of}). */
    Ring5Band ring5Band() {
        Ring5Band band = ring5Band;
        if (band == null) {
            band = new Ring5Band(this);
            ring5Band = band;
        }
        return band;
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
        return positionAt(effectiveDistance(x, z));
    }

    /** The ring position at an {@link #effectiveDistance}. */
    RingPosition positionAt(double distance) {
        for (int i = 0; i < radii.size(); i++) {
            double halfTransition = halfTransitions[i];
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

    /**
     * How far an {@link #effectiveDistance} lies inside {@code ring}, measured to the nearest of its two boundaries (ring 1
     * has no inner one), in overworld blocks. Negative outside the ring. The boundaries are the radii: the boundary noise is
     * already in the effective distance.
     */
    public double boundaryDepth(Ring ring, double distance) {
        if (ring == Ring.VOID) {
            throw new IllegalArgumentException("The void has no outer boundary");
        }
        int index = ring.ordinal();
        double inner = index == 0 ? Double.POSITIVE_INFINITY : distance - radii.get(index - 1);
        return Math.min(inner, radii.get(index) - distance);
    }

    /**
     * Whether {@code ring} can have weight within {@code margin} of an {@link #effectiveDistance}: inside the ring, in
     * a transition zone with it, or at most {@code margin} (effective distance) beyond. Outside this, every blend of
     * the ring's terrain gives it weight 0.
     */
    public boolean reaches(Ring ring, double distance, double margin) {
        int index = ring.ordinal();
        double lower = index == 0 ? Double.NEGATIVE_INFINITY : radii.get(index - 1) - halfTransitions[index - 1] - margin;
        double upper = index >= radii.size() ? Double.POSITIVE_INFINITY : radii.get(index) + halfTransitions[index] + margin;
        return distance > lower && distance < upper;
    }

    /**
     * {@link #reaches}, where the blend of the ring's outer boundary mixes only over the first {@code outerMix} of that
     * zone ({@code RingBlend}'s {@code mix}, in (0, 1]): beyond that share the ring has no weight, so its reach ends
     * there (plus {@code margin}). 1 is {@link #reaches} itself.
     */
    public boolean reaches(Ring ring, double distance, double margin, double outerMix) {
        int index = ring.ordinal();
        if (outerMix >= 1.0 || index >= radii.size()) {
            return reaches(ring, distance, margin);
        }
        double zoneStart = radii.get(index) - halfTransitions[index];
        return reaches(ring, distance, margin) && distance < zoneStart + 2.0 * halfTransitions[index] * outerMix + margin;
    }

    /**
     * Margin for {@link #reaches} in effective distance: {@code blocks} of the dimension ({@code scale} overworld
     * blocks each), widened in proportion to the boundary noise above its default of 96. The effective distance runs
     * steeper than one block per block where the noise is strong; the margin must still cover a cell's corners.
     */
    public double reachMargin(double blocks, int scale) {
        return blocks * scale * Math.max(1.0, boundaryNoise / 96.0);
    }

    /**
     * How far a position has crossed the void edge: 0 on the noisy outer boundary of ring 5, 1 at the void outer
     * radius (the world border), clamped to 0–1. The far end has no noise because the world border has none either.
     *
     * @param x overworld x (Nether positions already multiplied by {@link #NETHER_SCALE})
     */
    public double voidEdgeProgress(double x, double z) {
        double start = radii.getLast() - boundaryOffset(x, z);
        return Mth.clamp((Math.sqrt(x * x + z * z) - start) / (voidOuterRadius - start), 0.0, 1.0);
    }

    /**
     * How far a position lies across {@code ring}: 0 on its inner boundary (ring 1: at 0,0), towards 1 at its noisy
     * outer boundary. Empty outside the ring, with the same switch as {@link #sampleOverworld}{@code .ring()}.
     * Measured against both boundaries as shifted at this position, so ring 1 starts at 0 in the centre where no
     * boundary noise applies.
     *
     * @param x overworld x (Nether positions already multiplied by {@link #NETHER_SCALE})
     */
    public OptionalDouble ringProgress(Ring ring, double x, double z) {
        if (ring == Ring.VOID) {
            throw new IllegalArgumentException("The void has no outer boundary; use voidEdgeProgress");
        }
        int index = ring.ordinal();
        double distance = Math.sqrt(x * x + z * z);
        double offset = boundaryOffset(x, z);
        double inner = index == 0 ? 0.0 : radii.get(index - 1) - offset;
        double outer = radii.get(index) - offset;
        if (distance < inner || distance >= outer) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of((distance - inner) / (outer - inner));
    }

    /** Clamped so a boundary never moves more than {@code boundaryNoise}; {@link RingSettings#validate()} relies on that. */
    private double boundaryOffset(double x, double z) {
        double noise = WAVE_WEIGHT * waves.getValue(x * WAVE_FREQUENCY, z * WAVE_FREQUENCY)
                + RIPPLE_WEIGHT * ripples.getValue(x * RIPPLE_FREQUENCY, z * RIPPLE_FREQUENCY);
        return Mth.clamp(noise, -1.0, 1.0) * boundaryNoise;
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        long worldSeed = event.getServer().getWorldData().worldGenOptions().seed();
        current = new RingLayout(RingSettingsSnapshot.current(), noiseSeed(worldSeed));
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        current = null;
    }
}
