package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code [nightmares.unfinished.arrival]}: how The Unfinished comes (phase 4 step 2, fase-4-nightmares-concept.md "Stap 2
 * voor ring 2–5"): the standstill falls round his target, you hear him climb, he tears the ground open down to the void and
 * climbs out, and a small shallow void scar stays. While he roams, the rain hangs still where he passed.
 */
public final class UnfinishedArrivalConfig {
    public final ModConfigSpec.IntValue omenSeconds;
    public final ModConfigSpec.IntValue silenceSeconds;
    public final ModConfigSpec.DoubleValue standstillRadius;
    public final ModConfigSpec.IntValue standstillLingerSeconds;
    public final ModConfigSpec.DoubleValue arrivalDistance;
    public final ModConfigSpec.IntValue tearWarnTicks;
    public final ModConfigSpec.IntValue tearLength;
    public final ModConfigSpec.IntValue tearWidth;
    public final ModConfigSpec.IntValue tearOpenSeconds;
    public final ModConfigSpec.IntValue climbTicks;
    public final ModConfigSpec.IntValue climbDepth;
    public final ModConfigSpec.BooleanValue scarEnabled;
    public final ModConfigSpec.IntValue scarLength;
    public final ModConfigSpec.IntValue scarWidth;
    public final ModConfigSpec.IntValue scarDepth;
    public final ModConfigSpec.DoubleValue traceChance;
    public final ModConfigSpec.IntValue traceSeconds;
    public final ModConfigSpec.IntValue traceMax;

    UnfinishedArrivalConfig(ModConfigSpec.Builder builder) {
        builder.comment("His arrival (step 2): standstill, climbing, the torn ground and the void scar; his roaming trace.")
                .push("arrival");
        omenSeconds = builder.comment("Seconds of omens before he tears the ground open.")
                .defineInRange("omenSeconds", 30, 3, 600);
        silenceSeconds = builder.comment("Seconds of standstill at the start of the omens before you hear him climb.")
                .defineInRange("silenceSeconds", 6, 0, 600);
        standstillRadius = builder.comment("Blocks round the place of his arrival within which players feel the standstill",
                        "(frozen rain and clouds, the sound drained away). Only those players; the ring weather stays.")
                .defineInRange("standstillRadius", 48.0, 8.0, 160.0);
        standstillLingerSeconds = builder.comment("Seconds the standstill stays after he is out, before it fades.")
                .defineInRange("standstillLingerSeconds", 15, 0, 300);
        arrivalDistance = builder.comment("Blocks behind his target where he tears the ground open.")
                .defineInRange("arrivalDistance", 24.0, 8.0, 64.0);
        tearWarnTicks = builder.comment("Ticks at the end of the omens in which the ground of the tear flickers (glitch blocks).")
                .defineInRange("tearWarnTicks", 60, 0, 400);
        tearLength = builder.comment("Length in blocks of the tear he climbs out of, across his way to the target.")
                .defineInRange("tearLength", 9, 3, 17);
        tearWidth = builder.comment("Width in blocks of the tear, along his way to the target.")
                .defineInRange("tearWidth", 5, 3, 9);
        tearOpenSeconds = builder.comment("Seconds the tear stays open down to the void before it loads back in (its",
                        "middle as the void scar).")
                .defineInRange("tearOpenSeconds", 20, 2, 300);
        climbTicks = builder.comment("Ticks he climbs out of the tear; the first half nothing reaches him.")
                .defineInRange("climbTicks", 100, 20, 400);
        climbDepth = builder.comment("Blocks below the ground where his climb starts.")
                .defineInRange("climbDepth", 18, 4, 64);
        scarEnabled = builder.comment("Whether a small shallow void scar stays where he came, once the tear loads back in: a",
                        "pit with a voidstone floor and a rim of voidstone and corrupted dirt, only in natural ground (no",
                        "block entities, fluids or built blocks). false = the ground comes back whole.")
                .define("scarEnabled", true);
        scarLength = builder.comment("Length in blocks of the void scar (across his way, like the tear).")
                .defineInRange("scarLength", 5, 1, 9);
        scarWidth = builder.comment("Width in blocks of the void scar.")
                .defineInRange("scarWidth", 3, 1, 5);
        scarDepth = builder.comment("Depth in blocks of the void scar, down to its voidstone floor.")
                .defineInRange("scarDepth", 4, 1, 8);
        traceChance = builder.comment("Chance per roaming step that the rain hangs still where he passed (only where loaded).")
                .defineInRange("traceChance", 0.25, 0.0, 1.0);
        traceSeconds = builder.comment("Seconds a trace of hanging rain lasts.")
                .defineInRange("traceSeconds", 120, 5, 1200);
        traceMax = builder.comment("Most traces at once; the oldest goes first.")
                .defineInRange("traceMax", 8, 0, 64);
        builder.pop();
    }
}
