package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code [nightmares.director]}: the shared system of all five Nightmares (phase 4 steps 2 and 3, fase-4-nightmares-
 * concept.md "Stap 2+3: gedeeld systeem"): the threat per player and ring, the roaming of the virtual position, the
 * arrival, the defeat and the return. Read live by {@code entity/nightmare/director/}.
 */
public final class NightmareDirectorConfig {
    /** One in-game day in seconds. */
    public static final int DAY_SECONDS = 1200;

    public final ModConfigSpec.BooleanValue arrivals;
    public final ModConfigSpec.DoubleValue threatThreshold;
    public final ModConfigSpec.DoubleValue threatDaysMin;
    public final ModConfigSpec.DoubleValue threatDaysMax;
    public final ModConfigSpec.DoubleValue returnDays;
    public final ModConfigSpec.DoubleValue strengthPerReturn;
    public final ModConfigSpec.IntValue maxReturns;
    public final ModConfigSpec.IntValue roamIntervalSeconds;
    public final ModConfigSpec.DoubleValue roamStepBlocks;
    public final ModConfigSpec.IntValue missingSeconds;
    public final ModConfigSpec.IntValue appearRetrySeconds;
    public final ModConfigSpec.IntValue failedArrivalCooldownSeconds;
    public final ModConfigSpec.IntValue defaultOmenSeconds;
    public final ModConfigSpec.DoubleValue defaultArrivalDistance;

    NightmareDirectorConfig(ModConfigSpec.Builder builder) {
        builder.comment("The shared Nightmare system: threat, roaming, arrival, defeat and return (phase 4 steps 2 and 3).")
                .push("director");
        arrivals = builder.comment("Whether threat brings the Nightmares. Off: they only come through /ca nightmare arrive.")
                .define("arrivals", true);
        threatThreshold = builder.comment("Threat (per player and ring) at which the Nightmare of that ring comes for the player",
                        "with the most. It rises only in that ring of the overworld (survival and adventure) and is 0 again",
                        "after the visit.")
                .defineInRange("threatThreshold", 100.0, 1.0, 100000.0);
        threatDaysMin = builder.comment("In-game days in a ring before its threat reaches the threshold, drawn per player and ring",
                        "between min and max each time the threat starts from 0.")
                .defineInRange("threatDaysMin", 2.0, 0.01, 100.0);
        threatDaysMax = builder.defineInRange("threatDaysMax", 3.0, 0.01, 100.0);
        returnDays = builder.comment("In-game days after a defeat (0 HP) before the Nightmare can come again.")
                .defineInRange("returnDays", 3.0, 0.0, 100.0);
        strengthPerReturn = builder.comment("Extra health and damage per defeat, as a part (0.25: 1.25x after one defeat).")
                .defineInRange("strengthPerReturn", 0.25, 0.0, 10.0);
        maxReturns = builder.comment("Defeats that make it stronger; more defeats keep the strength of this many.")
                .defineInRange("maxReturns", 3, 0, 100);
        roamIntervalSeconds = builder.comment("Seconds between two steps of the virtual position through its ring.")
                .defineInRange("roamIntervalSeconds", 60, 1, 3600);
        roamStepBlocks = builder.comment("Blocks of one roaming step.")
                .defineInRange("roamStepBlocks", 32.0, 1.0, 1024.0);
        missingSeconds = builder.comment("Seconds a present Nightmare may be missing or frozen (its chunk unloaded or not ticking)",
                        "before it counts as gone: back to its virtual position, healed, not defeated.")
                .defineInRange("missingSeconds", 15, 1, 3600);
        appearRetrySeconds = builder.comment("Seconds the director keeps asking for room to appear after the omens, before it gives",
                        "up and roams on (the threat stays, so it comes again).")
                .defineInRange("appearRetrySeconds", 20, 1, 600);
        failedArrivalCooldownSeconds = builder.comment("Seconds after such a failed arrival before the Nightmare looks for a target",
                        "again (no new omens every half minute for a player in a boat).")
                .defineInRange("failedArrivalCooldownSeconds", 180, 0, 3600);
        defaultOmenSeconds = builder.comment("Seconds of omens of a Nightmare without its own arrival (the default one).")
                .defineInRange("defaultOmenSeconds", 30, 1, 600);
        defaultArrivalDistance = builder.comment("Blocks behind its target where a Nightmare without its own arrival appears.")
                .defineInRange("defaultArrivalDistance", 10.0, 2.0, 48.0);
        builder.pop();
    }

    /** {@link #threatDaysMin} and {@link #threatDaysMax}, the lower one first. */
    public double[] threatDays() {
        double min = threatDaysMin.get();
        double max = threatDaysMax.get();
        return min <= max ? new double[]{min, max} : new double[]{max, min};
    }

    public long returnTicks() {
        return Math.round(returnDays.get() * DAY_SECONDS * 20);
    }
}
