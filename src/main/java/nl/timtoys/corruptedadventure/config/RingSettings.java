package nl.timtoys.corruptedadventure.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Ring boundaries of one world, in overworld blocks from (0,0).
 * Values come from {@link CommonConfig} once, and are then frozen per world by {@link RingSettingsSnapshot}.
 * <p>
 * Boundaries are numbered from 0: boundary {@code i} is the outer boundary of ring {@code i + 1}, so 0 is 1|2 and
 * {@link #BOUNDARY_COUNT}{@code - 1} is 5|void.
 *
 * @param ringOuterRadii           outer radius of ring 1 to 5; the last one is where the void edge starts
 * @param voidOuterRadius          outer radius of the void edge (world border)
 * @param boundaryNoise            maximum seeded offset of a boundary, in either direction
 * @param defaultTransitionWidth   width of the blend zone centred on each boundary without an override (snapshot key
 *                                 {@code transition_width}); read widths through {@link #transitionWidth(int)}
 * @param transitionWidthOverrides per boundary a zone width that replaces {@code defaultTransitionWidth}; 0 = no override
 */
public record RingSettings(List<Integer> ringOuterRadii, int voidOuterRadius, int boundaryNoise, int defaultTransitionWidth,
                           List<Integer> transitionWidthOverrides) {
    public static final int RING_COUNT = 5;
    /** 1|2, 2|3, 3|4, 4|5 and 5|void. */
    public static final int BOUNDARY_COUNT = RING_COUNT;
    /** Every boundary at {@code defaultTransitionWidth}; what a snapshot from before change 58 means. */
    public static final List<Integer> NO_OVERRIDES = Collections.nCopies(BOUNDARY_COUNT, 0);

    public static final Codec<RingSettings> CODEC = RecordCodecBuilder.<RingSettings>create(instance -> instance.group(
            Codec.INT.listOf().fieldOf("ring_outer_radii").forGetter(RingSettings::ringOuterRadii),
            Codec.INT.fieldOf("void_outer_radius").forGetter(RingSettings::voidOuterRadius),
            Codec.INT.fieldOf("boundary_noise").forGetter(RingSettings::boundaryNoise),
            Codec.INT.fieldOf("transition_width").forGetter(RingSettings::defaultTransitionWidth),
            // Optional for snapshots written before change 58, but always written: optionalFieldOf with a default
            // would leave it out whenever it equals that default.
            Codec.INT.listOf().optionalFieldOf("transition_width_overrides")
                    .forGetter(settings -> Optional.of(settings.transitionWidthOverrides()))
    ).apply(instance, (radii, voidOuterRadius, boundaryNoise, defaultWidth, overrides) -> new RingSettings(
            radii, voidOuterRadius, boundaryNoise, defaultWidth, overrides.orElse(NO_OVERRIDES))))
            .validate(RingSettings::validate);

    public RingSettings {
        ringOuterRadii = List.copyOf(ringOuterRadii);
        transitionWidthOverrides = List.copyOf(transitionWidthOverrides);
    }

    /** Settings without overrides: every boundary is {@code defaultTransitionWidth} wide. */
    public RingSettings(List<Integer> ringOuterRadii, int voidOuterRadius, int boundaryNoise, int defaultTransitionWidth) {
        this(ringOuterRadii, voidOuterRadius, boundaryNoise, defaultTransitionWidth, NO_OVERRIDES);
    }

    /** Width of the blend zone centred on {@code boundary} (0 = 1|2 … 4 = 5|void): its override, else {@code defaultTransitionWidth}. */
    public int transitionWidth(int boundary) {
        int override = transitionWidthOverrides.get(boundary);
        return override > 0 ? override : defaultTransitionWidth;
    }

    /**
     * Every band (each ring and the void edge) must be wider than its two boundary zones can eat into it,
     * otherwise neighbouring boundaries overlap and a position could belong to three rings at once.
     */
    public DataResult<RingSettings> validate() {
        if (ringOuterRadii.size() != RING_COUNT) {
            return DataResult.error(() -> "Expected " + RING_COUNT + " ring radii, got " + ringOuterRadii.size());
        }
        if (transitionWidthOverrides.size() != BOUNDARY_COUNT) {
            return DataResult.error(() -> "Expected " + BOUNDARY_COUNT + " transition_width_overrides, got "
                    + transitionWidthOverrides.size());
        }
        if (boundaryNoise < 0 || defaultTransitionWidth < 0 || transitionWidthOverrides.stream().anyMatch(width -> width < 0)) {
            return DataResult.error(() -> "boundary_noise, transition_width and transition_width_overrides must be >= 0");
        }
        int inner = 0;
        for (int i = 0; i <= RING_COUNT; i++) {
            int outer = i < RING_COUNT ? ringOuterRadii.get(i) : voidOuterRadius;
            // Ring 1 has no inner boundary, so only its outer boundary reaches into it. The void edge has no outer
            // boundary of its own; it keeps the old margin of twice its inner one.
            int innerReach = i == 0 ? 0 : boundaryReach(i - 1);
            int outerReach = boundaryReach(Math.min(i, BOUNDARY_COUNT - 1));
            int required = innerReach + outerReach;
            if (outer - inner <= required) {
                String band = i < RING_COUNT ? "ring " + (i + 1) : "void edge";
                int from = inner;
                String widths = i == 0 ? String.valueOf(transitionWidth(0))
                        : transitionWidth(i - 1) + " / " + transitionWidth(Math.min(i, BOUNDARY_COUNT - 1));
                return DataResult.error(() -> band + " (" + from + " - " + outer + ") must be wider than " + required
                        + " blocks for boundary_noise " + boundaryNoise + " and transition width " + widths);
            }
            inner = outer;
        }
        return DataResult.success(this);
    }

    /** How far {@code boundary} can reach into a neighbouring band: its noise plus half its zone. */
    private int boundaryReach(int boundary) {
        return boundaryNoise + transitionWidth(boundary) / 2;
    }
}
