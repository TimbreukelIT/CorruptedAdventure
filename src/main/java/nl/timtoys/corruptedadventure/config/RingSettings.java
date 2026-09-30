package nl.timtoys.corruptedadventure.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Ring boundaries of one world, in overworld blocks from (0,0).
 * Values come from {@link CommonConfig} once, and are then frozen per world by {@link RingSettingsSnapshot}.
 *
 * @param ringOuterRadii  outer radius of ring 1 to 5; the last one is where the void edge starts
 * @param voidOuterRadius outer radius of the void edge (world border)
 * @param boundaryNoise   maximum seeded offset of a boundary, in either direction
 * @param transitionWidth width of the blend zone centred on each boundary
 */
public record RingSettings(List<Integer> ringOuterRadii, int voidOuterRadius, int boundaryNoise, int transitionWidth) {
    public static final int RING_COUNT = 5;

    public static final Codec<RingSettings> CODEC = RecordCodecBuilder.<RingSettings>create(instance -> instance.group(
            Codec.INT.listOf().fieldOf("ring_outer_radii").forGetter(RingSettings::ringOuterRadii),
            Codec.INT.fieldOf("void_outer_radius").forGetter(RingSettings::voidOuterRadius),
            Codec.INT.fieldOf("boundary_noise").forGetter(RingSettings::boundaryNoise),
            Codec.INT.fieldOf("transition_width").forGetter(RingSettings::transitionWidth)
    ).apply(instance, RingSettings::new)).validate(RingSettings::validate);

    public RingSettings {
        ringOuterRadii = List.copyOf(ringOuterRadii);
    }

    /**
     * Every band (each ring and the void edge) must be wider than two boundary zones can eat into it,
     * otherwise neighbouring boundaries overlap and a position could belong to three rings at once.
     */
    public DataResult<RingSettings> validate() {
        if (ringOuterRadii.size() != RING_COUNT) {
            return DataResult.error(() -> "Expected " + RING_COUNT + " ring radii, got " + ringOuterRadii.size());
        }
        if (boundaryNoise < 0 || transitionWidth < 0) {
            return DataResult.error(() -> "boundary_noise and transition_width must be >= 0");
        }
        int boundaryReach = boundaryNoise + transitionWidth / 2;
        int inner = 0;
        for (int i = 0; i <= RING_COUNT; i++) {
            int outer = i < RING_COUNT ? ringOuterRadii.get(i) : voidOuterRadius;
            // Ring 1 has no inner boundary, so only its outer boundary reaches into it.
            int required = (i == 0 ? 1 : 2) * boundaryReach;
            if (outer - inner <= required) {
                String band = i < RING_COUNT ? "ring " + (i + 1) : "void edge";
                int from = inner;
                return DataResult.error(() -> band + " (" + from + " - " + outer + ") must be wider than " + required
                        + " blocks for boundary_noise " + boundaryNoise + " and transition_width " + transitionWidth);
            }
            inner = outer;
        }
        return DataResult.success(this);
    }
}
