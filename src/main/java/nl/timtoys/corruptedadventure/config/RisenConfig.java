package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code [nightmares.risen]}: The Risen, the ring 3 Nightmare (phase 4, fase-4-nightmares-concept.md "Ring 3 — De
 * Herrijzende"). The shared keys in {@link #common} (its main attack is the dive with its talons), then its own: flight,
 * landing, ash rain, the choir's scream, released victims, the horn's stun, the melt pool, and from the leven-ronde the
 * swoop, the ember rain and their burning ground. Read live.
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
    public final ModConfigSpec.DoubleValue landChance;
    public final ModConfigSpec.IntValue landIntervalMin;
    public final ModConfigSpec.IntValue landIntervalMax;
    public final ModConfigSpec.DoubleValue walkSpeed;
    public final ModConfigSpec.DoubleValue stompDamage;
    public final ModConfigSpec.DoubleValue stompRadius;
    public final ModConfigSpec.DoubleValue stompToss;
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
    public final ModConfigSpec.IntValue swoopCooldownMin;
    public final ModConfigSpec.IntValue swoopCooldownMax;
    public final ModConfigSpec.DoubleValue swoopHeight;
    public final ModConfigSpec.IntValue swoopWindupTicks;
    public final ModConfigSpec.DoubleValue swoopSpeed;
    public final ModConfigSpec.DoubleValue swoopDamage;
    public final ModConfigSpec.DoubleValue swoopKnockback;
    public final ModConfigSpec.IntValue swoopSkimBlocks;
    public final ModConfigSpec.IntValue emberRainCooldownMin;
    public final ModConfigSpec.IntValue emberRainCooldownMax;
    public final ModConfigSpec.IntValue emberRainWindupTicks;
    public final ModConfigSpec.IntValue emberRainSeconds;
    public final ModConfigSpec.DoubleValue emberRainRadius;
    public final ModConfigSpec.IntValue emberRainVolleyTicks;
    public final ModConfigSpec.IntValue emberRainPerVolley;
    public final ModConfigSpec.IntValue emberFallTicks;
    public final ModConfigSpec.DoubleValue emberDamage;
    public final ModConfigSpec.BooleanValue fire;
    public final ModConfigSpec.IntValue fireSeconds;
    public final ModConfigSpec.IntValue fireMaxTiles;
    public final ModConfigSpec.DoubleValue fireDamage;
    public final ModConfigSpec.IntValue fireIgniteSeconds;

    RisenConfig(ModConfigSpec.Builder builder) {
        builder.comment("The Risen, the ring 3 Nightmare (step 1: body and fight, summoned with /summon).",
                "attackDamage and attackKnockback are its dive (talons). movementSpeed is its walk in a ground fight.")
                .push("risen");
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
        landChance = builder.comment("Chance that it lands beside its target for a ground fight (it walks after its target,",
                        "stomps, pecks, beats its wings when surrounded) when a burst of attacks in the air is over.")
                .defineInRange("landChance", 0.4, 0.0, 1.0);
        landIntervalMin = builder.comment("Seconds of flight after which it lands for a ground fight anyway, drawn between min",
                        "and max.")
                .defineInRange("landIntervalMin", 40, 1, 600);
        landIntervalMax = builder.defineInRange("landIntervalMax", 60, 1, 600);
        landedSeconds = builder.comment("Seconds a ground fight lasts before it takes off again.")
                .defineInRange("landedSeconds", 15, 1, 120);
        walkSpeed = builder.comment("Factor on movementSpeed for its walk on the ground.")
                .defineInRange("walkSpeed", 0.8, 0.0, 3.0);
        stompDamage = builder.comment("Damage (half hearts) of the stomp to everyone on the ground within stompRadius; a jump",
                        "avoids it.")
                .defineInRange("stompDamage", 8.0, 0.0, 100.0);
        stompRadius = builder.comment("Blocks around it the stomp reaches.")
                .defineInRange("stompRadius", 5.0, 1.0, 16.0);
        stompToss = builder.comment("Upward speed the stomp throws players with.")
                .defineInRange("stompToss", 0.5, 0.0, 2.0);
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
        swoopCooldownMin = builder.comment("Seconds between two swoops (it climbs high, flares, then drops on a straight line and",
                        "skims the ground, leaving a trail of fire), drawn between min and max.")
                .defineInRange("swoopCooldownMin", 30, 1, 600);
        swoopCooldownMax = builder.defineInRange("swoopCooldownMax", 45, 1, 600);
        swoopHeight = builder.comment("Blocks above its target's ground it climbs to before the swoop.")
                .defineInRange("swoopHeight", 28.0, 8.0, 64.0);
        swoopWindupTicks = builder.comment("Ticks it hangs flaring at the top before it drops: the warning. The line is aimed at",
                        "where its target stands when it drops, and does not steer after.")
                .defineInRange("swoopWindupTicks", 30, 5, 100);
        swoopSpeed = builder.comment("Speed in blocks per tick of the swoop.")
                .defineInRange("swoopSpeed", 2.0, 0.5, 4.0);
        swoopDamage = builder.comment("Damage (half hearts) of the swoop to whoever it passes through; they also catch fire.")
                .defineInRange("swoopDamage", 16.0, 0.0, 100.0);
        swoopKnockback = builder.comment("Strength of the push of the swoop.")
                .defineInRange("swoopKnockback", 1.5, 0.0, 5.0);
        swoopSkimBlocks = builder.comment("Blocks it skims the ground after the drop, laying the fire trail (3 wide).")
                .defineInRange("swoopSkimBlocks", 14, 0, 48);
        emberRainCooldownMin = builder.comment("Seconds between two ember rains, drawn between min and max. It hangs over the area",
                        "its target stood in, the rim smoulders, then embers fall; each marks its spot before it lands.")
                .defineInRange("emberRainCooldownMin", 22, 1, 600);
        emberRainCooldownMax = builder.defineInRange("emberRainCooldownMax", 32, 1, 600);
        emberRainWindupTicks = builder.comment("Ticks the rim smoulders before the first embers fall.")
                .defineInRange("emberRainWindupTicks", 30, 0, 100);
        emberRainSeconds = builder.comment("Seconds the embers fall.")
                .defineInRange("emberRainSeconds", 5, 1, 30);
        emberRainRadius = builder.comment("Radius in blocks of the ember rain's area.")
                .defineInRange("emberRainRadius", 7.0, 2.0, 20.0);
        emberRainVolleyTicks = builder.comment("Ticks between two volleys of embers.")
                .defineInRange("emberRainVolleyTicks", 4, 1, 40);
        emberRainPerVolley = builder.comment("Embers per volley.")
                .defineInRange("emberRainPerVolley", 2, 1, 10);
        emberFallTicks = builder.comment("Ticks from an ember's mark on the ground to its landing: the time to step aside.")
                .defineInRange("emberFallTicks", 20, 5, 60);
        emberDamage = builder.comment("Damage (half hearts) of an ember to whoever stands within 1.5 blocks of where it lands.")
                .defineInRange("emberDamage", 4.0, 0.0, 100.0);
        fire = builder.comment("Whether the swoop and the ember rain leave burning ground. Never fire blocks: tiles of its own on",
                        "free ground (air on a sturdy top face) that burn out by themselves and do not spread. Off: no fire.")
                .define("fire", true);
        fireSeconds = builder.comment("Seconds a tile of its fire burns.")
                .defineInRange("fireSeconds", 6, 1, 60);
        fireMaxTiles = builder.comment("Most tiles burning at once per Risen; the oldest go out first.")
                .defineInRange("fireMaxTiles", 96, 0, 512);
        fireDamage = builder.comment("Damage (half hearts) every half second to whoever stands in its fire.")
                .defineInRange("fireDamage", 1.0, 0.0, 20.0);
        fireIgniteSeconds = builder.comment("Seconds whoever stands in its fire, or is hit by an ember or the swoop, burns on.")
                .defineInRange("fireIgniteSeconds", 3, 0, 30);
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

    public int landIntervalTicks(int roll) {
        return NightmareConfig.ticksBetween(landIntervalMin, landIntervalMax, roll);
    }

    public int swoopCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(swoopCooldownMin, swoopCooldownMax, roll);
    }

    public int emberRainCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(emberRainCooldownMin, emberRainCooldownMax, roll);
    }
}
