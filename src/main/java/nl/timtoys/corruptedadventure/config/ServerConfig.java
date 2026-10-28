package nl.timtoys.corruptedadventure.config;

import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.Ring5WeatherBands;
import nl.timtoys.corruptedadventure.weather.RingWeatherSettings;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Server config ({@code config/corruptedadventure-server.toml}; a copy in {@code <world>/serverconfig/} overrides it
 * for that world), synced to clients by NeoForge on join. The only place for balance values: conditions, ring weather and its effects, the Nightmares. Only
 * numbers; which effect belongs to which threshold is code. Values are read live, so edits apply without a restart.
 * <p>
 * Readable only while a world is loaded (on the client: once the synced copy has arrived); check
 * {@code SPEC.isLoaded()} in client code.
 */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    public static final DebugCondition DEBUG;
    public static final DehydrationCondition DEHYDRATION;
    /** {@code [conditions.heat]}: ring 3 (change 43). */
    public static final HeatCondition HEAT;
    /** {@code [conditions.cold]}: Nether ring 3 (package 55). */
    public static final ColdCondition COLD;
    /** {@code [conditions.poison]}: ring 4 (package 69). */
    public static final PoisonCondition POISON;
    /** {@code [conditions.purifying]}: Nether ring 4, every meter falls faster (package 79). */
    public static final PurifyingCondition PURIFYING;
    /** {@code [conditions.void]}: overworld ring 5 and the void rim (package 91). */
    public static final VoidCondition VOID;
    /** {@code [conditions.silence]}: Nether ring 5 (package 97). */
    public static final SilenceCondition SILENCE;
    /** {@code [conditions.coating]}: coated armor slows the rising meters (phase 2 golf 1). */
    public static final CoatingEffect COATING;
    /** {@code [weather.ring2]}: the effects of sandstorm and heat wave (change 28). */
    public static final Ring2WeatherEffects RING_2_WEATHER;
    /** {@code [weather.ring3]}: ash cloud duration, heat factors and lava bombs (package 45). */
    public static final Ring3WeatherEffects RING_3_WEATHER;
    /** {@code [weather.ring4]}: the toxic rain's poison factor and the storm's push (package 70). */
    public static final Ring4WeatherEffects RING_4_WEATHER;
    /** {@code [weather.ring5]}: the outer band's weights, the standstill's duration and the void factors (package 92). */
    public static final Ring5WeatherEffects RING_5_WEATHER;
    /** {@code [nightmares.hollow]}: the ring 1 Nightmare (phase 4). */
    public static final HollowConfig HOLLOW;
    /** {@code [nightmares.director]}: threat, roaming, arrival and return of all five Nightmares (phase 4 steps 2 and 3). */
    public static final NightmareDirectorConfig NIGHTMARE_DIRECTOR;
    /** {@code [weakness_items]}: the sunstone flash and the volcanite horn (phase 4, stream I). */
    public static final WeaknessItemsConfig WEAKNESS_ITEMS;

    private static final List<ThresholdLevels> ALL_THRESHOLDS;
    /**
     * {@link #ring5Bands()} as of the last load or reload of the SERVER config (on the client: of the synced copy), so
     * the per-frame and per-player reads build no maps; null before the first load and after an unload.
     */
    private static volatile Ring5WeatherBands.Settings ring5Bands;
    /** Per ring with weather types; package-private for tests. */
    static final Map<Ring, RingWeather> RING_WEATHER = new EnumMap<>(Ring.class);

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Environmental conditions: one meter 0-100 per player per condition, updated every second.")
                .push("conditions");
        DEBUG = new DebugCondition(builder);
        DEHYDRATION = new DehydrationCondition(builder);
        HEAT = new HeatCondition(builder);
        COLD = new ColdCondition(builder);
        POISON = new PoisonCondition(builder);
        PURIFYING = new PurifyingCondition(builder);
        VOID = new VoidCondition(builder);
        SILENCE = new SilenceCondition(builder);
        COATING = new CoatingEffect(builder);
        builder.pop();
        builder.comment("Ring weather: a cycle per ring (clear, then a weather of that ring, then clear again).",
                "Durations in seconds, drawn uniformly between min and max; edits apply from the next change of weather.")
                .push("weather");
        for (Ring ring : RingWeatherType.ringsWithWeather(Level.OVERWORLD)) {
            RING_WEATHER.put(ring, new RingWeather(builder, ring));
        }
        builder.pop();
        RING_2_WEATHER = RING_WEATHER.get(Ring.RING_2).ring2Effects;
        RING_3_WEATHER = RING_WEATHER.get(Ring.RING_3).ring3Effects;
        RING_4_WEATHER = RING_WEATHER.get(Ring.RING_4).ring4Effects;
        RING_5_WEATHER = RING_WEATHER.get(Ring.RING_5).ring5Effects;
        builder.comment("The Nightmares: one roaming boss per ring (phase 4).").push("nightmares");
        HOLLOW = new HollowConfig(builder);
        NIGHTMARE_DIRECTOR = new NightmareDirectorConfig(builder);
        builder.pop();
        WEAKNESS_ITEMS = new WeaknessItemsConfig(builder);
        SPEC = builder.build();
        ALL_THRESHOLDS = List.of(DEBUG.thresholds, DEHYDRATION.thresholds, HEAT.thresholds, COLD.thresholds,
                POISON.thresholds, VOID.thresholds, SILENCE.thresholds);
    }

    private ServerConfig() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModConfigEvent.Loading.class, event -> validate(event.getConfig().getSpec()));
        modEventBus.addListener(ModConfigEvent.Reloading.class, event -> validate(event.getConfig().getSpec()));
        modEventBus.addListener(ModConfigEvent.Unloading.class, event -> {
            if (event.getConfig().getSpec() == SPEC) {
                ring5Bands = null;
            }
        });
    }

    private static void validate(Object spec) {
        if (spec == SPEC) {
            ALL_THRESHOLDS.forEach(ThresholdLevels::validate);
            RING_WEATHER.values().forEach(RingWeather::validate);
            ring5Bands = RING_5_WEATHER.bands(true);
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

    /**
     * What the ring 5 band rule needs from {@code [weather.ring5]} (outer weights, standstill range), read live. Also on
     * the client (the SERVER config is synced): before the synced copy has arrived, the defaults.
     */
    public static Ring5WeatherBands.Settings ring5Bands() {
        Ring5WeatherBands.Settings cached = ring5Bands;
        boolean loaded = SPEC.isLoaded();
        return cached != null && loaded ? cached : RING_5_WEATHER.bands(loaded);
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

    /** {@code [conditions.dehydration]}: ring 2 (change 27). */
    public static final class DehydrationCondition {
        public final ModConfigSpec.DoubleValue riseRate;
        public final ModConfigSpec.DoubleValue fallRateShade;
        public final ModConfigSpec.DoubleValue fallRateNight;
        public final ModConfigSpec.DoubleValue fallRateWater;
        public final ModConfigSpec.DoubleValue fallRateOasis;
        public final ModConfigSpec.DoubleValue fallRateOutside;
        public final ModConfigSpec.DoubleValue waterBottleAmount;
        public final ModConfigSpec.DoubleValue canteenSipAmount;
        public final ModConfigSpec.IntValue canteenCooledSeconds;
        public final ModConfigSpec.DoubleValue cactusJuiceAmount;
        public final ModConfigSpec.IntValue cactusJuiceCooledSeconds;
        public final ModConfigSpec.DoubleValue cooledFactor;
        public final ModConfigSpec.DoubleValue saltedJerkyAmount;
        public final ThresholdLevels thresholds;
        public final ModConfigSpec.IntValue effect1Amplifier;
        public final ModConfigSpec.IntValue effect2Amplifier;
        public final ModConfigSpec.IntValue effect3Amplifier;
        public final ModConfigSpec.DoubleValue damage;
        public final ModConfigSpec.IntValue damageInterval;

        private DehydrationCondition(ModConfigSpec.Builder builder) {
            builder.comment("Dehydration in ring 2 of the overworld. Rises only in direct sunlight by day (full sky light",
                            "at eye height; rain is ignored). Every other case falls; when several apply the strongest",
                            "fall rate wins (they do not stack).")
                    .push("dehydration");
            riseRate = builder.comment("Points per second in direct sunlight by day in ring 2 (0.4: 0 -> 100 in ~4 min).")
                    .defineInRange("riseRate", 0.4, 0.0, 100.0);
            fallRateShade = builder.comment("Points per second in shade by day in ring 2 (no full sky light at eye height).")
                    .defineInRange("fallRateShade", 0.5, 0.0, 100.0);
            fallRateNight = builder.comment("Points per second while the sun is down, in ring 2.")
                    .defineInRange("fallRateNight", 0.5, 0.0, 100.0);
            fallRateWater = builder.comment("Points per second when submerged above the waist (swimming counts), anywhere.")
                    .defineInRange("fallRateWater", 2.0, 0.0, 100.0);
            fallRateOasis = builder.comment("Points per second in the oasis biome, also in full sun.")
                    .defineInRange("fallRateOasis", 2.0, 0.0, 100.0);
            fallRateOutside = builder.comment("Points per second outside ring 2 (other rings, the void, the Nether).")
                    .defineInRange("fallRateOutside", 0.5, 0.0, 100.0);
            waterBottleAmount = builder.comment("Points removed per vanilla water bottle drunk (anywhere, no cooldown).")
                    .defineInRange("waterBottleAmount", 30.0, 0.0, 100.0);
            canteenSipAmount = builder.comment("Points removed per sip from a canteen (phase 2 golf 2; 4 sips per filling).")
                    .defineInRange("canteenSipAmount", 25.0, 0.0, 100.0);
            canteenCooledSeconds = builder.comment("Seconds of 'cooled' per canteen sip.")
                    .defineInRange("canteenCooledSeconds", 45, 0, 3600);
            cactusJuiceAmount = builder.comment("Points removed per cactus juice drunk.")
                    .defineInRange("cactusJuiceAmount", 15.0, 0.0, 100.0);
            cactusJuiceCooledSeconds = builder.comment("Seconds of 'cooled' per cactus juice.")
                    .defineInRange("cactusJuiceCooledSeconds", 20, 0, 3600);
            cooledFactor = builder.comment("While cooled, a rising dehydration rate is multiplied by this (0.5: rises half as",
                            "fast); a falling meter is left alone.")
                    .defineInRange("cooledFactor", 0.5, 0.0, 1.0);
            saltedJerkyAmount = builder.comment("Points added per salted jerky eaten (good food, more thirst).")
                    .defineInRange("saltedJerkyAmount", 8.0, 0.0, 100.0);
            thresholds = ThresholdLevels.define(builder, "conditions.dehydration", new int[]{25, 50, 75, 100},
                    "Hunger from here.", "Slowness from here.", "Mining fatigue from here.", "Damage from here.");
            effect1Amplifier = builder.comment("Hunger amplifier (0 = level I).")
                    .defineInRange("effect1Amplifier", 0, 0, 9);
            effect2Amplifier = builder.comment("Slowness amplifier (0 = level I).")
                    .defineInRange("effect2Amplifier", 0, 0, 9);
            effect3Amplifier = builder.comment("Mining fatigue amplifier (0 = level I).")
                    .defineInRange("effect3Amplifier", 0, 0, 9);
            damage = builder.comment("Damage (half hearts) per hit at threshold4; through armour. Like starving: none on",
                            "peaceful, down to 10 health on easy, down to 1 on normal, lethal on hard.")
                    .defineInRange("damage", 1.0, 0.0, 100.0);
            damageInterval = builder.comment("Seconds between two hits at threshold4 (vanilla starving: 4).")
                    .defineInRange("damageInterval", 4, 1, 600);
            builder.pop();
        }
    }

    /** {@code [conditions.heat]}: ring 3 (change 43); start values from {@code docs/plans/ring-3/contract.md} §5. */
    public static final class HeatCondition {
        public final ModConfigSpec.DoubleValue sunRisePerSecond;
        public final ModConfigSpec.DoubleValue sourceMultiplier;
        public final ModConfigSpec.IntValue sourceRadius;
        public final ModConfigSpec.DoubleValue peakMultiplier;
        public final ModConfigSpec.DoubleValue nightFall;
        public final ModConfigSpec.DoubleValue shadeFall;
        public final ModConfigSpec.DoubleValue undergroundFall;
        public final ModConfigSpec.DoubleValue waterFall;
        public final ModConfigSpec.DoubleValue refugeFall;
        public final ModConfigSpec.DoubleValue outsideFall;
        public final ModConfigSpec.DoubleValue fireResistanceFactor;
        public final ModConfigSpec.DoubleValue cooledFactor;
        public final ModConfigSpec.DoubleValue dewDraughtAmount;
        public final ModConfigSpec.IntValue dewDraughtCooledSeconds;
        public final ThresholdLevels thresholds;
        public final ModConfigSpec.IntValue effect1Amplifier;
        public final ModConfigSpec.IntValue effect2Amplifier;
        public final ModConfigSpec.IntValue effect3Amplifier;

        private HeatCondition(ModConfigSpec.Builder builder) {
            builder.comment("Heat in ring 3 of the overworld. Rises in direct sunlight by day (full sky light at eye height)",
                            "and near a lit heat source (block tag corruptedadventure:heat_sources; also at night and underground).",
                            "The strongest factor wins (source or peak), they do not stack. Every other case falls; water and",
                            "the refuge biomes cool even in the sun or next to a source; the strongest fall rate wins.")
                    .push("heat");
            sunRisePerSecond = builder.comment("Points per second in direct sunlight by day in ring 3 (0.55: 0 -> 100 in ~3 min).")
                    .defineInRange("sunRisePerSecond", 0.55, 0.0, 100.0);
            sourceMultiplier = builder.comment("Factor on the rise within sourceRadius of a heat source.")
                    .defineInRange("sourceMultiplier", 1.5, 1.0, 10.0);
            sourceRadius = builder.comment("Blocks around the player (a box, from feet and head) searched for heat sources.")
                    .defineInRange("sourceRadius", 4, 1, 8);
            peakMultiplier = builder.comment("Factor on the rise in the peak biomes (biome tag corruptedadventure:heat_peaks).")
                    .defineInRange("peakMultiplier", 2.0, 1.0, 10.0);
            nightFall = builder.comment("Points per second while the sun is down, in ring 3, away from heat sources.")
                    .defineInRange("nightFall", 0.5, 0.0, 100.0);
            shadeFall = builder.comment("Points per second by day without full sky light but with no solid block above",
                            "(leaves); deliberately low: shade alone never really cools.")
                    .defineInRange("shadeFall", 0.25, 0.0, 100.0);
            undergroundFall = builder.comment("Points per second underground or under a roof (any solid block above the head),",
                            "away from heat sources.")
                    .defineInRange("undergroundFall", 0.5, 0.0, 100.0);
            waterFall = builder.comment("Points per second when submerged above the waist (swimming counts), anywhere.")
                    .defineInRange("waterFall", 2.0, 0.0, 100.0);
            refugeFall = builder.comment("Points per second in the refuge biomes (biome tag corruptedadventure:heat_refuges),",
                            "also in full sun.")
                    .defineInRange("refugeFall", 2.0, 0.0, 100.0);
            outsideFall = builder.comment("Points per second outside ring 3 (other rings, the void, the Nether).")
                    .defineInRange("outsideFall", 0.5, 0.0, 100.0);
            fireResistanceFactor = builder.comment("Factor on a rising heat rate while the player has fire resistance.")
                    .defineInRange("fireResistanceFactor", 0.5, 0.0, 1.0);
            cooledFactor = builder.comment("While cooled (canteen, cactus juice, dew draught), a rising heat rate is multiplied",
                            "by this (0.5: rises half as fast); a falling meter is left alone. The canteen and the cactus juice",
                            "lower only dehydration, the dew draught only heat.")
                    .defineInRange("cooledFactor", 0.5, 0.0, 1.0);
            dewDraughtAmount = builder.comment("Points removed per dew draught drunk (phase 2 golf 3).")
                    .defineInRange("dewDraughtAmount", 20.0, 0.0, 100.0);
            dewDraughtCooledSeconds = builder.comment("Seconds of 'cooled' per dew draught.")
                    .defineInRange("dewDraughtCooledSeconds", 45, 0, 3600);
            thresholds = ThresholdLevels.define(builder, "conditions.heat", new int[]{25, 50, 75, 100},
                    "Slowness from here.", "Weakness from here.", "Mining fatigue from here.",
                    "Burning from here: on fire and heat damage every second (own death message);"
                            + " not in water, rain or powder snow.");
            effect1Amplifier = builder.comment("Slowness amplifier (0 = level I).")
                    .defineInRange("effect1Amplifier", 0, 0, 9);
            effect2Amplifier = builder.comment("Weakness amplifier (0 = level I).")
                    .defineInRange("effect2Amplifier", 0, 0, 9);
            effect3Amplifier = builder.comment("Mining fatigue amplifier (0 = level I).")
                    .defineInRange("effect3Amplifier", 0, 0, 9);
            builder.pop();
        }
    }

    /**
     * {@code [conditions.cold]}: Nether ring 3 (package 55); start values from the answers G2-C1..C3 in
     * {@code docs/plans/ring-3/golf-2-conditions.md}.
     */
    public static final class ColdCondition {
        public final ModConfigSpec.DoubleValue riseInZone;
        public final ModConfigSpec.DoubleValue iceMultiplier;
        public final ModConfigSpec.DoubleValue exposedMultiplier;
        public final ModConfigSpec.IntValue warmthRadius;
        public final ModConfigSpec.DoubleValue warmthFall;
        public final ModConfigSpec.DoubleValue outsideFall;
        public final ModConfigSpec.DoubleValue leatherReductionPerPiece;
        public final ModConfigSpec.DoubleValue warmedFactor;
        public final ModConfigSpec.DoubleValue handWarmerAmount;
        public final ModConfigSpec.IntValue handWarmerWarmedSeconds;
        public final ThresholdLevels thresholds;
        public final ModConfigSpec.IntValue effect1Amplifier;
        public final ModConfigSpec.IntValue effect2Amplifier;
        public final ModConfigSpec.DoubleValue damage;
        public final ModConfigSpec.IntValue damageInterval;

        private ColdCondition(ModConfigSpec.Builder builder) {
            builder.comment("Cold in ring 3 of the Nether. Rises everywhere in the zone, faster on ice (block tag minecraft:ice)",
                            "and in the exposed biomes (biome tag corruptedadventure:cold_exposed); the strongest factor wins,",
                            "they do not stack. Falls near a lit warmth source (block tag corruptedadventure:warmth_sources)",
                            "and outside the zone. Fire resistance does nothing against the cold.")
                    .push("cold");
            riseInZone = builder.comment("Points per second in the zone, away from warmth (0.42: 0 -> 100 in ~4 min).")
                    .defineInRange("riseInZone", 0.42, 0.0, 100.0);
            iceMultiplier = builder.comment("Factor on the rise while standing on ice.")
                    .defineInRange("iceMultiplier", 1.5, 1.0, 10.0);
            exposedMultiplier = builder.comment("Factor on the rise in the exposed biomes (the open plain).")
                    .defineInRange("exposedMultiplier", 1.5, 1.0, 10.0);
            warmthRadius = builder.comment("Blocks around the player (a box, from feet and head) searched for warmth sources.")
                    .defineInRange("warmthRadius", 4, 1, 8);
            warmthFall = builder.comment("Points per second within warmthRadius of a lit warmth source, in the zone.")
                    .defineInRange("warmthFall", 2.0, 0.0, 100.0);
            outsideFall = builder.comment("Points per second outside Nether ring 3 (other rings, the overworld).")
                    .defineInRange("outsideFall", 0.5, 0.0, 100.0);
            leatherReductionPerPiece = builder.comment("Part of a rising cold rate taken off per worn leather piece",
                            "(item tag minecraft:freeze_immune_wearables); 0.15: full leather 60 %. At most 0.2 (full leather",
                            "80 %), so leather never makes immune.")
                    .defineInRange("leatherReductionPerPiece", 0.15, 0.0, 0.2);
            warmedFactor = builder.comment("While warmed (hand warmer), a rising cold rate is multiplied by this (0.5: rises",
                            "half as fast); a falling meter is left alone.")
                    .defineInRange("warmedFactor", 0.5, 0.0, 1.0);
            handWarmerAmount = builder.comment("Points removed per hand warmer charge used (phase 2 golf 4; 4 charges).")
                    .defineInRange("handWarmerAmount", 20.0, 0.0, 100.0);
            handWarmerWarmedSeconds = builder.comment("Seconds of 'warmed' per hand warmer charge.")
                    .defineInRange("handWarmerWarmedSeconds", 45, 0, 3600);
            thresholds = ThresholdLevels.define(builder, "conditions.cold", new int[]{33, 66, 100},
                    "Slowness from here.", "Mining fatigue from here.",
                    "Freezing from here: vanilla frost (overlay, frozen hearts, powder snow slowness) and cold damage"
                            + " every damageInterval seconds (own death message), also with leather; not while on fire.");
            effect1Amplifier = builder.comment("Slowness amplifier (0 = level I).")
                    .defineInRange("effect1Amplifier", 0, 0, 9);
            effect2Amplifier = builder.comment("Mining fatigue amplifier (0 = level I).")
                    .defineInRange("effect2Amplifier", 0, 0, 9);
            damage = builder.comment("Cold damage per hit at the freezing threshold (1.0 = half a heart).")
                    .defineInRange("damage", 1.0, 0.0, 100.0);
            damageInterval = builder.comment("Seconds between hits at the freezing threshold.")
                    .defineInRange("damageInterval", 2, 1, 60);
            builder.pop();
        }
    }

    /**
     * {@code [conditions.poison]}: ring 4 (package 69); start values from {@code docs/plans/ring-4/contract.md} §5, to be
     * tuned in teststop 5b.
     */
    public static final class PoisonCondition {
        public final ModConfigSpec.DoubleValue poolRise;
        public final ModConfigSpec.IntValue poolRadius;
        public final ModConfigSpec.DoubleValue gasRise;
        public final ModConfigSpec.DoubleValue rainRise;
        public final ModConfigSpec.DoubleValue fall;
        public final ModConfigSpec.DoubleValue outsideFall;
        public final ModConfigSpec.DoubleValue milkAmount;
        public final ModConfigSpec.DoubleValue honeyAmount;
        public final ModConfigSpec.DoubleValue purifiedWaterAmount;
        public final ModConfigSpec.IntValue purifiedWaterCleansedSeconds;
        public final ModConfigSpec.DoubleValue cleansedFactor;
        public final ThresholdLevels thresholds;
        public final ModConfigSpec.IntValue effect1Amplifier;
        public final ModConfigSpec.IntValue effect2Amplifier;
        public final ModConfigSpec.IntValue effect3Amplifier;
        public final ModConfigSpec.DoubleValue damage;
        public final ModConfigSpec.IntValue damageInterval;

        private PoisonCondition(ModConfigSpec.Builder builder) {
            builder.comment("Poison in ring 4 of the overworld. Rises only at a source: in or next to a toxic pool (block tag",
                            "corruptedadventure:poison_sources), head in swamp gas (block tag corruptedadventure:poison_gases),",
                            "toxic rain under the open sky in the blighted band. The strongest source wins, they do not stack.",
                            "Water and the blighted glowcaps are no source. Everywhere else the meter falls.")
                    .push("poison");
            poolRise = builder.comment("Points per second in or next to a toxic pool (3.0: 0 -> 100 in ~35 s).")
                    .defineInRange("poolRise", 3.0, 0.0, 100.0);
            poolRadius = builder.comment("Blocks around the player (a box, from feet and head) that count as next to a pool.")
                    .defineInRange("poolRadius", 1, 1, 4);
            gasRise = builder.comment("Points per second with the head (eye height) in swamp gas (2.0: 0 -> 100 in ~50 s).")
                    .defineInRange("gasRise", 2.0, 0.0, 100.0);
            rainRise = builder.comment("Points per second in full toxic rain under the open sky in the blighted band (0.5:",
                            "0 -> 100 in ~3.5 min); follows the fade of the weather. A factor from the ring 4 weather config",
                            "will be applied on top of it (linked in integration 1).")
                    .defineInRange("rainRise", 0.5, 0.0, 100.0);
            fall = builder.comment("Points per second in ring 4 away from every source.")
                    .defineInRange("fall", 1.0, 0.0, 100.0);
            outsideFall = builder.comment("Points per second outside ring 4 (other rings, the void, the Nether), also next to a",
                            "poison source.")
                    .defineInRange("outsideFall", 1.0, 0.0, 100.0);
            milkAmount = builder.comment("Points removed per milk bucket drunk (anywhere, no cooldown).")
                    .defineInRange("milkAmount", 40.0, 0.0, 100.0);
            honeyAmount = builder.comment("Points removed per honey bottle drunk (anywhere, no cooldown).")
                    .defineInRange("honeyAmount", 20.0, 0.0, 100.0);
            purifiedWaterAmount = builder.comment("Points removed per purified water drunk (phase 2 golf 4).")
                    .defineInRange("purifiedWaterAmount", 20.0, 0.0, 100.0);
            purifiedWaterCleansedSeconds = builder.comment("Seconds of 'cleansed' per purified water.")
                    .defineInRange("purifiedWaterCleansedSeconds", 45, 0, 3600);
            cleansedFactor = builder.comment("While cleansed (purified water), a rising poison rate is multiplied by this (0.5:",
                            "rises half as fast); a falling meter is left alone.")
                    .defineInRange("cleansedFactor", 0.5, 0.0, 1.0);
            thresholds = ThresholdLevels.define(builder, "conditions.poison", new int[]{30, 60, 90},
                    "Nausea from here.", "Vanilla poison from here (never kills: it stops at 1 health).",
                    "Wither from here: black hearts and poison damage every damageInterval seconds (own death message);"
                            + " can kill on normal and hard.");
            effect1Amplifier = builder.comment("Nausea amplifier (0 = level I).")
                    .defineInRange("effect1Amplifier", 0, 0, 9);
            effect2Amplifier = builder.comment("Poison amplifier (0 = level I).")
                    .defineInRange("effect2Amplifier", 0, 0, 9);
            effect3Amplifier = builder.comment("Wither amplifier (0 = level I); only the black hearts, the damage is below.")
                    .defineInRange("effect3Amplifier", 0, 0, 9);
            damage = builder.comment("Poison damage per hit at threshold3 (1.0 = half a heart; vanilla wither I: 1.0 every 2 s);",
                            "through armour. None on peaceful, down to 10 health on easy, lethal on normal and hard.")
                    .defineInRange("damage", 1.0, 0.0, 100.0);
            damageInterval = builder.comment("Seconds between hits at threshold3.")
                    .defineInRange("damageInterval", 2, 1, 60);
            builder.pop();
        }
    }

    /**
     * {@code [conditions.coating]} (phase 2 golf 1, fase-2-items.md §4): no meter of its own; every armor piece with the
     * coating of a condition slows that condition's rise.
     */
    public static final class CoatingEffect {
        public final ModConfigSpec.DoubleValue reductionPerPiece;

        private CoatingEffect(ModConfigSpec.Builder builder) {
            builder.comment("Coatings (armor trims of corruptedadventure:<condition>_coating): every worn piece with the coating of a",
                            "condition slows that condition's rising meter; a falling meter is left alone.")
                    .push("coating");
            reductionPerPiece = builder.comment("Part of a rising rate taken off per coated piece, linearly (0.1875: 1 piece 0.81x,",
                            "2 pieces 0.63x, 3 pieces 0.44x, 4 pieces 0.25x, so a full set rises ~4x slower). At most 0.24, so a",
                            "full set never makes immune.")
                    .defineInRange("reductionPerPiece", 0.1875, 0.0, 0.24);
            builder.pop();
        }
    }

    /**
     * {@code [conditions.purifying]}: Nether ring 4 (package 79). No meter of its own: there every falling meter falls
     * faster. Start value from {@code docs/plans/ring-4/contract.md} §5, to be tuned in teststop 5b.
     */
    public static final class PurifyingCondition {
        public final ModConfigSpec.DoubleValue fallMultiplier;

        private PurifyingCondition(ModConfigSpec.Builder builder) {
            builder.comment("Nether ring 4 purifies (biome tag corruptedadventure:purifying): every condition meter that falls",
                            "there falls faster. A rising meter is left alone.")
                    .push("purifying");
            fallMultiplier = builder.comment("Factor on every falling meter in a purifying biome (3.0: poison falls 3 points per",
                            "second instead of 1). 1.0 turns purifying off.")
                    .defineInRange("fallMultiplier", 3.0, 1.0, 10.0);
            builder.pop();
        }
    }

    /**
     * {@code [conditions.void]}: overworld ring 5 and the void rim (package 91); numbers from
     * {@code docs/plans/ring-5/contract.md} §11, to be tuned in teststop 6b.
     */
    public static final class VoidCondition {
        public final ModConfigSpec.DoubleValue innerRise;
        public final ModConfigSpec.DoubleValue outerRise;
        public final ModConfigSpec.DoubleValue sourceRise;
        public final ModConfigSpec.IntValue sourceRadius;
        public final ModConfigSpec.DoubleValue nightMultiplier;
        public final ModConfigSpec.IntValue lightLevel;
        public final ModConfigSpec.DoubleValue lightFall;
        public final ModConfigSpec.DoubleValue outsideFall;
        public final ModConfigSpec.DoubleValue anchoredFactor;
        public final ModConfigSpec.DoubleValue skyLightAmount;
        public final ModConfigSpec.IntValue skyLightAnchoredSeconds;
        public final ThresholdLevels thresholds;
        public final ModConfigSpec.IntValue teleportMinBlocks;
        public final ModConfigSpec.IntValue teleportMaxBlocks;
        public final ModConfigSpec.IntValue teleportIntervalSeconds;
        public final ModConfigSpec.IntValue darknessAmplifier;
        public final ModConfigSpec.DoubleValue damage;
        public final ModConfigSpec.IntValue damageInterval;

        private VoidCondition(ModConfigSpec.Builder builder) {
            builder.comment("The void in ring 5 of the overworld and on the void rim beyond it (the rim counts as outer band).",
                            "Creeps up everywhere there; faster in the outer band and near a void source (block tag",
                            "corruptedadventure:void_sources). The strongest wins, they do not stack. At night and in ring weather",
                            "it rises faster (the larger factor counts, not both). Block light at eye height keeps it away.")
                    .push("void");
            innerRise = builder.comment("Points per second in the inner band (0.28: 0 -> 100 in ~6 min).")
                    .defineInRange("innerRise", 0.28, 0.0, 100.0);
            outerRise = builder.comment("Points per second in the outer band and on the void rim (0.55: 0 -> 100 in ~3 min).")
                    .defineInRange("outerRise", 0.55, 0.0, 100.0);
            sourceRise = builder.comment("Points per second within sourceRadius of a void source (0.55: 0 -> 100 in ~3 min).")
                    .defineInRange("sourceRise", 0.55, 0.0, 100.0);
            sourceRadius = builder.comment("Blocks around the player (a box, from feet and head) that count as near a void source.")
                    .defineInRange("sourceRadius", 4, 1, 8);
            nightMultiplier = builder.comment("Factor on the rise at night (1.5: inner band 0 -> 100 in ~4 min). Does not stack",
                            "with the ring weather factor ([weather.ring5]): the larger one counts.")
                    .defineInRange("nightMultiplier", 1.5, 1.0, 4.0);
            lightLevel = builder.comment("Block light at eye height from which the meter falls instead (daylight does not count),",
                            "also near a source and in the outer band.")
                    .defineInRange("lightLevel", 12, 1, 15);
            lightFall = builder.comment("Points per second in block light of at least lightLevel (1.0: 100 -> 0 in ~1.7 min).")
                    .defineInRange("lightFall", 1.0, 0.0, 100.0);
            outsideFall = builder.comment("Points per second outside ring 5 and the void rim (other rings, the Nether).")
                    .defineInRange("outsideFall", 1.0, 0.0, 100.0);
            anchoredFactor = builder.comment("While anchored (sky light), a rising void rate is multiplied by this (0.5: rises",
                            "half as fast, after the night and weather factor); a falling meter is left alone.")
                    .defineInRange("anchoredFactor", 0.5, 0.0, 1.0);
            skyLightAmount = builder.comment("Points removed per sky light drunk (phase 2 golf 5).")
                    .defineInRange("skyLightAmount", 20.0, 0.0, 100.0);
            skyLightAnchoredSeconds = builder.comment("Seconds of 'anchored' per sky light.")
                    .defineInRange("skyLightAnchoredSeconds", 45, 0, 3600);
            thresholds = ThresholdLevels.define(builder, "conditions.void", new int[]{30, 60, 90},
                    "Glitch visuals from here (client only, no gameplay effect).",
                    "From here: in the inner band a short teleport every teleportIntervalSeconds while standing on solid"
                            + " ground; in the outer band and on the void rim darkness instead.",
                    "Darkness and void damage every damageInterval seconds from here (own death message); can kill on"
                            + " normal and hard. The teleports of threshold2 go on in the inner band.");
            teleportMinBlocks = builder.comment("Shortest teleport at threshold2, in blocks (horizontal).")
                    .defineInRange("teleportMinBlocks", 1, 1, 16);
            teleportMaxBlocks = builder.comment("Longest teleport at threshold2, in blocks (horizontal); below teleportMinBlocks",
                            "counts as teleportMinBlocks.")
                    .defineInRange("teleportMaxBlocks", 5, 1, 16);
            teleportIntervalSeconds = builder.comment("Seconds between two teleports at threshold2 (the first one this long after",
                            "reaching it in the inner band). Without a safe spot that time there is none; in the air it waits",
                            "for the ground.")
                    .defineInRange("teleportIntervalSeconds", 20, 5, 600);
            darknessAmplifier = builder.comment("Darkness amplifier (0 = level I) at threshold2 in the outer band and at threshold3.")
                    .defineInRange("darknessAmplifier", 0, 0, 9);
            damage = builder.comment("Void damage per hit at threshold3 (1.0 = half a heart, like the poison's wither: 1.0 every",
                            "2 s); through armour. None on peaceful, down to 10 health on easy, lethal on normal and hard.")
                    .defineInRange("damage", 1.0, 0.0, 100.0);
            damageInterval = builder.comment("Seconds between hits at threshold3.")
                    .defineInRange("damageInterval", 2, 1, 60);
            builder.pop();
        }

        /** The longest teleport, never below the shortest. */
        public int teleportMax() {
            return Math.max(teleportMinBlocks.get(), teleportMaxBlocks.get());
        }
    }

    /** {@code [conditions.silence]}: the silence of Nether ring 5 (package 97, ring 5 contract §5, §11). */
    public static final class SilenceCondition {
        public final ModConfigSpec.DoubleValue rise;
        public final ModConfigSpec.DoubleValue openMultiplier;
        public final ModConfigSpec.IntValue companionRadius;
        public final ModConfigSpec.DoubleValue companionMultiplier;
        public final ModConfigSpec.IntValue soundReliefRadius;
        public final ModConfigSpec.IntValue soundReliefSeconds;
        public final ModConfigSpec.DoubleValue soundFall;
        public final ModConfigSpec.DoubleValue outsideFall;
        public final ModConfigSpec.DoubleValue resoundingFactor;
        public final ModConfigSpec.DoubleValue echoShellAmount;
        public final ModConfigSpec.IntValue echoShellResoundingSeconds;
        public final ModConfigSpec.IntValue echoShellCooldownSeconds;
        public final ThresholdLevels thresholds;
        public final ModConfigSpec.IntValue darknessAmplifier;
        public final ModConfigSpec.DoubleValue damage;
        public final ModConfigSpec.IntValue damageInterval;

        private SilenceCondition(ModConfigSpec.Builder builder) {
            builder.comment("The silence in ring 5 of the Nether. Rises everywhere there; faster in open space (the hollow",
                            "cathedral, on the white plateaus), slower with another player close by. A bell, goat horn, echo shell,",
                            "note block or jukebox nearby (also played by redstone) makes it fall for a while, whatever else holds.")
                    .push("silence");
            rise = builder.comment("Points per second in the zone (0.33: 0 -> 100 in ~5 min).")
                    .defineInRange("rise", 0.33, 0.0, 100.0);
            openMultiplier = builder.comment("Factor on the rise in the hollow cathedral and on a white plateau (standing on",
                            "polished white stone) (1.5: ~3.3 min).")
                    .defineInRange("openMultiplier", 1.5, 1.0, 4.0);
            companionRadius = builder.comment("Blocks within which another player (not a spectator) slows the rise.")
                    .defineInRange("companionRadius", 8, 1, 32);
            companionMultiplier = builder.comment("Factor on the rise with at least one other player within companionRadius;",
                            "more players do not stack (0.5: ~10 min, ~6.7 min in open space).")
                    .defineInRange("companionMultiplier", 0.5, 0.0, 1.0);
            soundReliefRadius = builder.comment("Blocks from a bell, goat horn, echo shell, note block or playing jukebox within",
                            "which it counts.")
                    .defineInRange("soundReliefRadius", 16, 1, 64);
            soundReliefSeconds = builder.comment("Seconds the meter keeps falling after the last such sound (a playing jukebox",
                            "sounds every second, so it keeps it falling).")
                    .defineInRange("soundReliefSeconds", 10, 1, 120);
            soundFall = builder.comment("Points per second while a sound relieves (2.0: one bell ring ~ -20, 100 -> 0 in 50 s).")
                    .defineInRange("soundFall", 2.0, 0.0, 100.0);
            outsideFall = builder.comment("Points per second outside Nether ring 5 (other rings, the overworld).")
                    .defineInRange("outsideFall", 1.0, 0.0, 100.0);
            resoundingFactor = builder.comment("While resounding (echo shell), a rising silence rate is multiplied by this (0.5:",
                            "rises half as fast); a falling meter is left alone.")
                    .defineInRange("resoundingFactor", 0.5, 0.0, 1.0);
            echoShellAmount = builder.comment("Points removed from the one who blows the echo shell (phase 2 golf 5); those",
                            "within soundReliefRadius hear it like a goat horn.")
                    .defineInRange("echoShellAmount", 20.0, 0.0, 100.0);
            echoShellResoundingSeconds = builder.comment("Seconds of 'resounding' per blow, for the one who blows it.")
                    .defineInRange("echoShellResoundingSeconds", 45, 0, 3600);
            echoShellCooldownSeconds = builder.comment("Seconds before the echo shell can be blown again.")
                    .defineInRange("echoShellCooldownSeconds", 45, 1, 3600);
            thresholds = ThresholdLevels.define(builder, "conditions.silence", new int[]{30, 60, 90},
                    "Sound muffled from here (client only, no gameplay effect).",
                    "The HUD hidden from here, except the hotbar (client only, no gameplay effect).",
                    "All sound gone, darkness and a heartbeat (damage every damageInterval seconds) from here; never"
                            + " below 1 health.");
            darknessAmplifier = builder.comment("Darkness amplifier (0 = level I) at threshold3.")
                    .defineInRange("darknessAmplifier", 0, 0, 9);
            damage = builder.comment("Heartbeat damage per hit at threshold3 (1.0 = half a heart); through armour, on every",
                            "difficulty, but never below 1 health: the silence never kills.")
                    .defineInRange("damage", 1.0, 0.0, 100.0);
            damageInterval = builder.comment("Seconds between heartbeats at threshold3.")
                    .defineInRange("damageInterval", 2, 1, 60);
            builder.pop();
        }
    }

    /** The effects of the ring 2 weather, in {@code [weather.ring2]} next to its cycle (change 28). */
    public static final class Ring2WeatherEffects {
        public final ModConfigSpec.DoubleValue heatWaveDehydrationMultiplier;
        public final ModConfigSpec.DoubleValue sandstormPushStrength;
        public final ModConfigSpec.IntValue sandstormGustTicks;

        private Ring2WeatherEffects(ModConfigSpec.Builder builder) {
            heatWaveDehydrationMultiplier = builder.comment("Factor on rising dehydration during a full heat wave; follows the",
                            "fade of the weather (1 + (factor - 1) x intensity). Only the rise in the sun, never the fall.")
                    .defineInRange("heatWaveDehydrationMultiplier", 2.0, 1.0, 5.0);
            sandstormPushStrength = builder.comment("Wind push per gust during a full sandstorm, in blocks per tick, for players",
                            "in ring 2 who see the sky (0.08 with a gust every 10 ticks: ~0.35 blocks per second drift).")
                    .defineInRange("sandstormPushStrength", 0.08, 0.0, 0.5);
            sandstormGustTicks = builder.comment("Ticks between two wind gusts (one small packet per exposed player per gust).")
                    .defineInRange("sandstormGustTicks", 10, 5, 100);
        }
    }

    /**
     * The extras of the ring 3 weather, in {@code [weather.ring3]} next to its cycle (package 45): the ash cloud's own
     * duration, the heat factors (read by the heat modifiers of stream C) and the lava bombs of an eruption.
     */
    public static final class Ring3WeatherEffects {
        static final int DEFAULT_ASH_CLOUD_MIN = 120;
        static final int DEFAULT_ASH_CLOUD_MAX = 240;

        public final ModConfigSpec.IntValue ashCloudMinSeconds;
        public final ModConfigSpec.IntValue ashCloudMaxSeconds;
        public final ModConfigSpec.DoubleValue ashRainHeatMultiplier;
        public final ModConfigSpec.DoubleValue eruptionHeatMultiplier;
        public final ModConfigSpec.DoubleValue ashCloudHeatMultiplier;
        public final ModConfigSpec.DoubleValue lavaBombsPerMinute;
        public final ModConfigSpec.IntValue lavaBombRadius;
        public final ModConfigSpec.DoubleValue lavaBombPeakFactor;

        private Ring3WeatherEffects(ModConfigSpec.Builder builder) {
            ashCloudMinSeconds = builder.comment("Shortest ash cloud; it is shorter than the other ring 3 weathers (its own",
                            "range instead of weatherMinSeconds/weatherMaxSeconds). The first 20 s are the warning.")
                    .defineInRange("ashCloudMinSeconds", DEFAULT_ASH_CLOUD_MIN, 1, RingWeather.MAX_SECONDS);
            ashCloudMaxSeconds = builder.comment("Longest ash cloud. Not below ashCloudMinSeconds.")
                    .defineInRange("ashCloudMaxSeconds", DEFAULT_ASH_CLOUD_MAX, 1, RingWeather.MAX_SECONDS);
            ashRainHeatMultiplier = builder.comment("Factor on the rising heat meter during full ash rain; follows the fade of",
                            "the weather (1 + (factor - 1) x intensity).")
                    .defineInRange("ashRainHeatMultiplier", 1.5, 1.0, 10.0);
            eruptionHeatMultiplier = builder.comment("Factor on the rising heat meter during a full eruption.")
                    .defineInRange("eruptionHeatMultiplier", 2.0, 1.0, 10.0);
            ashCloudHeatMultiplier = builder.comment("Factor on the rising heat meter in a full ash cloud.")
                    .defineInRange("ashCloudHeatMultiplier", 3.0, 1.0, 10.0);
            lavaBombsPerMinute = builder.comment("Lava bombs (small fireballs) per minute around each player in ring 3 under the",
                            "open sky, during a full eruption (scaled by its fade). Fire on landing follows mobGriefing.")
                    .defineInRange("lavaBombsPerMinute", 12.0, 0.0, 60.0);
            lavaBombRadius = builder.comment("Lava bombs land at most this many blocks from the player (horizontally).")
                    .defineInRange("lavaBombRadius", 40, 8, 96);
            lavaBombPeakFactor = builder.comment("Factor on lavaBombsPerMinute while the player stands in a #corruptedadventure:heat_peaks",
                            "biome. lavaBombsPerMinute x lavaBombPeakFactor is the cap per player per minute.")
                    .defineInRange("lavaBombPeakFactor", 2.0, 1.0, 4.0);
        }

        /** The heat factor of a full {@code type} (ash rain, eruption, ash cloud); 1 for every other type. */
        public double heatMultiplier(RingWeatherType type) {
            return heatMultiplier(type, ModConfigSpec.DoubleValue::get);
        }

        /** {@link #heatMultiplier(RingWeatherType)} with its own read of the values (a unit test reads the defaults). */
        public double heatMultiplier(RingWeatherType type, ToDoubleFunction<ModConfigSpec.DoubleValue> value) {
            return switch (type) {
                case ASH_RAIN -> value.applyAsDouble(ashRainHeatMultiplier);
                case ERUPTION -> value.applyAsDouble(eruptionHeatMultiplier);
                case ASH_CLOUD -> value.applyAsDouble(ashCloudHeatMultiplier);
                default -> 1.0;
            };
        }

        /** The ash cloud's own duration in seconds; a min above its max falls back to the defaults. */
        RingWeatherSettings.Range ashCloudSeconds() {
            int min = ashCloudMinSeconds.get();
            int max = ashCloudMaxSeconds.get();
            return min > max ? new RingWeatherSettings.Range(DEFAULT_ASH_CLOUD_MIN, DEFAULT_ASH_CLOUD_MAX)
                    : new RingWeatherSettings.Range(min, max);
        }
    }

    /**
     * The extras of the ring 4 weather, in {@code [weather.ring4]} next to its cycle (package 70): the toxic rain's
     * poison factor (read by the poison modifier of stream C) and the storm's push high up.
     */
    public static final class Ring4WeatherEffects {
        public final ModConfigSpec.DoubleValue toxicRainPoisonMultiplier;
        public final ModConfigSpec.DoubleValue stormPushStrength;
        public final ModConfigSpec.IntValue stormPushMinY;

        private Ring4WeatherEffects(ModConfigSpec.Builder builder) {
            toxicRainPoisonMultiplier = builder.comment("Factor on the poison that toxic rain adds under the open sky (applied by the",
                            "ToxicRainPoisonModifier at the weather stage; 1 = the rate in [conditions.poison]). Toxic rain falls only in ring 4's blighted band.")
                    .defineInRange("toxicRainPoisonMultiplier", 1.0, 0.0, 10.0);
            stormPushStrength = builder.comment("Wind push per gust (every 10 ticks) during a full storm, in blocks per tick, for",
                            "players in ring 4's mythical band at or above stormPushMinY who see the sky. 0 = no push.")
                    .defineInRange("stormPushStrength", 0.05, 0.0, 0.5);
            stormPushMinY = builder.comment("The storm pushes only at or above this height (feet), on and between the islands;",
                            "lower down it is only atmosphere.")
                    .defineInRange("stormPushMinY", 120, -64, 320);
        }
    }

    /**
     * The extras of the ring 5 weather, in {@code [weather.ring5]} next to its cycle (package 92, OV1 = A): the cycle's
     * own weights ({@code glitchStormWeight} etc.) draw the inner band's type; the outer band draws its own type from
     * the same weather's seed with the weights here. The standstill lasts its own short range in both bands. The void
     * factors are read by the void modifier of stream C.
     */
    public static final class Ring5WeatherEffects {
        static final int DEFAULT_STANDSTILL_MIN = 40;
        static final int DEFAULT_STANDSTILL_MAX = 120;
        /** Outer band defaults (P2, 2026-10-21): reverse rain 70, glitch storm 20, standstill 10. */
        static final int DEFAULT_OUTER_GLITCH_STORM = 20;
        static final int DEFAULT_OUTER_REVERSE_RAIN = 70;
        static final int DEFAULT_OUTER_STANDSTILL = 10;

        public final ModConfigSpec.IntValue outerGlitchStormWeight;
        public final ModConfigSpec.IntValue outerReverseRainWeight;
        public final ModConfigSpec.IntValue outerStandstillWeight;
        public final ModConfigSpec.IntValue standstillMinSeconds;
        public final ModConfigSpec.IntValue standstillMaxSeconds;
        public final ModConfigSpec.DoubleValue glitchStormVoidMultiplier;
        public final ModConfigSpec.DoubleValue reverseRainVoidMultiplier;
        public final ModConfigSpec.DoubleValue standstillVoidMultiplier;

        private Ring5WeatherEffects(ModConfigSpec.Builder builder) {
            outerGlitchStormWeight = builder.comment("Outer band: relative chance of glitch_storm when the ring has weather. The cycle's",
                            "own weights above (glitchStormWeight, reverseRainWeight, standstillWeight) are the inner band's; both",
                            "bands share the timing, each draws its own type. All outer weights 0 = the outer band stays clear.",
                            "Changing an outer weight while a weather runs changes that weather's outer type at once; a client",
                            "follows when its synced copy of this config is updated.")
                    .defineInRange("outerGlitchStormWeight", DEFAULT_OUTER_GLITCH_STORM, 0, 100);
            outerReverseRainWeight = builder.comment("Outer band: relative chance of reverse_rain.")
                    .defineInRange("outerReverseRainWeight", DEFAULT_OUTER_REVERSE_RAIN, 0, 100);
            outerStandstillWeight = builder.comment("Outer band: relative chance of standstill.")
                    .defineInRange("outerStandstillWeight", DEFAULT_OUTER_STANDSTILL, 0, 100);
            standstillMinSeconds = builder.comment("Shortest standstill; it is short (its own range instead of",
                            "weatherMinSeconds/weatherMaxSeconds, in both bands).")
                    .defineInRange("standstillMinSeconds", DEFAULT_STANDSTILL_MIN, 1, RingWeather.MAX_SECONDS);
            standstillMaxSeconds = builder.comment("Longest standstill. Not below standstillMinSeconds.")
                    .defineInRange("standstillMaxSeconds", DEFAULT_STANDSTILL_MAX, 1, RingWeather.MAX_SECONDS);
            glitchStormVoidMultiplier = builder.comment("Factor on the rising void meter during a full glitch storm. Applied by stream C's",
                            "void modifier; the largest of night and weather counts (they do not stack).")
                    .defineInRange("glitchStormVoidMultiplier", 2.0, 1.0, 10.0);
            reverseRainVoidMultiplier = builder.comment("Factor on the rising void meter during full reverse rain (same rule; 1 or more: the meter is only amplified).")
                    .defineInRange("reverseRainVoidMultiplier", 1.5, 1.0, 10.0);
            standstillVoidMultiplier = builder.comment("Factor on the rising void meter during a full standstill (same rule; 1 or more: the meter is only amplified).")
                    .defineInRange("standstillVoidMultiplier", 1.5, 1.0, 10.0);
        }

        /** The void factor of a full {@code type} (glitch storm, reverse rain, standstill); 1 for every other type. */
        public double voidMultiplier(RingWeatherType type) {
            return voidMultiplier(type, ModConfigSpec.DoubleValue::get);
        }

        /** {@link #voidMultiplier(RingWeatherType)} with its own read of the values (a unit test reads the defaults). */
        public double voidMultiplier(RingWeatherType type, ToDoubleFunction<ModConfigSpec.DoubleValue> value) {
            return switch (type) {
                case GLITCH_STORM -> value.applyAsDouble(glitchStormVoidMultiplier);
                case REVERSE_RAIN -> value.applyAsDouble(reverseRainVoidMultiplier);
                case STANDSTILL -> value.applyAsDouble(standstillVoidMultiplier);
                default -> 1.0;
            };
        }

        /** The standstill's own duration in seconds; a min above its max falls back to the defaults. */
        RingWeatherSettings.Range standstillSeconds() {
            return standstillSeconds(standstillMinSeconds.get(), standstillMaxSeconds.get());
        }

        static RingWeatherSettings.Range standstillSeconds(int min, int max) {
            return min > max ? new RingWeatherSettings.Range(DEFAULT_STANDSTILL_MIN, DEFAULT_STANDSTILL_MAX)
                    : new RingWeatherSettings.Range(min, max);
        }

        /** The band rule's settings: the configured values when {@code loaded}, else the defaults. */
        Ring5WeatherBands.Settings bands(boolean loaded) {
            Map<RingWeatherType, Integer> weights = new EnumMap<>(RingWeatherType.class);
            weights.put(RingWeatherType.GLITCH_STORM, loaded ? outerGlitchStormWeight.get() : outerGlitchStormWeight.getDefault());
            weights.put(RingWeatherType.REVERSE_RAIN, loaded ? outerReverseRainWeight.get() : outerReverseRainWeight.getDefault());
            weights.put(RingWeatherType.STANDSTILL, loaded ? outerStandstillWeight.get() : outerStandstillWeight.getDefault());
            RingWeatherSettings.Range seconds = loaded ? standstillSeconds()
                    : standstillSeconds(standstillMinSeconds.getDefault(), standstillMaxSeconds.getDefault());
            return new Ring5WeatherBands.Settings(weights, new RingWeatherSettings.Range(
                    seconds.min() * RingWeatherSettings.TICKS_PER_SECOND, seconds.max() * RingWeatherSettings.TICKS_PER_SECOND));
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
        /** Only in {@code [weather.ring2]}; null for the other rings. */
        final Ring2WeatherEffects ring2Effects;
        /** Only in {@code [weather.ring3]}; null for the other rings. */
        final Ring3WeatherEffects ring3Effects;
        /** Only in {@code [weather.ring4]}; null for the other rings. */
        final Ring4WeatherEffects ring4Effects;
        /** Only in {@code [weather.ring5]}; null for the other rings. */
        final Ring5WeatherEffects ring5Effects;

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
                                + " after a clear spell; 0 = never in the cycle." + (ring == Ring.RING_5 ? " Inner band." : ""))
                        .defineInRange(weightKey(type), defaultWeight(type), 0, 100));
            }
            ring2Effects = ring == Ring.RING_2 ? new Ring2WeatherEffects(builder) : null;
            ring3Effects = ring == Ring.RING_3 ? new Ring3WeatherEffects(builder) : null;
            ring4Effects = ring == Ring.RING_4 ? new Ring4WeatherEffects(builder) : null;
            ring5Effects = ring == Ring.RING_5 ? new Ring5WeatherEffects(builder) : null;
            builder.pop();
        }

        /**
         * Equal weights, except ring 3: ash rain 45, eruption 40, the rare ash cloud 15 (decision V6, 2026-10-06); and
         * ring 5's inner band: glitch storm 55, standstill 30, reverse rain 15 (P2, 2026-10-21).
         */
        static int defaultWeight(RingWeatherType type) {
            return switch (type) {
                case ASH_RAIN -> 45;
                case ERUPTION -> 40;
                case ASH_CLOUD -> 15;
                case GLITCH_STORM -> 55;
                case REVERSE_RAIN -> 15;
                case STANDSTILL -> 30;
                default -> DEFAULT_WEIGHT;
            };
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
            Map<RingWeatherType, RingWeatherSettings.Range> durations = new EnumMap<>(RingWeatherType.class);
            if (ring3Effects != null) {
                durations.put(RingWeatherType.ASH_CLOUD, ring3Effects.ashCloudSeconds());
            }
            if (ring5Effects != null) {
                durations.put(RingWeatherType.STANDSTILL, ring5Effects.standstillSeconds());
            }
            return RingWeatherSettings.ofSeconds(clearMin, clearMax, weatherMin, weatherMax, configured, durations);
        }

        private void validate() {
            check(clearMinSeconds, clearMaxSeconds, DEFAULT_CLEAR_MIN, DEFAULT_CLEAR_MAX);
            check(weatherMinSeconds, weatherMaxSeconds, DEFAULT_WEATHER_MIN, DEFAULT_WEATHER_MAX);
            if (ring3Effects != null) {
                check(ring3Effects.ashCloudMinSeconds, ring3Effects.ashCloudMaxSeconds,
                        Ring3WeatherEffects.DEFAULT_ASH_CLOUD_MIN, Ring3WeatherEffects.DEFAULT_ASH_CLOUD_MAX);
            }
            if (ring5Effects != null) {
                check(ring5Effects.standstillMinSeconds, ring5Effects.standstillMaxSeconds,
                        Ring5WeatherEffects.DEFAULT_STANDSTILL_MIN, Ring5WeatherEffects.DEFAULT_STANDSTILL_MAX);
            }
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
