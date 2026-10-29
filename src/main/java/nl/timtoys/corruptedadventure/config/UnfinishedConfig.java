package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code [nightmares.unfinished]}: The Unfinished, the ring 5 Nightmare (phase 4, fase-4-nightmares-concept.md "Ring 5 —
 * De Onvoltooide"). The shared keys in {@link #common} (its main attack is the slam of the grafted Hollow arm), then the
 * weak spots and the sky glass lens, its stride and its memories. Read live, so edits apply to one that is already
 * fighting.
 */
public final class UnfinishedConfig {
    /** Also the attribute defaults of {@code TheUnfinished}. */
    public static final NightmareConfig.Defaults DEFAULTS = new NightmareConfig.Defaults(1200.0, 10.0, 0.2, 14.0, 2.0, 1000);

    public final NightmareConfig common;
    public final ModConfigSpec.DoubleValue bodyDamageFactor;
    public final ModConfigSpec.DoubleValue weakDamageFactor;
    public final ModConfigSpec.DoubleValue graftBreakShare;
    public final ModConfigSpec.DoubleValue lensRange;
    public final ModConfigSpec.DoubleValue lensConeDegrees;
    public final ModConfigSpec.IntValue lensLingerTicks;
    public final ModConfigSpec.DoubleValue strideStopDistance;
    public final ModConfigSpec.DoubleValue climbPerTick;
    public final ModConfigSpec.IntValue attackGapTicks;
    public final ModConfigSpec.DoubleValue slamRadius;
    public final ModConfigSpec.IntValue slamWindupTicks;
    public final ModConfigSpec.IntValue slamCooldownMin;
    public final ModConfigSpec.IntValue slamCooldownMax;
    public final ModConfigSpec.DoubleValue riftStepFarDistance;
    public final ModConfigSpec.DoubleValue riftStepDistance;
    public final ModConfigSpec.IntValue riftStepSinkTicks;
    public final ModConfigSpec.IntValue riftStepCooldownMin;
    public final ModConfigSpec.IntValue riftStepCooldownMax;
    public final ModConfigSpec.DoubleValue grabRange;
    public final ModConfigSpec.IntValue grabReachTicks;
    public final ModConfigSpec.IntValue grabHoldTicks;
    public final ModConfigSpec.DoubleValue grabLift;
    public final ModConfigSpec.DoubleValue grabBreakDamage;
    public final ModConfigSpec.IntValue grabCooldownMin;
    public final ModConfigSpec.IntValue grabCooldownMax;
    public final ModConfigSpec.DoubleValue ashRange;
    public final ModConfigSpec.DoubleValue ashRadius;
    public final ModConfigSpec.IntValue ashSeconds;
    public final ModConfigSpec.IntValue ashMaxStacks;
    public final ModConfigSpec.IntValue ashCooldownMin;
    public final ModConfigSpec.IntValue ashCooldownMax;
    public final ModConfigSpec.DoubleValue lureRadius;
    public final ModConfigSpec.IntValue lureTicks;
    public final ModConfigSpec.DoubleValue lureStrength;
    public final ModConfigSpec.IntValue lureCooldownMin;
    public final ModConfigSpec.IntValue lureCooldownMax;
    public final ModConfigSpec.DoubleValue voidPullRadius;
    public final ModConfigSpec.IntValue voidPullTicks;
    public final ModConfigSpec.DoubleValue voidPullStrength;
    public final ModConfigSpec.DoubleValue voidPullMeterPerSecond;
    public final ModConfigSpec.IntValue voidEdgeSearch;
    public final ModConfigSpec.IntValue voidPullCooldownMin;
    public final ModConfigSpec.IntValue voidPullCooldownMax;
    public final ModConfigSpec.DoubleValue glitchMinRange;
    public final ModConfigSpec.DoubleValue glitchMaxRange;
    public final ModConfigSpec.IntValue glitchFreezeTicks;
    public final ModConfigSpec.IntValue glitchStepsMin;
    public final ModConfigSpec.IntValue glitchStepsMax;
    public final ModConfigSpec.DoubleValue glitchStepMin;
    public final ModConfigSpec.DoubleValue glitchStepMax;
    public final ModConfigSpec.IntValue glitchPauseMin;
    public final ModConfigSpec.IntValue glitchPauseMax;
    public final ModConfigSpec.IntValue glitchTelegraphTicks;
    public final ModConfigSpec.DoubleValue glitchStrikeDistance;
    public final ModConfigSpec.DoubleValue glitchStrikeRadius;
    public final ModConfigSpec.DoubleValue glitchStrikeDamage;
    public final ModConfigSpec.IntValue glitchCooldownMin;
    public final ModConfigSpec.IntValue glitchCooldownMax;
    public final ModConfigSpec.DoubleValue throwMinRange;
    public final ModConfigSpec.DoubleValue throwRange;
    public final ModConfigSpec.IntValue throwPieces;
    public final ModConfigSpec.IntValue throwRiseTicks;
    public final ModConfigSpec.DoubleValue throwImpactRadius;
    public final ModConfigSpec.DoubleValue throwDamage;
    public final ModConfigSpec.IntValue throwLingerSeconds;
    public final ModConfigSpec.IntValue throwMaxLyingBlocks;
    public final ModConfigSpec.IntValue throwCooldownMin;
    public final ModConfigSpec.IntValue throwCooldownMax;
    public final ModConfigSpec.BooleanValue unloadEnabled;
    public final ModConfigSpec.DoubleValue unloadRange;
    public final ModConfigSpec.IntValue unloadRadius;
    public final ModConfigSpec.IntValue unloadWarnTicks;
    public final ModConfigSpec.IntValue unloadOpenTicks;
    public final ModConfigSpec.IntValue unloadLayersPerTick;
    public final ModConfigSpec.IntValue unloadMaxBlocks;
    public final ModConfigSpec.IntValue unloadCatchDepth;
    public final ModConfigSpec.DoubleValue unloadCatchMeter;
    public final ModConfigSpec.DoubleValue unloadCatchDamage;
    public final ModConfigSpec.IntValue unloadCooldownMin;
    public final ModConfigSpec.IntValue unloadCooldownMax;
    public final ModConfigSpec.IntValue tempRestorePerTick;

    UnfinishedConfig(ModConfigSpec.Builder builder) {
        builder.comment("The Unfinished, the ring 5 Nightmare (step 1: body and fight, summoned with /summon).",
                "attackDamage and attackKnockback are its slam with the grafted arm of The Hollow; movementSpeed x 0.4 is",
                "its stride in blocks per tick.").push("unfinished");
        common = new NightmareConfig(builder, DEFAULTS);

        bodyDamageFactor = builder.comment("Share of the damage his mosaic body (the main hitbox and the body parts) takes.")
                .defineInRange("bodyDamageFactor", 0.25, 0.0, 1.0);
        weakDamageFactor = builder.comment("Damage factor on a weak spot while it is loaded (sky glass lens); unloaded a weak",
                        "spot takes nothing.")
                .defineInRange("weakDamageFactor", 2.0, 0.0, 10.0);
        graftBreakShare = builder.comment("Damage on a graft's weak spot, as a share of his max health, after which that graft",
                        "falls away (and with it its memory attack). 0 = never.")
                .defineInRange("graftBreakShare", 0.15, 0.0, 1.0);
        lensRange = builder.comment("Blocks within which a player looking at him through a sky glass lens loads his weak spots.")
                .defineInRange("lensRange", 48.0, 4.0, 128.0);
        lensConeDegrees = builder.comment("Degrees between the lens's line of sight and a weak spot (or his body) that still",
                        "count as looking at him.")
                .defineInRange("lensConeDegrees", 12.0, 1.0, 45.0);
        lensLingerTicks = builder.comment("Ticks the weak spots stay loaded after the last look through a lens (time to switch",
                        "to a weapon).")
                .defineInRange("lensLingerTicks", 120, 0, 1200);

        strideStopDistance = builder.comment("Blocks to his target at which he stops striding toward it.")
                .defineInRange("strideStopDistance", 9.0, 2.0, 64.0);
        climbPerTick = builder.comment("Blocks per tick he climbs up (or drops down) to the ground under him.")
                .defineInRange("climbPerTick", 0.3, 0.05, 4.0);
        attackGapTicks = builder.comment("Ticks after one attack before any next one can start.")
                .defineInRange("attackGapTicks", 30, 0, 600);

        slamRadius = builder.comment("Blocks round the spot where the fist lands that the slam hits.")
                .defineInRange("slamRadius", 4.5, 1.0, 12.0);
        slamWindupTicks = builder.comment("Ticks between raising the arm and the hit: the time to get away.")
                .defineInRange("slamWindupTicks", 25, 1, 100);
        slamCooldownMin = builder.comment("Seconds between two slams, drawn between min and max.")
                .defineInRange("slamCooldownMin", 3, 1, 600);
        slamCooldownMax = builder.defineInRange("slamCooldownMax", 5, 1, 600);

        riftStepFarDistance = builder.comment("Memory of The Hollow: blocks to his target beyond which he steps through the void",
                        "at once, whatever the cooldown.")
                .defineInRange("riftStepFarDistance", 24.0, 8.0, 128.0);
        riftStepDistance = builder.comment("Blocks from his target where a rift step comes out.")
                .defineInRange("riftStepDistance", 9.0, 3.0, 32.0);
        riftStepSinkTicks = builder.comment("Ticks he sinks into his void before the step (and rises after it).")
                .defineInRange("riftStepSinkTicks", 30, 5, 100);
        riftStepCooldownMin = builder.comment("Seconds between two rift steps, drawn between min and max.")
                .defineInRange("riftStepCooldownMin", 18, 1, 600);
        riftStepCooldownMax = builder.defineInRange("riftStepCooldownMax", 28, 1, 600);

        grabRange = builder.comment("Memory of The Layered: blocks within which he can grab a player.")
                .defineInRange("grabRange", 14.0, 2.0, 32.0);
        grabReachTicks = builder.comment("Ticks between the leg swinging out and the claw closing.")
                .defineInRange("grabReachTicks", 16, 1, 100);
        grabHoldTicks = builder.comment("Ticks he holds a grabbed player, lifting him, before letting go.")
                .defineInRange("grabHoldTicks", 60, 1, 400);
        grabLift = builder.comment("Blocks above his feet he lifts a grabbed player to (the fall is the damage).")
                .defineInRange("grabLift", 16.0, 2.0, 40.0);
        grabBreakDamage = builder.comment("Health he must lose during a hold before the claw lets go early.")
                .defineInRange("grabBreakDamage", 40.0, 1.0, 1000.0);
        grabCooldownMin = builder.comment("Seconds between two grabs, drawn between min and max.")
                .defineInRange("grabCooldownMin", 20, 1, 600);
        grabCooldownMax = builder.defineInRange("grabCooldownMax", 30, 1, 600);

        ashRange = builder.comment("Memory of The Risen: blocks to his target within which he lets ash rain on it.")
                .defineInRange("ashRange", 28.0, 4.0, 64.0);
        ashRadius = builder.comment("Radius in blocks of the ash cloud.")
                .defineInRange("ashRadius", 6.0, 1.0, 24.0);
        ashSeconds = builder.comment("Seconds an ash cloud keeps raining.")
                .defineInRange("ashSeconds", 8, 1, 120);
        ashMaxStacks = builder.comment("Layers of ash (one per second in the cloud, one off per second outside) until a player",
                        "stands fast; each layer is a level of slowness.")
                .defineInRange("ashMaxStacks", 6, 1, 10);
        ashCooldownMin = builder.comment("Seconds between two ash rains, drawn between min and max.")
                .defineInRange("ashCooldownMin", 20, 1, 600);
        ashCooldownMax = builder.defineInRange("ashCooldownMax", 30, 1, 600);

        lureRadius = builder.comment("Memory of The Lost Keeper: blocks within which his song pulls players toward him.")
                .defineInRange("lureRadius", 28.0, 4.0, 64.0);
        lureTicks = builder.comment("Ticks the song lasts.")
                .defineInRange("lureTicks", 80, 1, 400);
        lureStrength = builder.comment("Pull of the song in blocks per tick per tick (sneaking players feel 40 %).")
                .defineInRange("lureStrength", 0.07, 0.0, 1.0);
        lureCooldownMin = builder.comment("Seconds between two songs, drawn between min and max.")
                .defineInRange("lureCooldownMin", 25, 1, 600);
        lureCooldownMax = builder.defineInRange("lureCooldownMax", 35, 1, 600);

        voidPullRadius = builder.comment("Blocks within which his void pull takes players.")
                .defineInRange("voidPullRadius", 24.0, 4.0, 64.0);
        voidPullTicks = builder.comment("Ticks the void pull lasts.")
                .defineInRange("voidPullTicks", 80, 1, 400);
        voidPullStrength = builder.comment("Pull toward the nearest void edge in blocks per tick per tick (sneaking: 40 %).")
                .defineInRange("voidPullStrength", 0.08, 0.0, 1.0);
        voidPullMeterPerSecond = builder.comment("Points per second the void meter of a pulled player rises.")
                .defineInRange("voidPullMeterPerSecond", 8.0, 0.0, 100.0);
        voidEdgeSearch = builder.comment("Blocks round a player searched for a void column to pull toward; none found: toward",
                        "him.")
                .defineInRange("voidEdgeSearch", 24, 2, 48);
        voidPullCooldownMin = builder.comment("Seconds between two void pulls, drawn between min and max.")
                .defineInRange("voidPullCooldownMin", 30, 1, 600);
        voidPullCooldownMax = builder.defineInRange("voidPullCooldownMax", 40, 1, 600);

        glitchMinRange = builder.comment("Glitch step (his own): blocks to his target from which he stutters toward it.")
                .defineInRange("glitchMinRange", 10.0, 2.0, 64.0);
        glitchMaxRange = builder.comment("Blocks to his target up to which he uses the glitch step.")
                .defineInRange("glitchMaxRange", 30.0, 4.0, 128.0);
        glitchFreezeTicks = builder.comment("Ticks he freezes and jerks before the first step (the warning).")
                .defineInRange("glitchFreezeTicks", 20, 1, 100);
        glitchStepsMin = builder.comment("Steps of one glitch step, drawn between min and max (the last one ends the run).")
                .defineInRange("glitchStepsMin", 4, 1, 12);
        glitchStepsMax = builder.defineInRange("glitchStepsMax", 6, 1, 12);
        glitchStepMin = builder.comment("Blocks of one step, drawn between min and max.")
                .defineInRange("glitchStepMin", 3.0, 0.5, 16.0);
        glitchStepMax = builder.defineInRange("glitchStepMax", 5.0, 0.5, 16.0);
        glitchPauseMin = builder.comment("Ticks between two steps, drawn between min and max: the stutter.")
                .defineInRange("glitchPauseMin", 4, 1, 60);
        glitchPauseMax = builder.defineInRange("glitchPauseMax", 10, 1, 60);
        glitchTelegraphTicks = builder.comment("Ticks a pillar of glitch pixels marks the next spot before he is there.")
                .defineInRange("glitchTelegraphTicks", 6, 0, 40);
        glitchStrikeDistance = builder.comment("Blocks from his target where the last step lands.")
                .defineInRange("glitchStrikeDistance", 5.0, 1.0, 16.0);
        glitchStrikeRadius = builder.comment("Blocks round where the last step lands that its frame blow hits.")
                .defineInRange("glitchStrikeRadius", 4.0, 0.0, 12.0);
        glitchStrikeDamage = builder.comment("Damage of the frame blow (grows with his strength). 0 = none.")
                .defineInRange("glitchStrikeDamage", 10.0, 0.0, 100.0);
        glitchCooldownMin = builder.comment("Seconds between two glitch steps, drawn between min and max.")
                .defineInRange("glitchCooldownMin", 14, 1, 600);
        glitchCooldownMax = builder.defineInRange("glitchCooldownMax", 22, 1, 600);

        throwMinRange = builder.comment("Throwing memories (his own): blocks to his target from which he throws.")
                .defineInRange("throwMinRange", 8.0, 2.0, 64.0);
        throwRange = builder.comment("Blocks to his target up to which he throws.")
                .defineInRange("throwRange", 32.0, 4.0, 96.0);
        throwPieces = builder.comment("Blocks (shards) in one thrown memory.")
                .defineInRange("throwPieces", 12, 1, 27);
        throwRiseTicks = builder.comment("Ticks the memory rises above his crown before the throw (the warning).")
                .defineInRange("throwRiseTicks", 30, 1, 100);
        throwImpactRadius = builder.comment("Blocks round where it comes down that the impact hits.")
                .defineInRange("throwImpactRadius", 3.0, 0.5, 12.0);
        throwDamage = builder.comment("Damage of the impact (grows with his strength).")
                .defineInRange("throwDamage", 12.0, 0.0, 100.0);
        throwLingerSeconds = builder.comment("Seconds a shard lies as a block before it flickers away (the spot is restored).")
                .defineInRange("throwLingerSeconds", 10, 1, 120);
        throwMaxLyingBlocks = builder.comment("Most shards lying as blocks at once in a level; the rest bursts into pixels. 0 =",
                        "shards never lie (no world change).")
                .defineInRange("throwMaxLyingBlocks", 48, 0, 512);
        throwCooldownMin = builder.comment("Seconds between two throws, drawn between min and max.")
                .defineInRange("throwCooldownMin", 16, 1, 600);
        throwCooldownMax = builder.defineInRange("throwCooldownMax", 24, 1, 600);

        unloadEnabled = builder.comment("Unloading ground (his own): a patch round his target flickers away down to the void",
                        "and comes back exactly. Only whole natural columns (no block entities, fluids or built blocks).",
                        "false = he never uses it.")
                .define("unloadEnabled", true);
        unloadRange = builder.comment("Blocks to his target within which he unloads the ground under it.")
                .defineInRange("unloadRange", 24.0, 4.0, 64.0);
        unloadRadius = builder.comment("Radius in blocks of the unloaded patch.")
                .defineInRange("unloadRadius", 3, 1, 8);
        unloadWarnTicks = builder.comment("Ticks the patch flickers (glitch blocks) before it opens: the time to step off.")
                .defineInRange("unloadWarnTicks", 40, 5, 200);
        unloadOpenTicks = builder.comment("Ticks the patch stays open before it loads back in, bottom first.")
                .defineInRange("unloadOpenTicks", 80, 10, 600);
        unloadLayersPerTick = builder.comment("Layers per tick the patch unloads downward (and spreads the work).")
                .defineInRange("unloadLayersPerTick", 16, 1, 512);
        unloadMaxBlocks = builder.comment("Most blocks one unloading may take away (columns beyond it stay).")
                .defineInRange("unloadMaxBlocks", 6000, 1, 50000);
        unloadCatchDepth = builder.comment("Blocks below the patch's surface at which the void catches a player who fell in.")
                .defineInRange("unloadCatchDepth", 12, 1, 64);
        unloadCatchMeter = builder.comment("Void meter points a caught player gets.")
                .defineInRange("unloadCatchMeter", 30.0, 0.0, 100.0);
        unloadCatchDamage = builder.comment("Void damage a caught player takes.")
                .defineInRange("unloadCatchDamage", 4.0, 0.0, 100.0);
        unloadCooldownMin = builder.comment("Seconds between two unloadings, drawn between min and max.")
                .defineInRange("unloadCooldownMin", 24, 1, 600);
        unloadCooldownMax = builder.defineInRange("unloadCooldownMax", 34, 1, 600);
        tempRestorePerTick = builder.comment("Most blocks per tick per level that come back (unloaded ground, lying shards).")
                .defineInRange("tempRestorePerTick", 1024, 16, 65536);
        builder.pop();
    }

    /** A cooldown in ticks, drawn between two second values. */
    public static int cooldown(ModConfigSpec.IntValue min, ModConfigSpec.IntValue max, int roll) {
        return NightmareConfig.ticksBetween(min, max, roll);
    }
}
