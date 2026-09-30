package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Common config ({@code config/corruptedadventure-common.toml}). The only place where ring boundaries are defined.
 */
public final class CommonConfig {
    public static final ModConfigSpec SPEC;

    private static final List<ModConfigSpec.IntValue> RING_OUTER_RADII = new ArrayList<>();
    private static final ModConfigSpec.IntValue VOID_OUTER_RADIUS;
    private static final ModConfigSpec.IntValue BOUNDARY_NOISE;
    private static final ModConfigSpec.IntValue TRANSITION_WIDTH;

    static {
        int[] defaultRadii = {1500, 3500, 5500, 7500, 9500};
        int maxRadius = 29_999_984;

        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(
                "Ring boundaries in overworld blocks from (0,0); the Nether uses the same values divided by 4.",
                "These are defaults for NEW worlds only: each world stores a snapshot on creation",
                "(<world>/corruptedadventure/ring_settings.json) and keeps using it after this file changes."
        ).push("rings");
        for (int ring = 1; ring <= RingSettings.RING_COUNT; ring++) {
            RING_OUTER_RADII.add(builder
                    .comment("Outer radius of ring " + ring + (ring == RingSettings.RING_COUNT ? "; the void edge starts here." : "."))
                    .defineInRange("ring" + ring + "OuterRadius", defaultRadii[ring - 1], 1, maxRadius));
        }
        VOID_OUTER_RADIUS = builder
                .comment("Outer radius of the void edge; the world border is placed here.")
                .defineInRange("voidOuterRadius", 10000, 1, maxRadius);
        BOUNDARY_NOISE = builder
                .comment("Maximum seeded offset of each boundary, in either direction.")
                .defineInRange("boundaryNoise", 96, 0, 4096);
        TRANSITION_WIDTH = builder
                .comment("Width of the blend zone centred on each boundary.")
                .defineInRange("transitionWidth", 128, 0, 4096);
        builder.pop();
        SPEC = builder.build();
    }

    private CommonConfig() {
    }

    /** Current config values; only {@link RingSettingsSnapshot} should call this, when a new world is created. */
    static RingSettings ringSettings() {
        return new RingSettings(
                RING_OUTER_RADII.stream().map(ModConfigSpec.IntValue::get).toList(),
                VOID_OUTER_RADIUS.get(),
                BOUNDARY_NOISE.get(),
                TRANSITION_WIDTH.get());
    }
}
