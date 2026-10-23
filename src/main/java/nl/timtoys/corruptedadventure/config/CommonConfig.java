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
    /** Outer radii of ring 1 to 5 in a fresh config (since change 58). */
    public static final List<Integer> DEFAULT_OUTER_RADII = List.of(1500, 3500, 6000, 8000, 9700);
    /** Void edge in a fresh config (since change 58). */
    public static final int DEFAULT_VOID_OUTER_RADIUS = 10200;
    /**
     * Per boundary 1|2, 2|3, 3|4, 4|5, 5|void in a fresh config: the zones 3|4 ({@code steam_fens} plus the
     * {@code verdant_falls} descent, since verdant_falls) and 4|5 (since change 58) are 256 wide.
     */
    public static final int DEFAULT_BOUNDARY_NOISE = 96;
    public static final int DEFAULT_TRANSITION_WIDTH = 128;
    public static final List<Integer> DEFAULT_TRANSITION_WIDTH_OVERRIDES = List.of(0, 0, 256, 256, 0);
    /** Default of {@code transitionWidthOverrides} from change 58 until verdant_falls; moved to the new one on load. */
    static final List<Integer> OLD_DEFAULT_TRANSITION_WIDTH_OVERRIDES = List.of(0, 0, 0, 256, 0);

    static final OuterBoundaries DEFAULT_BOUNDARIES = new OuterBoundaries(DEFAULT_OUTER_RADII, DEFAULT_VOID_OUTER_RADIUS);
    /**
     * Earlier defaults: before change 37c, and from 37c until 58. A config that still holds exactly one of these
     * (radii and void edge) is moved to the new defaults on load.
     */
    static final List<OuterBoundaries> OLD_DEFAULT_BOUNDARIES = List.of(
            new OuterBoundaries(List.of(1500, 3500, 5500, 7500, 9500), 10000),
            new OuterBoundaries(List.of(1500, 3500, 6000, 7800, 9500), 10000));

    private static final List<ModConfigSpec.IntValue> RING_OUTER_RADII = new ArrayList<>();
    private static final ModConfigSpec.IntValue VOID_OUTER_RADIUS;
    private static final ModConfigSpec.IntValue BOUNDARY_NOISE;
    private static final ModConfigSpec.IntValue TRANSITION_WIDTH;
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> TRANSITION_WIDTH_OVERRIDES;

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
                .defineInRange("voidOuterRadius", DEFAULT_VOID_OUTER_RADIUS, 1, maxRadius);
        BOUNDARY_NOISE = builder
                .comment("Maximum seeded offset of each boundary, in either direction.")
                .defineInRange("boundaryNoise", DEFAULT_BOUNDARY_NOISE, 0, 4096);
        TRANSITION_WIDTH = builder
                .comment("Width of the blend zone centred on each boundary without an override in transitionWidthOverrides.")
                .defineInRange("transitionWidth", DEFAULT_TRANSITION_WIDTH, 0, 4096);
        TRANSITION_WIDTH_OVERRIDES = builder
                .comment("Width of the blend zone per boundary, in the order 1|2, 2|3, 3|4, 4|5, 5|void;",
                        "0 uses transitionWidth. Exactly " + RingSettings.BOUNDARY_COUNT + " values.")
                .defineList(List.of("transitionWidthOverrides"), () -> DEFAULT_TRANSITION_WIDTH_OVERRIDES, () -> 0,
                        // Integer only: NeoForge's correction drops a fraction such as 0.5 instead of truncating it, and
                        // RingSettings.validate() then refuses the shorter list when a new world is created.
                        value -> value instanceof Integer width && width >= 0 && width <= 4096,
                        ModConfigSpec.Range.of(RingSettings.BOUNDARY_COUNT, RingSettings.BOUNDARY_COUNT));
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
     * A config file keeps its values when the defaults change, so an untouched older file would give every new world the
     * old ring layout. Moves exactly the old defaults to the new ones; other values are the user's choice and stay.
     */
    private static void migrate() {
        boolean changed = migrateBoundaries();
        // After the radii, so the validity check in migratedOverrides sees the radii new worlds will get.
        changed |= migrateTransitionWidthOverrides();
        if (changed) {
            SPEC.save();
        }
    }

    private static boolean migrateBoundaries() {
        OuterBoundaries current = new OuterBoundaries(radii(), VOID_OUTER_RADIUS.get());
        OuterBoundaries migrated = migrated(current);
        if (!migrated.equals(current)) {
            for (int i = 0; i < RingSettings.RING_COUNT; i++) {
                RING_OUTER_RADII.get(i).set(migrated.ringOuterRadii().get(i));
            }
            VOID_OUTER_RADIUS.set(migrated.voidOuterRadius());
            CorruptedAdventure.LOGGER.info("Moved the ring radii in the common config from the old defaults {} to {}",
                    current, migrated);
            return true;
        }
        if (!current.equals(DEFAULT_BOUNDARIES)) {
            CorruptedAdventure.LOGGER.warn("The common config sets ring radii {} instead of the defaults {}; new worlds use them",
                    current, DEFAULT_BOUNDARIES);
        }
        return false;
    }

    private static boolean migrateTransitionWidthOverrides() {
        RingSettings current = ringSettings();
        RingSettings migrated = migratedOverrides(current);
        if (migrated != current) {
            // A mutable copy, like the list NightConfig itself reads from the file.
            TRANSITION_WIDTH_OVERRIDES.set(new ArrayList<>(migrated.transitionWidthOverrides()));
            CorruptedAdventure.LOGGER.info("Moved transitionWidthOverrides in the common config from the old default {} to {}",
                    current.transitionWidthOverrides(), migrated.transitionWidthOverrides());
            return true;
        }
        if (current.transitionWidthOverrides().equals(OLD_DEFAULT_TRANSITION_WIDTH_OVERRIDES)) {
            CorruptedAdventure.LOGGER.warn("Kept the old default transitionWidthOverrides {} in the common config: with "
                    + "the default {} the configured rings do not validate ({})", current.transitionWidthOverrides(),
                    DEFAULT_TRANSITION_WIDTH_OVERRIDES, withDefaultOverrides(current).validate().error()
                            .map(error -> error.message()).orElse("?"));
        } else if (!current.transitionWidthOverrides().equals(DEFAULT_TRANSITION_WIDTH_OVERRIDES)) {
            CorruptedAdventure.LOGGER.warn("The common config sets transitionWidthOverrides {} instead of the default {}; "
                    + "new worlds use them", current.transitionWidthOverrides(), DEFAULT_TRANSITION_WIDTH_OVERRIDES);
        }
        return false;
    }

    /** The boundaries a config should hold: the new defaults for one of the old defaults, anything else unchanged. */
    static OuterBoundaries migrated(OuterBoundaries current) {
        return OLD_DEFAULT_BOUNDARIES.contains(current) ? DEFAULT_BOUNDARIES : current;
    }

    /**
     * {@code current} with the new default overrides when it holds exactly the old default list, otherwise
     * {@code current} itself. Also unchanged when the wider zone 3|4 would make the other values invalid (rings the user
     * narrowed): a config that worked keeps working.
     */
    static RingSettings migratedOverrides(RingSettings current) {
        if (!OLD_DEFAULT_TRANSITION_WIDTH_OVERRIDES.equals(current.transitionWidthOverrides())) {
            return current;
        }
        RingSettings migrated = withDefaultOverrides(current);
        return migrated.validate().isSuccess() ? migrated : current;
    }

    private static RingSettings withDefaultOverrides(RingSettings settings) {
        return new RingSettings(settings.ringOuterRadii(), settings.voidOuterRadius(), settings.boundaryNoise(),
                settings.defaultTransitionWidth(), DEFAULT_TRANSITION_WIDTH_OVERRIDES);
    }

    /** Ring outer radii and the void edge, the values {@link #migrate} compares as one set. */
    record OuterBoundaries(List<Integer> ringOuterRadii, int voidOuterRadius) {
        OuterBoundaries {
            ringOuterRadii = List.copyOf(ringOuterRadii);
        }
    }

    private static List<Integer> radii() {
        return RING_OUTER_RADII.stream().map(ModConfigSpec.IntValue::get).toList();
    }

    /** Current config values; only {@link RingSettingsSnapshot} should call this, when a new world is created. */
    static RingSettings ringSettings() {
        List<Integer> overrides = List.copyOf(TRANSITION_WIDTH_OVERRIDES.get());
        return new RingSettings(radii(), VOID_OUTER_RADIUS.get(), BOUNDARY_NOISE.get(), TRANSITION_WIDTH.get(), overrides);
    }
}
