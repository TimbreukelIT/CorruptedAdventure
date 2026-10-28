package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code [nightmares.hollow]}: The Hollow, the ring 1 Nightmare (phase 4, fase-4-nightmares-concept.md "Fase 4-sparring
 * De Holle"). The shared keys in {@link #common} (its main attack is the slam of the giant arm), then its own. Read live,
 * so edits apply to a Hollow that is already fighting.
 */
public final class HollowConfig {
    /** Also the attribute defaults of {@code TheHollow}. */
    public static final NightmareConfig.Defaults DEFAULTS = new NightmareConfig.Defaults(300.0, 4.0, 0.26, 10.0, 1.5, 250);

    public final NightmareConfig common;
    public final ModConfigSpec.DoubleValue slamReach;
    public final ModConfigSpec.IntValue slamWindupTicks;
    public final ModConfigSpec.IntValue slamCooldownTicks;
    public final ModConfigSpec.IntValue riftStepCooldownMin;
    public final ModConfigSpec.IntValue riftStepCooldownMax;
    public final ModConfigSpec.IntValue riftStepWarningTicks;
    public final ModConfigSpec.DoubleValue riftStepDistance;
    public final ModConfigSpec.IntValue phaseIntervalMin;
    public final ModConfigSpec.IntValue phaseIntervalMax;
    public final ModConfigSpec.IntValue phaseDurationMin;
    public final ModConfigSpec.IntValue phaseDurationMax;
    public final ModConfigSpec.IntValue swapCooldown;
    public final ModConfigSpec.IntValue swapWarningTicks;
    public final ModConfigSpec.IntValue pinSeconds;

    HollowConfig(ModConfigSpec.Builder builder) {
        builder.comment("The Hollow, the ring 1 Nightmare (step 1: body and fight, summoned with /summon).",
                "attackDamage and attackKnockback are its giant-arm slam.").push("hollow");
        common = new NightmareConfig(builder, DEFAULTS);
        slamReach = builder.comment("Blocks the slam reaches beyond The Hollow's hitbox, horizontally.")
                .defineInRange("slamReach", 2.0, 0.5, 6.0);
        slamWindupTicks = builder.comment("Ticks between raising the arm and the hit: the time to step away.")
                .defineInRange("slamWindupTicks", 12, 1, 60);
        slamCooldownTicks = builder.comment("Ticks after a slam before the next can start.")
                .defineInRange("slamCooldownTicks", 30, 0, 200);
        riftStepCooldownMin = builder.comment("Seconds between two rift steps, drawn between min and max.")
                .defineInRange("riftStepCooldownMin", 8, 1, 600);
        riftStepCooldownMax = builder.defineInRange("riftStepCooldownMax", 14, 1, 600);
        riftStepWarningTicks = builder.comment("Ticks between the cracking warning and the step itself.")
                .defineInRange("riftStepWarningTicks", 20, 1, 100);
        riftStepDistance = builder.comment("Blocks behind its target where a rift step comes out.")
                .defineInRange("riftStepDistance", 3.0, 1.0, 8.0);
        phaseIntervalMin = builder.comment("Seconds between two out-of-phase spells, drawn between min and max.")
                .defineInRange("phaseIntervalMin", 12, 1, 600);
        phaseIntervalMax = builder.defineInRange("phaseIntervalMax", 20, 1, 600);
        phaseDurationMin = builder.comment("Seconds an out-of-phase spell lasts (transparent and invulnerable), drawn between",
                        "min and max.")
                .defineInRange("phaseDurationMin", 3, 1, 60);
        phaseDurationMax = builder.defineInRange("phaseDurationMax", 5, 1, 60);
        swapCooldown = builder.comment("Seconds between two swaps of two players (only with at least two in the fight).")
                .defineInRange("swapCooldown", 25, 1, 600);
        swapWarningTicks = builder.comment("Ticks between the start of the swap and the players changing places.")
                .defineInRange("swapWarningTicks", 15, 1, 100);
        pinSeconds = builder.comment("Seconds a hit with a soul crystal in the main hand pins The Hollow in phase (no rift",
                        "step, never transparent). Costs one crystal, not in creative.")
                .defineInRange("pinSeconds", 4, 1, 60);
        builder.pop();
    }

    public int riftStepCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(riftStepCooldownMin, riftStepCooldownMax, roll);
    }

    public int phaseIntervalTicks(int roll) {
        return NightmareConfig.ticksBetween(phaseIntervalMin, phaseIntervalMax, roll);
    }

    public int phaseDurationTicks(int roll) {
        return NightmareConfig.ticksBetween(phaseDurationMin, phaseDurationMax, roll);
    }
}
