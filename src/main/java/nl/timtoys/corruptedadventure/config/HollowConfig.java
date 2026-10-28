package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code [nightmares.hollow]}: The Hollow, the ring 1 Nightmare (phase 4, fase-4-nightmares-concept.md "Fase 4-sparring
 * De Holle"). Read live, so edits apply to a Hollow that is already fighting from its next check.
 */
public final class HollowConfig {
    public final ModConfigSpec.DoubleValue baseHealth;
    public final ModConfigSpec.DoubleValue healthPerExtraPlayer;
    public final ModConfigSpec.DoubleValue fightRadius;
    public final ModConfigSpec.DoubleValue armor;
    public final ModConfigSpec.DoubleValue movementSpeed;
    public final ModConfigSpec.DoubleValue slamDamage;
    public final ModConfigSpec.DoubleValue slamKnockback;
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
    public final ModConfigSpec.DoubleValue retreatRadius;
    public final ModConfigSpec.IntValue retreatSeconds;
    public final ModConfigSpec.IntValue experience;

    HollowConfig(ModConfigSpec.Builder builder) {
        builder.comment("The Hollow, the ring 1 Nightmare (step 1: body and fight, summoned with /summon).").push("hollow");
        baseHealth = builder.comment("Max health with one player in the fight (300 = the Wither).")
                .defineInRange("baseHealth", 300.0, 1.0, 10000.0);
        healthPerExtraPlayer = builder.comment("Extra max health per further player in the fight, as a part of baseHealth",
                        "(0.5: two players 1.5x, three 2x). Rescaled when a player joins or leaves; the health ratio is kept.")
                .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
        fightRadius = builder.comment("Blocks round The Hollow within which a living, non-spectator player is in the fight",
                        "(health scaling and the swap).")
                .defineInRange("fightRadius", 32.0, 4.0, 128.0);
        armor = builder.comment("Armor points (the Wither has 4).")
                .defineInRange("armor", 4.0, 0.0, 30.0);
        movementSpeed = builder.comment("Movement speed attribute (a zombie has 0.23).")
                .defineInRange("movementSpeed", 0.26, 0.0, 1.0);
        slamDamage = builder.comment("Damage (half hearts) of a slam of the giant arm, before armor.")
                .defineInRange("slamDamage", 10.0, 0.0, 100.0);
        slamKnockback = builder.comment("Knockback of a slam (attack_knockback attribute; a ravager has 1.5).")
                .defineInRange("slamKnockback", 1.5, 0.0, 5.0);
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
        retreatRadius = builder.comment("Blocks within which a living, non-spectator player keeps The Hollow here.")
                .defineInRange("retreatRadius", 64.0, 8.0, 256.0);
        retreatSeconds = builder.comment("Seconds without such a player before The Hollow retreats (disappears, no drop).")
                .defineInRange("retreatSeconds", 30, 1, 3600);
        experience = builder.comment("Experience dropped on its defeat (the Wither drops 50).")
                .defineInRange("experience", 250, 0, 100000);
        builder.pop();
    }

    /** {@code min} when {@code max} is below it, so a mistyped pair never throws. */
    static int between(ModConfigSpec.IntValue min, ModConfigSpec.IntValue max, int roll) {
        int lo = min.get();
        int hi = Math.max(lo, max.get());
        return lo + Math.floorMod(roll, hi - lo + 1);
    }

    public int riftStepCooldownTicks(int roll) {
        return between(riftStepCooldownMin, riftStepCooldownMax, roll) * 20;
    }

    public int phaseIntervalTicks(int roll) {
        return between(phaseIntervalMin, phaseIntervalMax, roll) * 20;
    }

    public int phaseDurationTicks(int roll) {
        return between(phaseDurationMin, phaseDurationMax, roll) * 20;
    }
}
