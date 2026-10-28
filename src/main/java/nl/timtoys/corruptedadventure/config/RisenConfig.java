package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code [nightmares.risen]}: The Risen, the ring 3 Nightmare (phase 4, fase-4-nightmares-concept.md "Ring 3 — De
 * Herrijzende"). The shared keys in {@link #common} (its main attack is the dive with its talons), then its own: flight,
 * landing, ash rain, the choir's scream, released victims, the horn's stun and the melt pool. Read live.
 */
public final class RisenConfig {
    /** Also the attribute defaults of {@code TheRisen}. Ring 3: 600 HP (design doc). */
    public static final NightmareConfig.Defaults DEFAULTS = new NightmareConfig.Defaults(600.0, 6.0, 0.3, 12.0, 1.5, 400);

    public final NightmareConfig common;
    public final ModConfigSpec.DoubleValue flightHeight;
    public final ModConfigSpec.DoubleValue circleRadius;
    public final ModConfigSpec.DoubleValue flySpeed;
    public final ModConfigSpec.DoubleValue diveSpeed;
    public final ModConfigSpec.IntValue diveCooldownMin;
    public final ModConfigSpec.IntValue diveCooldownMax;
    public final ModConfigSpec.IntValue landInterval;
    public final ModConfigSpec.IntValue landedSeconds;
    public final ModConfigSpec.DoubleValue peckDamage;
    public final ModConfigSpec.DoubleValue peckReach;
    public final ModConfigSpec.DoubleValue buffetDamage;
    public final ModConfigSpec.DoubleValue buffetRadius;
    public final ModConfigSpec.DoubleValue buffetKnockback;
    public final ModConfigSpec.IntValue ashRainIntervalMin;
    public final ModConfigSpec.IntValue ashRainIntervalMax;
    public final ModConfigSpec.IntValue ashRainSeconds;
    public final ModConfigSpec.DoubleValue ashRainRadius;
    public final ModConfigSpec.IntValue ashTicksPerLayer;
    public final ModConfigSpec.IntValue ashDecaySeconds;
    public final ModConfigSpec.IntValue ashEncasedSeconds;
    public final ModConfigSpec.IntValue ashKnockOff;
    public final ModConfigSpec.IntValue screamCooldownMin;
    public final ModConfigSpec.IntValue screamCooldownMax;
    public final ModConfigSpec.IntValue screamWindupTicks;
    public final ModConfigSpec.DoubleValue screamRadius;
    public final ModConfigSpec.IntValue screamWeaknessSeconds;
    public final ModConfigSpec.IntValue screamNauseaSeconds;
    public final ModConfigSpec.DoubleValue fleeRadius;
    public final ModConfigSpec.IntValue releaseCooldownMin;
    public final ModConfigSpec.IntValue releaseCooldownMax;
    public final ModConfigSpec.IntValue releaseCount;
    public final ModConfigSpec.IntValue maxWalkers;
    public final ModConfigSpec.IntValue stunGraceSeconds;
    public final ModConfigSpec.IntValue stunImmunitySeconds;
    public final ModConfigSpec.DoubleValue wingDamageFactor;
    public final ModConfigSpec.BooleanValue meltPool;
    public final ModConfigSpec.IntValue meltPoolRadius;
    public final ModConfigSpec.IntValue meltPoolSeconds;

    RisenConfig(ModConfigSpec.Builder builder) {
        builder.comment("The Risen, the ring 3 Nightmare (step 1: body and fight, summoned with /summon).",
                "attackDamage and attackKnockback are its dive (talons). movementSpeed is unused: it flies.").push("risen");
        common = new NightmareConfig(builder, DEFAULTS);
        flightHeight = builder.comment("Blocks above the ground (at its target) at which it circles.")
                .defineInRange("flightHeight", 14.0, 4.0, 48.0);
        circleRadius = builder.comment("Radius in blocks of the circle it flies around its target.")
                .defineInRange("circleRadius", 20.0, 6.0, 64.0);
        flySpeed = builder.comment("Flight speed in blocks per tick while circling.")
                .defineInRange("flySpeed", 0.8, 0.1, 3.0);
        diveSpeed = builder.comment("Speed in blocks per tick of a dive at its target.")
                .defineInRange("diveSpeed", 1.4, 0.2, 4.0);
        diveCooldownMin = builder.comment("Seconds between two dives, drawn between min and max.")
                .defineInRange("diveCooldownMin", 8, 1, 600);
        diveCooldownMax = builder.defineInRange("diveCooldownMax", 14, 1, 600);
        landInterval = builder.comment("Seconds of flight before it lands beside its target (beak and wing buffet).")
                .defineInRange("landInterval", 25, 1, 600);
        landedSeconds = builder.comment("Seconds it stays on the ground before it takes off again.")
                .defineInRange("landedSeconds", 8, 1, 120);
        peckDamage = builder.comment("Damage (half hearts) of the beak on the ground.")
                .defineInRange("peckDamage", 9.0, 0.0, 100.0);
        peckReach = builder.comment("Blocks the beak reaches beyond its hitbox, in front of it.")
                .defineInRange("peckReach", 3.0, 0.5, 8.0);
        buffetDamage = builder.comment("Damage (half hearts) of the wing buffet on the ground, to everyone in buffetRadius.")
                .defineInRange("buffetDamage", 6.0, 0.0, 100.0);
        buffetRadius = builder.comment("Blocks around it the wing buffet reaches.")
                .defineInRange("buffetRadius", 7.0, 1.0, 20.0);
        buffetKnockback = builder.comment("Strength of the wing buffet's push (a ravager's roar has ~1.5).")
                .defineInRange("buffetKnockback", 1.8, 0.0, 5.0);
        ashRainIntervalMin = builder.comment("Seconds between two ash rains while it circles, drawn between min and max.")
                .defineInRange("ashRainIntervalMin", 10, 1, 600);
        ashRainIntervalMax = builder.defineInRange("ashRainIntervalMax", 16, 1, 600);
        ashRainSeconds = builder.comment("Seconds an ash rain lasts; it circles tight over its target meanwhile.")
                .defineInRange("ashRainSeconds", 5, 1, 60);
        ashRainRadius = builder.comment("Radius in blocks of the ash falling under it.")
                .defineInRange("ashRainRadius", 7.0, 1.0, 24.0);
        ashTicksPerLayer = builder.comment("Ticks under the ash rain (open sky above you) per layer of ash. Six layers and you",
                        "stand fast; every layer slows you by a sixth.")
                .defineInRange("ashTicksPerLayer", 15, 1, 200);
        ashDecaySeconds = builder.comment("Seconds out of the rain before one layer of ash falls off.")
                .defineInRange("ashDecaySeconds", 5, 1, 120);
        ashEncasedSeconds = builder.comment("Seconds you stand fast (six layers) before you break free by yourself (back to",
                        "three layers). Other players free you sooner by hitting you.")
                .defineInRange("ashEncasedSeconds", 6, 1, 120);
        ashKnockOff = builder.comment("Layers of ash one hit of another player knocks off (no damage).")
                .defineInRange("ashKnockOff", 2, 1, 6);
        screamCooldownMin = builder.comment("Seconds between two screams of the choir, drawn between min and max.")
                .defineInRange("screamCooldownMin", 18, 1, 600);
        screamCooldownMax = builder.defineInRange("screamCooldownMax", 26, 1, 600);
        screamWindupTicks = builder.comment("Ticks from the head going back to the scream: the time to get away.")
                .defineInRange("screamWindupTicks", 24, 1, 100);
        screamRadius = builder.comment("Blocks within which the scream gives players weakness and nausea.")
                .defineInRange("screamRadius", 16.0, 1.0, 64.0);
        screamWeaknessSeconds = builder.defineInRange("screamWeaknessSeconds", 10, 0, 120);
        screamNauseaSeconds = builder.defineInRange("screamNauseaSeconds", 6, 0, 120);
        fleeRadius = builder.comment("Blocks within which animals and mobs flee from the scream.")
                .defineInRange("fleeRadius", 24.0, 0.0, 64.0);
        releaseCooldownMin = builder.comment("Seconds between two releases of victims from its feathers, drawn between min and",
                        "max.")
                .defineInRange("releaseCooldownMin", 20, 1, 600);
        releaseCooldownMax = builder.defineInRange("releaseCooldownMax", 30, 1, 600);
        releaseCount = builder.comment("Ash walkers per release.")
                .defineInRange("releaseCount", 2, 0, 10);
        maxWalkers = builder.comment("No release while this many ash walkers are within 48 blocks of it.")
                .defineInRange("maxWalkers", 6, 0, 50);
        stunGraceSeconds = builder.comment("Seconds it stays stunned on the ground after the effect corruptedadventure:stunned",
                        "(the strengthened ring 3 horn) has worn off. Stunned in the air, it falls.")
                .defineInRange("stunGraceSeconds", 3, 0, 60);
        stunImmunitySeconds = builder.comment("Seconds after a stun in which the horn does nothing to it.")
                .defineInRange("stunImmunitySeconds", 10, 0, 600);
        wingDamageFactor = builder.comment("Factor on damage dealt to its wings (their own hitboxes) instead of its body.")
                .defineInRange("wingDamageFactor", 0.5, 0.0, 2.0);
        meltPool = builder.comment("Whether it melts the ground to a lava pool to rise from when it appears on the ground.",
                        "Only natural blocks (tag corruptedadventure:risen_meltable) with open air above; the pool sets to a",
                        "permanent scar of basalt, magma and obsidian. Off: it rises without touching a block.")
                .define("meltPool", true);
        meltPoolRadius = builder.comment("Radius of the melt pool (3 = 7 wide).")
                .defineInRange("meltPoolRadius", 3, 1, 6);
        meltPoolSeconds = builder.comment("Seconds the pool stays lava after it has risen, before it sets.")
                .defineInRange("meltPoolSeconds", 15, 1, 600);
        builder.pop();
    }

    public int diveCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(diveCooldownMin, diveCooldownMax, roll);
    }

    public int ashRainIntervalTicks(int roll) {
        return NightmareConfig.ticksBetween(ashRainIntervalMin, ashRainIntervalMax, roll);
    }

    public int screamCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(screamCooldownMin, screamCooldownMax, roll);
    }

    public int releaseCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(releaseCooldownMin, releaseCooldownMax, roll);
    }
}
