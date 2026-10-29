package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code [nightmares.lost_keeper]}: The Lost Keeper, the ring 4 Nightmare (phase 4, fase-4-nightmares-concept.md "Ring 4 —
 * De Verloren Hoeder"). The shared keys in {@link #common} (its main attack is the bite), then its own: swimming, the
 * crown, its mist bank, its seven attacks, its arrival and its traces. Read live, so edits apply to a keeper that is
 * already fighting.
 */
public final class LostKeeperConfig {
    /** Also the attribute defaults of {@code TheLostKeeper}; movementSpeed is its crawl on land. */
    public static final NightmareConfig.Defaults DEFAULTS = new NightmareConfig.Defaults(800.0, 6.0, 0.12, 9.0, 0.5, 500);

    public final NightmareConfig common;
    public final ModConfigSpec.DoubleValue swimSpeed;
    public final ModConfigSpec.DoubleValue turnDegrees;
    public final ModConfigSpec.DoubleValue orbitRadius;
    public final ModConfigSpec.DoubleValue crownDamageMultiplier;
    public final ModConfigSpec.IntValue crownBreakCooldownTicks;
    public final ModConfigSpec.DoubleValue landingDamage;
    public final ModConfigSpec.IntValue stunSeconds;
    public final ModConfigSpec.DoubleValue mistRadius;
    public final ModConfigSpec.DoubleValue mistFade;
    public final ModConfigSpec.DoubleValue mistVisibility;
    public final ModConfigSpec.DoubleValue mistSyncRange;
    public final ModConfigSpec.IntValue lureCooldownMin;
    public final ModConfigSpec.IntValue lureCooldownMax;
    public final ModConfigSpec.IntValue lureSeconds;
    public final ModConfigSpec.DoubleValue lureRadius;
    public final ModConfigSpec.DoubleValue lurePull;
    public final ModConfigSpec.IntValue breathCooldownMin;
    public final ModConfigSpec.IntValue breathCooldownMax;
    public final ModConfigSpec.IntValue breathWindupTicks;
    public final ModConfigSpec.IntValue breathTicks;
    public final ModConfigSpec.DoubleValue breathRange;
    public final ModConfigSpec.DoubleValue breathAngle;
    public final ModConfigSpec.DoubleValue breathPoison;
    public final ModConfigSpec.IntValue cloudSeconds;
    public final ModConfigSpec.DoubleValue cloudRadius;
    public final ModConfigSpec.DoubleValue cloudPoison;
    public final ModConfigSpec.IntValue grabCooldownMin;
    public final ModConfigSpec.IntValue grabCooldownMax;
    public final ModConfigSpec.IntValue grabWindupTicks;
    public final ModConfigSpec.DoubleValue grabReach;
    public final ModConfigSpec.IntValue holdSeconds;
    public final ModConfigSpec.DoubleValue freeDamage;
    public final ModConfigSpec.DoubleValue squeezeDamage;
    public final ModConfigSpec.IntValue waterSearch;
    public final ModConfigSpec.IntValue whipCooldownMin;
    public final ModConfigSpec.IntValue whipCooldownMax;
    public final ModConfigSpec.IntValue whipWindupTicks;
    public final ModConfigSpec.IntValue whipTicks;
    public final ModConfigSpec.DoubleValue whipDamage;
    public final ModConfigSpec.DoubleValue whipKnockback;
    public final ModConfigSpec.DoubleValue whipReach;
    public final ModConfigSpec.IntValue bubbleCooldownMin;
    public final ModConfigSpec.IntValue bubbleCooldownMax;
    public final ModConfigSpec.DoubleValue bubbleRange;
    public final ModConfigSpec.IntValue bubbleSpots;
    public final ModConfigSpec.IntValue bubbleWarnTicks;
    public final ModConfigSpec.DoubleValue bubbleRadius;
    public final ModConfigSpec.IntValue bubblePullTicks;
    public final ModConfigSpec.DoubleValue bubblePull;
    public final ModConfigSpec.DoubleValue bubbleLaunch;
    public final ModConfigSpec.DoubleValue bubbleDamage;
    public final ModConfigSpec.IntValue encircleCooldownMin;
    public final ModConfigSpec.IntValue encircleCooldownMax;
    public final ModConfigSpec.IntValue encircleTicks;
    public final ModConfigSpec.DoubleValue encircleStartRadius;
    public final ModConfigSpec.DoubleValue encircleEndRadius;
    public final ModConfigSpec.DoubleValue encircleSpeed;
    public final ModConfigSpec.DoubleValue encircleWallDamage;
    public final ModConfigSpec.DoubleValue encircleWallPush;
    public final ModConfigSpec.DoubleValue encircleBreakDamage;
    public final ModConfigSpec.DoubleValue encircleSqueezeDamage;
    public final ModConfigSpec.IntValue burstCooldownMin;
    public final ModConfigSpec.IntValue burstCooldownMax;
    public final ModConfigSpec.IntValue burstMinDepth;
    public final ModConfigSpec.IntValue burstShoreReach;
    public final ModConfigSpec.IntValue burstLurkTicks;
    public final ModConfigSpec.IntValue burstWarnTicks;
    public final ModConfigSpec.DoubleValue burstSpeed;
    public final ModConfigSpec.DoubleValue burstRadius;
    public final ModConfigSpec.DoubleValue burstDamage;
    public final ModConfigSpec.DoubleValue burstLaunch;
    public final ModConfigSpec.IntValue arrivalOmenSeconds;
    public final ModConfigSpec.IntValue arrivalWaterSearch;
    public final ModConfigSpec.IntValue arrivalWaterDepth;
    public final ModConfigSpec.IntValue arrivalWaterSpread;
    public final ModConfigSpec.DoubleValue arrivalWaterShare;
    public final ModConfigSpec.DoubleValue arrivalMistDistance;
    public final ModConfigSpec.DoubleValue arrivalGlowDistance;
    public final ModConfigSpec.DoubleValue arrivalAirDistance;
    public final ModConfigSpec.DoubleValue arrivalAirHeight;
    public final ModConfigSpec.DoubleValue arrivalRiseSpeed;
    public final ModConfigSpec.DoubleValue arrivalAirSpeed;
    public final ModConfigSpec.IntValue arrivalMaxSeconds;
    public final ModConfigSpec.DoubleValue traceChance;
    public final ModConfigSpec.IntValue traceSeconds;
    public final ModConfigSpec.IntValue maxTraces;
    public final ModConfigSpec.IntValue traceWaterSearch;
    public final ModConfigSpec.DoubleValue traceRadius;
    public final ModConfigSpec.DoubleValue traceFade;
    public final ModConfigSpec.DoubleValue traceVisibility;

    LostKeeperConfig(ModConfigSpec.Builder builder) {
        builder.comment("The Lost Keeper, the ring 4 Nightmare (step 1: body and fight, summoned with /summon).",
                "attackDamage and attackKnockback are its bite; movementSpeed is its crawl on land.").push("lost_keeper");
        common = new NightmareConfig(builder, DEFAULTS);
        swimSpeed = builder.comment("Blocks per tick it swims, through water and (with its crown) through the air.")
                .defineInRange("swimSpeed", 0.42, 0.05, 2.0);
        turnDegrees = builder.comment("Degrees its head can turn per tick while swimming: lower is wider curves.")
                .defineInRange("turnDegrees", 7.0, 1.0, 45.0);
        orbitRadius = builder.comment("Blocks from its target at which it circles between attacks.")
                .defineInRange("orbitRadius", 14.0, 4.0, 48.0);

        builder.comment("The crown of pearl and coral: its weak spot.").push("crown");
        crownDamageMultiplier = builder.comment("Damage factor on hits on the crown (each hit also breaks off a branch).")
                .defineInRange("crownDamageMultiplier", 1.5, 0.0, 10.0);
        crownBreakCooldownTicks = builder.comment("Ticks after a broken branch before the next hit can break one.")
                .defineInRange("crownBreakCooldownTicks", 10, 0, 200);
        landingDamage = builder.comment("Damage of the landing after the crown is gone and it falls, as a part of its max",
                        "health (only when it lands on the ground, not in water).")
                .defineInRange("landingDamage", 0.08, 0.0, 1.0);
        stunSeconds = builder.comment("Seconds it lies stunned after that landing (in water: half).")
                .defineInRange("stunSeconds", 5, 0, 60);
        builder.pop();

        builder.comment("Its own mist bank (server state, synced to the players near it; the client draws the fog).").push("mist");
        mistRadius = builder.comment("Blocks round its head where the mist is at its thickest.")
                .defineInRange("mistRadius", 40.0, 0.0, 256.0);
        mistFade = builder.comment("Blocks beyond mistRadius over which the mist thins out to nothing.")
                .defineInRange("mistFade", 24.0, 1.0, 256.0);
        mistVisibility = builder.comment("Blocks of view in the thickest mist (the fog's far plane).")
                .defineInRange("mistVisibility", 32.0, 2.0, 256.0);
        mistSyncRange = builder.comment("Blocks within which players get its mist bank (beyond it they see clear air).")
                .defineInRange("mistSyncRange", 192.0, 16.0, 512.0);
        builder.pop();

        builder.comment("The siren's call: its glow and song pull the fighters slowly towards it; walking away resists.").push("lure");
        lureCooldownMin = builder.comment("Seconds between two calls, drawn between min and max.")
                .defineInRange("lureCooldownMin", 18, 1, 600);
        lureCooldownMax = builder.defineInRange("lureCooldownMax", 26, 1, 600);
        lureSeconds = builder.comment("Seconds a call lasts.")
                .defineInRange("lureSeconds", 6, 1, 60);
        lureRadius = builder.comment("Blocks round its head within which a fighter who can see it is pulled.")
                .defineInRange("lureRadius", 24.0, 2.0, 96.0);
        lurePull = builder.comment("Speed added per tick towards it (blocks/tick); standing still that drifts ~1 block a",
                        "second, walking away still gets you away, slowly.")
                .defineInRange("lurePull", 0.03, 0.0, 1.0);
        builder.pop();

        builder.comment("The poison breath of its rotten half: the ring 4 poison meter rises fast.").push("breath");
        breathCooldownMin = builder.comment("Seconds between two breaths, drawn between min and max.")
                .defineInRange("breathCooldownMin", 10, 1, 600);
        breathCooldownMax = builder.defineInRange("breathCooldownMax", 16, 1, 600);
        breathWindupTicks = builder.comment("Ticks it rears back before the breath: the time to get out of the way.")
                .defineInRange("breathWindupTicks", 15, 1, 100);
        breathTicks = builder.comment("Ticks the breath lasts.")
                .defineInRange("breathTicks", 45, 1, 400);
        breathRange = builder.comment("Blocks the breath reaches from its mouth.")
                .defineInRange("breathRange", 10.0, 1.0, 48.0);
        breathAngle = builder.comment("Half-angle of the breath's cone in degrees.")
                .defineInRange("breathAngle", 25.0, 1.0, 90.0);
        breathPoison = builder.comment("Poison points per tick in the cone (0.75: 15 a second).")
                .defineInRange("breathPoison", 0.75, 0.0, 100.0);
        cloudSeconds = builder.comment("Seconds the green cloud lingers where the breath ends.")
                .defineInRange("cloudSeconds", 5, 0, 60);
        cloudRadius = builder.comment("Blocks round the cloud's centre.")
                .defineInRange("cloudRadius", 2.5, 0.5, 16.0);
        cloudPoison = builder.comment("Poison points per tick inside the cloud (0.2: 4 a second).")
                .defineInRange("cloudPoison", 0.2, 0.0, 100.0);
        builder.pop();

        builder.comment("Pulling under water: it lunges, coils round a fighter and dives; damage frees them. Without water",
                "near the fighter the lunge is a bite (attackDamage).").push("grab");
        grabCooldownMin = builder.comment("Seconds between two lunges, drawn between min and max.")
                .defineInRange("grabCooldownMin", 12, 1, 600);
        grabCooldownMax = builder.defineInRange("grabCooldownMax", 18, 1, 600);
        grabWindupTicks = builder.comment("Ticks from the start of the lunge to the snap.")
                .defineInRange("grabWindupTicks", 10, 1, 100);
        grabReach = builder.comment("Blocks from its head's centre within which the snap catches (or bites).")
                .defineInRange("grabReach", 3.5, 0.5, 12.0);
        holdSeconds = builder.comment("Seconds at most it holds a fighter.")
                .defineInRange("holdSeconds", 6, 1, 60);
        freeDamage = builder.comment("Damage it must take (from anyone) while holding before it lets go.")
                .defineInRange("freeDamage", 30.0, 0.0, 10000.0);
        squeezeDamage = builder.comment("Damage to the held fighter every second (besides drowning).")
                .defineInRange("squeezeDamage", 2.0, 0.0, 100.0);
        waterSearch = builder.comment("Blocks below and round a fighter in which water counts as near (else: a bite).")
                .defineInRange("waterSearch", 6, 0, 32);
        builder.pop();

        builder.comment("The tail whip: its rear half sweeps sideways and knocks fighters off an island or shore.").push("whip");
        whipCooldownMin = builder.comment("Seconds between two whips, drawn between min and max.")
                .defineInRange("whipCooldownMin", 8, 1, 600);
        whipCooldownMax = builder.defineInRange("whipCooldownMax", 12, 1, 600);
        whipWindupTicks = builder.comment("Ticks of warning (the tail rattles) before the sweep.")
                .defineInRange("whipWindupTicks", 8, 1, 100);
        whipTicks = builder.comment("Ticks the sweep lasts.")
                .defineInRange("whipTicks", 10, 1, 100);
        whipDamage = builder.comment("Damage of a hit by the tail.")
                .defineInRange("whipDamage", 10.0, 0.0, 100.0);
        whipKnockback = builder.comment("Horizontal knockback of the tail (blocks/tick at the start: 1.4 throws a player ~15 blocks).")
                .defineInRange("whipKnockback", 1.4, 0.0, 10.0);
        whipReach = builder.comment("Blocks round the rear body parts that the sweep hits.")
                .defineInRange("whipReach", 2.5, 0.0, 10.0);
        builder.pop();

        builder.comment("The bubble trap: bubbles rise under a fighter as a warning (step aside), then a current pulls them",
                "down into the deep (in or on water) or a gust throws them up (on land, an island or in the air).").push("bubble_trap");
        bubbleCooldownMin = builder.comment("Seconds between two bubble traps, drawn between min and max.")
                .defineInRange("bubbleCooldownMin", 14, 1, 600);
        bubbleCooldownMax = builder.defineInRange("bubbleCooldownMax", 20, 1, 600);
        bubbleRange = builder.comment("Blocks round its head within which it calls up bubbles under fighters.")
                .defineInRange("bubbleRange", 28.0, 2.0, 96.0);
        bubbleSpots = builder.comment("Fighters at most that get bubbles under them at once (its target first).")
                .defineInRange("bubbleSpots", 2, 1, 16);
        bubbleWarnTicks = builder.comment("Ticks the bubbles rise before the current or the gust: the time to step aside.")
                .defineInRange("bubbleWarnTicks", 30, 1, 200);
        bubbleRadius = builder.comment("Blocks round the spot (horizontally) that the current or the gust catches.")
                .defineInRange("bubbleRadius", 1.8, 0.5, 8.0);
        bubblePullTicks = builder.comment("Ticks the current pulls down; swimming out of the column sideways escapes it.")
                .defineInRange("bubblePullTicks", 40, 1, 400);
        bubblePull = builder.comment("Speed added downwards per tick by the current (blocks/tick; 0.08 sinks ~0.4 a tick).")
                .defineInRange("bubblePull", 0.08, 0.0, 1.0);
        bubbleLaunch = builder.comment("Upward speed of the gust (blocks/tick; 1.5 throws a player ~11 blocks up).")
                .defineInRange("bubbleLaunch", 1.5, 0.0, 5.0);
        bubbleDamage = builder.comment("Damage when the current or the gust catches someone (the fall is the gust's real damage).")
                .defineInRange("bubbleDamage", 4.0, 0.0, 100.0);
        builder.pop();

        builder.comment("Encircling: it coils a shrinking ring round the fighters; its body is a wall (pushes back). Over the",
                "thin tail, under it in deep water, or enough damage breaks the ring; inside at the end: squeezed.").push("encircle");
        encircleCooldownMin = builder.comment("Seconds between two rings, drawn between min and max.")
                .defineInRange("encircleCooldownMin", 22, 1, 600);
        encircleCooldownMax = builder.defineInRange("encircleCooldownMax", 32, 1, 600);
        encircleTicks = builder.comment("Ticks the ring shrinks from the start to the end radius.")
                .defineInRange("encircleTicks", 120, 10, 1200);
        encircleStartRadius = builder.comment("Blocks of the ring at the start (open: the body covers less than half).")
                .defineInRange("encircleStartRadius", 8.0, 3.0, 32.0);
        encircleEndRadius = builder.comment("Blocks of the ring at the end (3.5: the ~21-block body just closes it).")
                .defineInRange("encircleEndRadius", 3.5, 2.0, 32.0);
        encircleSpeed = builder.comment("Its swim speed while coiling, times swimSpeed.")
                .defineInRange("encircleSpeed", 1.4, 0.1, 5.0);
        encircleWallDamage = builder.comment("Damage when a fighter runs into its body (at most twice a second).")
                .defineInRange("encircleWallDamage", 2.0, 0.0, 100.0);
        encircleWallPush = builder.comment("Speed (blocks/tick) with which its body pushes a fighter back.")
                .defineInRange("encircleWallPush", 0.55, 0.0, 5.0);
        encircleBreakDamage = builder.comment("Damage it must take (from anyone) during the ring before it breaks it off.")
                .defineInRange("encircleBreakDamage", 30.0, 0.0, 10000.0);
        encircleSqueezeDamage = builder.comment("Damage to everyone still inside when the ring closes (they are thrown up too).")
                .defineInRange("encircleSqueezeDamage", 6.0, 0.0, 100.0);
        builder.pop();

        builder.comment("Dive and burst: it dives away, the water churns under a fighter (or a boat), then it bursts up there",
                "and launches them. Only where there is water under or beside the fighter (open water or a shore).").push("burst");
        burstCooldownMin = builder.comment("Seconds between two bursts, drawn between min and max.")
                .defineInRange("burstCooldownMin", 16, 1, 600);
        burstCooldownMax = builder.defineInRange("burstCooldownMax", 24, 1, 600);
        burstMinDepth = builder.comment("Blocks of water it needs under the burst spot to dive in.")
                .defineInRange("burstMinDepth", 4, 2, 64);
        burstShoreReach = builder.comment("Blocks beside a fighter on a shore within which water counts for a burst.")
                .defineInRange("burstShoreReach", 4, 0, 16);
        burstLurkTicks = builder.comment("Ticks at most it swims deep under the fighter before the warning.")
                .defineInRange("burstLurkTicks", 40, 1, 400);
        burstWarnTicks = builder.comment("Ticks the surface churns before it bursts up there: the time to get away.")
                .defineInRange("burstWarnTicks", 25, 1, 200);
        burstSpeed = builder.comment("Its swim speed as it bursts up, times swimSpeed.")
                .defineInRange("burstSpeed", 2.6, 0.5, 8.0);
        burstRadius = builder.comment("Blocks round the spot (horizontally) that the burst hits.")
                .defineInRange("burstRadius", 2.5, 0.5, 10.0);
        burstDamage = builder.comment("Damage of the burst.")
                .defineInRange("burstDamage", 12.0, 0.0, 100.0);
        burstLaunch = builder.comment("Upward speed of the launch (blocks/tick; 1.4 throws a player ~10 blocks up). A boat",
                        "goes up too and its rider falls out.")
                .defineInRange("burstLaunch", 1.4, 0.0, 5.0);
        builder.pop();

        builder.comment("Its arrival for the director (out of the mist): its mist rolls in, its glow and its soft song come from",
                "far to near; out of big water near the target it rises, otherwise it swims to them through the air.").push("arrival");
        arrivalOmenSeconds = builder.comment("Seconds of omens (the mist rolling in, the glow coming closer) before it appears.")
                .defineInRange("arrivalOmenSeconds", 30, 1, 600);
        arrivalWaterSearch = builder.comment("Blocks round the target (horizontally) within which big water lets it rise from it.")
                .defineInRange("arrivalWaterSearch", 32, 0, 64);
        arrivalWaterDepth = builder.comment("Blocks of water it needs under the spot it rises from.")
                .defineInRange("arrivalWaterDepth", 4, 2, 64);
        arrivalWaterSpread = builder.comment("Blocks round that spot that are looked at to call the water big.")
                .defineInRange("arrivalWaterSpread", 3, 1, 8);
        arrivalWaterShare = builder.comment("Share of the columns round the spot that must be water at the surface for big water.")
                .defineInRange("arrivalWaterShare", 0.6, 0.0, 1.0);
        arrivalMistDistance = builder.comment("Blocks from the arrival's place at which its mist bank starts rolling in.")
                .defineInRange("arrivalMistDistance", 96.0, 0.0, 256.0);
        arrivalGlowDistance = builder.comment("Blocks from the target at which its glow is first seen through the mist (never",
                        "nearer than where it appears).")
                .defineInRange("arrivalGlowDistance", 28.0, 0.0, 128.0);
        arrivalAirDistance = builder.comment("Without big water: blocks from the target (ahead of them) where it appears in the air.")
                .defineInRange("arrivalAirDistance", 20.0, 4.0, 96.0);
        arrivalAirHeight = builder.comment("Blocks above the target's feet at which it appears in the air.")
                .defineInRange("arrivalAirHeight", 6.0, 0.0, 32.0);
        arrivalRiseSpeed = builder.comment("Its swim speed as it rises from the water, times swimSpeed.")
                .defineInRange("arrivalRiseSpeed", 2.0, 0.5, 8.0);
        arrivalAirSpeed = builder.comment("Its swim speed as it comes through the air to the target, times swimSpeed.")
                .defineInRange("arrivalAirSpeed", 1.2, 0.5, 8.0);
        arrivalMaxSeconds = builder.comment("Seconds at most the rise or the swim to the target lasts before it fights.")
                .defineInRange("arrivalMaxSeconds", 10, 1, 60);
        builder.pop();

        builder.comment("Its traces while it roams: a wisp of mist with a faint glow on the water near its virtual position.",
                "Nothing stays: the wisp lifts after traceSeconds.").push("trace");
        traceChance = builder.comment("Chance per roaming step that it leaves a wisp (0: none).")
                .defineInRange("traceChance", 0.35, 0.0, 1.0);
        traceSeconds = builder.comment("Seconds a wisp hangs before it has lifted.")
                .defineInRange("traceSeconds", 180, 10, 1200);
        maxTraces = builder.comment("Wisps at most at the same time (the oldest lifts at once for a new one).")
                .defineInRange("maxTraces", 4, 1, 8);
        traceWaterSearch = builder.comment("Blocks round its position within which the wisp looks for water (none: no wisp).")
                .defineInRange("traceWaterSearch", 12, 0, 32);
        traceRadius = builder.comment("Blocks round the wisp's centre where its mist is thickest.")
                .defineInRange("traceRadius", 6.0, 0.0, 64.0);
        traceFade = builder.comment("Blocks beyond traceRadius over which its mist thins out.")
                .defineInRange("traceFade", 10.0, 1.0, 64.0);
        traceVisibility = builder.comment("Blocks of view inside the wisp.")
                .defineInRange("traceVisibility", 24.0, 2.0, 256.0);
        builder.pop();
        builder.pop();
    }

    public int lureCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(lureCooldownMin, lureCooldownMax, roll);
    }

    public int breathCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(breathCooldownMin, breathCooldownMax, roll);
    }

    public int grabCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(grabCooldownMin, grabCooldownMax, roll);
    }

    public int whipCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(whipCooldownMin, whipCooldownMax, roll);
    }

    public int bubbleCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(bubbleCooldownMin, bubbleCooldownMax, roll);
    }

    public int encircleCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(encircleCooldownMin, encircleCooldownMax, roll);
    }

    public int burstCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(burstCooldownMin, burstCooldownMax, roll);
    }
}
