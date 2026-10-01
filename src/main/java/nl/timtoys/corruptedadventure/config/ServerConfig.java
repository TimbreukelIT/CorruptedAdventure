package nl.timtoys.corruptedadventure.config;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherSettings;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Server config ({@code config/corruptedadventure-server.toml}; a copy in {@code <world>/serverconfig/} overrides it
 * for that world), synced to clients by NeoForge on join. The only place for balance values: conditions and ring weather now, weather effects later. Only
 * numbers; which effect belongs to which threshold is code. Values are read live, so edits apply without a restart.
 * <p>
 * Readable only while a world is loaded (on the client: once the synced copy has arrived); check
 * {@code SPEC.isLoaded()} in client code.
 */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    public static final DebugCondition DEBUG;

    private static final List<ThresholdLevels> ALL_THRESHOLDS;
    /** Per ring with weather types; package-private for tests. */
    static final Map<Ring, RingWeather> RING_WEATHER = new EnumMap<>(Ring.class);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Environmental conditions: one meter 0-100 per player per condition, updated every second.")
                .push("conditions");
        DEBUG = new DebugCondition(builder);
        builder.pop();
        builder.comment("Ring weather: a cycle per ring (clear, then a weather of that ring, then clear again).",
                "Durations in seconds, drawn uniformly between min and max; edits apply from the next change of weather.")
                .push("weather");
        for (Ring ring : RingWeatherType.ringsWithWeather(Level.OVERWORLD)) {
            RING_WEATHER.put(ring, new RingWeather(builder, ring));
        }
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
            RING_WEATHER.values().forEach(RingWeather::validate);
        }
    }

    /**
     * The weather cycle of a ring, read live; {@link RingWeatherSettings#NONE} for a ring without ring weather. A min
     * above its max falls back to the default pair (logged on load).
     */
    public static RingWeatherSettings ringWeather(Ring ring) {
        RingWeather config = RING_WEATHER.get(ring);
        return config == null ? RingWeatherSettings.NONE : config.settings();
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

    /** {@code [weather.ringN]}: the cycle of one ring with weather types. */
    static final class RingWeather {
        static final int DEFAULT_CLEAR_MIN = 600;
        static final int DEFAULT_CLEAR_MAX = 9000;
        static final int DEFAULT_WEATHER_MIN = 600;
        static final int DEFAULT_WEATHER_MAX = 1200;
        static final int DEFAULT_WEIGHT = 1;
        private static final int MAX_SECONDS = 86_400;

        private final String section;
        final ModConfigSpec.IntValue clearMinSeconds;
        final ModConfigSpec.IntValue clearMaxSeconds;
        final ModConfigSpec.IntValue weatherMinSeconds;
        final ModConfigSpec.IntValue weatherMaxSeconds;
        final Map<RingWeatherType, ModConfigSpec.IntValue> weights = new EnumMap<>(RingWeatherType.class);

        private RingWeather(ModConfigSpec.Builder builder, Ring ring) {
            String name = "ring" + ring.number();
            section = "weather." + name;
            builder.push(name);
            clearMinSeconds = builder.comment("Shortest clear spell (vanilla: 600, half a day).")
                    .defineInRange("clearMinSeconds", DEFAULT_CLEAR_MIN, 1, MAX_SECONDS);
            clearMaxSeconds = builder.comment("Longest clear spell (vanilla: 9000, 7.5 days). Not below clearMinSeconds.")
                    .defineInRange("clearMaxSeconds", DEFAULT_CLEAR_MAX, 1, MAX_SECONDS);
            weatherMinSeconds = builder.comment("Shortest weather (vanilla rain: 600).")
                    .defineInRange("weatherMinSeconds", DEFAULT_WEATHER_MIN, 1, MAX_SECONDS);
            weatherMaxSeconds = builder.comment("Longest weather (vanilla rain: 1200). Not below weatherMinSeconds.")
                    .defineInRange("weatherMaxSeconds", DEFAULT_WEATHER_MAX, 1, MAX_SECONDS);
            for (RingWeatherType type : RingWeatherType.typesFor(Level.OVERWORLD, ring)) {
                weights.put(type, builder.comment("Relative chance of " + type.getSerializedName()
                                + " after a clear spell; 0 = never in the cycle.")
                        .defineInRange(weightKey(type), DEFAULT_WEIGHT, 0, 100));
            }
            builder.pop();
        }

        /** {@code heat_wave} → {@code heatWaveWeight}. */
        static String weightKey(RingWeatherType type) {
            StringBuilder key = new StringBuilder();
            boolean upper = false;
            for (char c : type.getSerializedName().toCharArray()) {
                if (c == '_') {
                    upper = true;
                } else {
                    key.append(upper ? Character.toUpperCase(c) : c);
                    upper = false;
                }
            }
            return key.append("Weight").toString();
        }

        RingWeatherSettings settings() {
            int clearMin = clearMinSeconds.get();
            int clearMax = clearMaxSeconds.get();
            if (clearMin > clearMax) {
                clearMin = DEFAULT_CLEAR_MIN;
                clearMax = DEFAULT_CLEAR_MAX;
            }
            int weatherMin = weatherMinSeconds.get();
            int weatherMax = weatherMaxSeconds.get();
            if (weatherMin > weatherMax) {
                weatherMin = DEFAULT_WEATHER_MIN;
                weatherMax = DEFAULT_WEATHER_MAX;
            }
            Map<RingWeatherType, Integer> configured = new EnumMap<>(RingWeatherType.class);
            weights.forEach((type, value) -> configured.put(type, value.get()));
            return RingWeatherSettings.ofSeconds(clearMin, clearMax, weatherMin, weatherMax, configured);
        }

        private void validate() {
            check(clearMinSeconds, clearMaxSeconds, DEFAULT_CLEAR_MIN, DEFAULT_CLEAR_MAX);
            check(weatherMinSeconds, weatherMaxSeconds, DEFAULT_WEATHER_MIN, DEFAULT_WEATHER_MAX);
        }

        private void check(ModConfigSpec.IntValue min, ModConfigSpec.IntValue max, int defaultMin, int defaultMax) {
            if (min.get() > max.get()) {
                CorruptedAdventure.LOGGER.error(String.format(Locale.ROOT,
                        "[%s] %s (%d) is above %s (%d); using the defaults %d-%d", section, min.getPath().getLast(),
                        min.get(), max.getPath().getLast(), max.get(), defaultMin, defaultMax));
            }
        }
    }
}
