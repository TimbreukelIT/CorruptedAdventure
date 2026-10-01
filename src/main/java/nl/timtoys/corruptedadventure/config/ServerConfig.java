package nl.timtoys.corruptedadventure.config;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Server config ({@code config/corruptedadventure-server.toml}; a copy in {@code <world>/serverconfig/} overrides it
 * for that world), synced to clients by NeoForge on join. The only place for balance values: conditions now, ring weather and its effects later. Only
 * numbers; which effect belongs to which threshold is code. Values are read live, so edits apply without a restart.
 * <p>
 * Readable only while a world is loaded (on the client: once the synced copy has arrived); check
 * {@code SPEC.isLoaded()} in client code.
 */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    public static final DebugCondition DEBUG;

    private static final List<ThresholdLevels> ALL_THRESHOLDS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Environmental conditions: one meter 0-100 per player per condition, updated every second.")
                .push("conditions");
        DEBUG = new DebugCondition(builder);
        builder.pop();
        SPEC = builder.build();
        ALL_THRESHOLDS = List.of(DEBUG.thresholds);
    }

    private ServerConfig() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModConfigEvent.Loading.class, event -> validate(event.getConfig().getSpec()));
        modEventBus.addListener(ModConfigEvent.Reloading.class, event -> validate(event.getConfig().getSpec()));
    }

    private static void validate(Object spec) {
        if (spec == SPEC) {
            ALL_THRESHOLDS.forEach(ThresholdLevels::validate);
        }
    }

    /** {@code [conditions.debug]}: the test condition, driven by {@code /ca condition debug}. */
    public static final class DebugCondition {
        public final ModConfigSpec.DoubleValue riseRate;
        public final ModConfigSpec.DoubleValue fallRate;
        public final ThresholdLevels thresholds;
        public final ModConfigSpec.IntValue effect1Amplifier;
        public final ModConfigSpec.IntValue effect2Amplifier;
        public final ModConfigSpec.DoubleValue damage;
        public final ModConfigSpec.IntValue damageInterval;

        private DebugCondition(ModConfigSpec.Builder builder) {
            builder.comment("Test condition; only rises while '/ca condition debug <player> rise' is set.")
                    .push("debug");
            riseRate = builder.comment("Points per second while set to rise.")
                    .defineInRange("riseRate", 5.0, 0.0, 100.0);
            fallRate = builder.comment("Points per second otherwise.")
                    .defineInRange("fallRate", 5.0, 0.0, 100.0);
            thresholds = ThresholdLevels.define(builder, "conditions.debug", new int[]{50, 75, 100},
                    "Slowness from here.", "Weakness from here.", "Damage from here.");
            effect1Amplifier = builder.comment("Slowness amplifier (0 = level I).")
                    .defineInRange("effect1Amplifier", 0, 0, 9);
            effect2Amplifier = builder.comment("Weakness amplifier (0 = level I).")
                    .defineInRange("effect2Amplifier", 0, 0, 9);
            damage = builder.comment("Damage (half hearts) per hit at threshold3.")
                    .defineInRange("damage", 1.0, 0.0, 100.0);
            damageInterval = builder.comment("Seconds between two hits at threshold3.")
                    .defineInRange("damageInterval", 1, 1, 600);
            builder.pop();
        }
    }
}
