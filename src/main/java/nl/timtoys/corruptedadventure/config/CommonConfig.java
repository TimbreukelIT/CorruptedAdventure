package nl.timtoys.corruptedadventure.config;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

import java.util.ArrayList;
import java.util.List;

/**
 * Common config ({@code config/corruptedadventure-common.toml}). The only place where ring boundaries are defined.
 */
public final class CommonConfig {
    public static final ModConfigSpec SPEC;
    /** Outer radii of ring 1 to 5 in a fresh config (since change 37c). */
    public static final List<Integer> DEFAULT_OUTER_RADII = List.of(1500, 3500, 6000, 7800, 9500);
    /** The defaults before change 37c; a config that still holds exactly these is moved to the new defaults on load. */
    static final List<Integer> PRE_37C_OUTER_RADII = List.of(1500, 3500, 5500, 7500, 9500);

    private static final List<ModConfigSpec.IntValue> RING_OUTER_RADII = new ArrayList<>();
    private static final ModConfigSpec.IntValue VOID_OUTER_RADIUS;
    private static final ModConfigSpec.IntValue BOUNDARY_NOISE;
    private static final ModConfigSpec.IntValue TRANSITION_WIDTH;

    static {
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
                    .defineInRange("ring" + ring + "OuterRadius", DEFAULT_OUTER_RADII.get(ring - 1), 1, maxRadius));
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

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModConfigEvent.Loading.class, event -> {
            if (event.getConfig().getSpec() == SPEC) {
                migrate();
            }
        });
    }

    /**
     * A config file keeps its values when the defaults change, so an untouched pre-37c file would give every new world the
     * old, narrower ring 3. Moves exactly the old defaults to the new ones; other values are the user's choice and stay.
     */
    private static void migrate() {
        List<Integer> radii = radii();
        List<Integer> migrated = migrated(radii);
        if (!migrated.equals(radii)) {
            for (int i = 0; i < RingSettings.RING_COUNT; i++) {
                RING_OUTER_RADII.get(i).set(migrated.get(i));
            }
            SPEC.save();
            CorruptedAdventure.LOGGER.info("Moved the ring radii in the common config from the old defaults {} to {}",
                    radii, migrated);
        } else if (!radii.equals(DEFAULT_OUTER_RADII)) {
            CorruptedAdventure.LOGGER.warn("The common config sets ring radii {} instead of the defaults {}; new worlds use them",
                    radii, DEFAULT_OUTER_RADII);
        }
    }

    /** The radii a config should hold: the new defaults for the old ones, anything else unchanged. */
    static List<Integer> migrated(List<Integer> radii) {
        return radii.equals(PRE_37C_OUTER_RADII) ? DEFAULT_OUTER_RADII : radii;
    }

    private static List<Integer> radii() {
        return RING_OUTER_RADII.stream().map(ModConfigSpec.IntValue::get).toList();
    }

    /** Current config values; only {@link RingSettingsSnapshot} should call this, when a new world is created. */
    static RingSettings ringSettings() {
        return new RingSettings(radii(), VOID_OUTER_RADIUS.get(), BOUNDARY_NOISE.get(), TRANSITION_WIDTH.get());
    }
}
