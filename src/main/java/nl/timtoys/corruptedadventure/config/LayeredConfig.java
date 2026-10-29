package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code [nightmares.layered]}: The Layered, the ring 2 Nightmare (phase 4, fase-4-nightmares-concept.md "Ring 2 — De
 * Gelaagde"). The shared keys in {@link #common} (its main attack is the bite of its beak), then its own: the grab and
 * the climb with a player, the rock fall (it changes the world, so it can be switched off), the armoured back and the fall
 * from the wall; then those of the life round: the pounce from a wall, the burrow under the sand, the dead heads' spit. Read live, so edits apply to a Layered that is already fighting.
 */
public final class LayeredConfig {
    /** Also the attribute defaults of {@code TheLayered}. */
    public static final NightmareConfig.Defaults DEFAULTS = new NightmareConfig.Defaults(450.0, 6.0, 0.28, 8.0, 0.5, 350);

    public final NightmareConfig common;
    public final ModConfigSpec.DoubleValue biteReach;
    public final ModConfigSpec.IntValue biteWindupTicks;
    public final ModConfigSpec.IntValue biteCooldownTicks;
    public final ModConfigSpec.IntValue grabCooldownMin;
    public final ModConfigSpec.IntValue grabCooldownMax;
    public final ModConfigSpec.DoubleValue grabDamage;
    public final ModConfigSpec.IntValue wallSearchRadius;
    public final ModConfigSpec.DoubleValue carryHeight;
    public final ModConfigSpec.IntValue carrySeconds;
    public final ModConfigSpec.DoubleValue shakeDamage;
    public final ModConfigSpec.IntValue freeHitsByOthers;
    public final ModConfigSpec.IntValue freeHitsBySelf;
    public final ModConfigSpec.BooleanValue rockfallEnabled;
    public final ModConfigSpec.IntValue rockfallCooldownMin;
    public final ModConfigSpec.IntValue rockfallCooldownMax;
    public final ModConfigSpec.IntValue rockfallWindupTicks;
    public final ModConfigSpec.IntValue rockfallBlocksPerAttack;
    public final ModConfigSpec.IntValue rockfallBlocksPerFight;
    public final ModConfigSpec.IntValue rockfallReachUp;
    public final ModConfigSpec.DoubleValue rockfallDamagePerBlockFallen;
    public final ModConfigSpec.IntValue rockfallMaxDamage;
    public final ModConfigSpec.DoubleValue perchHeight;
    public final ModConfigSpec.DoubleValue backDamageMultiplier;
    public final ModConfigSpec.DoubleValue fallImpactDamage;
    public final ModConfigSpec.IntValue fallenSeconds;
    public final ModConfigSpec.BooleanValue dazzleFlipsOnGround;
    public final ModConfigSpec.IntValue dazzleFlipCooldownSeconds;
    public final ModConfigSpec.IntValue specialGapTicks;
    public final ModConfigSpec.IntValue pounceCooldownMin;
    public final ModConfigSpec.IntValue pounceCooldownMax;
    public final ModConfigSpec.IntValue pounceWindupTicks;
    public final ModConfigSpec.DoubleValue pounceMinDrop;
    public final ModConfigSpec.DoubleValue pounceRange;
    public final ModConfigSpec.DoubleValue pounceRadius;
    public final ModConfigSpec.DoubleValue pounceDamage;
    public final ModConfigSpec.DoubleValue pounceKnockback;
    public final ModConfigSpec.BooleanValue burrowEnabled;
    public final ModConfigSpec.IntValue burrowCooldownMin;
    public final ModConfigSpec.IntValue burrowCooldownMax;
    public final ModConfigSpec.IntValue burrowDigTicks;
    public final ModConfigSpec.DoubleValue burrowRange;
    public final ModConfigSpec.DoubleValue burrowSpeed;
    public final ModConfigSpec.IntValue burrowMaxSeconds;
    public final ModConfigSpec.IntValue burrowWarnTicks;
    public final ModConfigSpec.DoubleValue burrowRadius;
    public final ModConfigSpec.DoubleValue burrowDamage;
    public final ModConfigSpec.DoubleValue burrowLaunch;
    public final ModConfigSpec.IntValue spitCooldownMin;
    public final ModConfigSpec.IntValue spitCooldownMax;
    public final ModConfigSpec.IntValue spitWindupTicks;
    public final ModConfigSpec.DoubleValue spitMinRange;
    public final ModConfigSpec.DoubleValue spitRange;
    public final ModConfigSpec.DoubleValue spitSpeed;
    public final ModConfigSpec.DoubleValue spitRadius;
    public final ModConfigSpec.DoubleValue spitDamage;
    public final ModConfigSpec.IntValue spitBlindSeconds;

    LayeredConfig(ModConfigSpec.Builder builder) {
        builder.comment("The Layered, the ring 2 Nightmare (step 1: body and fight, summoned with /summon).",
                "attackDamage and attackKnockback are the bite of its beak.").push("layered");
        common = new NightmareConfig(builder, DEFAULTS);
        biteReach = builder.comment("Blocks from the tip of its beak within which the bite (and the grab) lands.")
                .defineInRange("biteReach", 2.0, 0.5, 6.0);
        biteWindupTicks = builder.comment("Ticks between rearing up and the bite: the time to step away.")
                .defineInRange("biteWindupTicks", 10, 1, 60);
        biteCooldownTicks = builder.comment("Ticks after a bite before the next can start.")
                .defineInRange("biteCooldownTicks", 30, 0, 200);
        grabCooldownMin = builder.comment("Seconds between two grabs, drawn between min and max. A bite in between does not",
                        "hold on.")
                .defineInRange("grabCooldownMin", 15, 1, 600);
        grabCooldownMax = builder.defineInRange("grabCooldownMax", 20, 1, 600);
        grabDamage = builder.comment("Damage of the bite that grabs (it holds on, so it hurts less than a plain bite).")
                .defineInRange("grabDamage", 4.0, 0.0, 100.0);
        wallSearchRadius = builder.comment("Blocks around it where it looks for a wall to climb (with a grabbed player, or to",
                        "drop rocks from).")
                .defineInRange("wallSearchRadius", 12, 2, 32);
        carryHeight = builder.comment("Blocks it climbs with a grabbed player before it lets go (the wall's top ends it sooner).")
                .defineInRange("carryHeight", 10.0, 1.0, 64.0);
        carrySeconds = builder.comment("Seconds at most it holds a player; without a wall it shakes them and throws them up.")
                .defineInRange("carrySeconds", 6, 1, 60);
        shakeDamage = builder.comment("Damage per shake (every half second) to a held player when there is no wall to climb.")
                .defineInRange("shakeDamage", 2.0, 0.0, 100.0);
        freeHitsByOthers = builder.comment("Hits on its head by another player that make it let go of the held player.")
                .defineInRange("freeHitsByOthers", 1, 1, 20);
        freeHitsBySelf = builder.comment("Hits on its head by the held player themselves that make it let go.")
                .defineInRange("freeHitsBySelf", 3, 1, 20);
        rockfallEnabled = builder.comment("Whether it breaks rock and loose sand loose above its target. The blocks are real and",
                        "stay where they land (only natural stone and sand: the block tag",
                        "corruptedadventure:layered_rockfall). false: it never uses this attack.")
                .define("rockfallEnabled", true);
        rockfallCooldownMin = builder.comment("Seconds between two rock falls, drawn between min and max.")
                .defineInRange("rockfallCooldownMin", 10, 1, 600);
        rockfallCooldownMax = builder.defineInRange("rockfallCooldownMax", 16, 1, 600);
        rockfallWindupTicks = builder.comment("Ticks of hammering on the wall before the rock breaks loose.")
                .defineInRange("rockfallWindupTicks", 14, 1, 100);
        rockfallBlocksPerAttack = builder.comment("Blocks at most that one rock fall breaks loose.")
                .defineInRange("rockfallBlocksPerAttack", 6, 1, 32);
        rockfallBlocksPerFight = builder.comment("Blocks at most that one Layered breaks loose in its whole life (it keeps the",
                        "count when the world is saved).")
                .defineInRange("rockfallBlocksPerFight", 48, 0, 1024);
        rockfallReachUp = builder.comment("Blocks above its target's feet where it looks for rock to break loose.")
                .defineInRange("rockfallReachUp", 12, 3, 32);
        rockfallDamagePerBlockFallen = builder.comment("Damage per block a falling rock has fallen (an anvil has 2).")
                .defineInRange("rockfallDamagePerBlockFallen", 2.0, 0.0, 10.0);
        rockfallMaxDamage = builder.comment("Damage at most of one falling rock (an anvil has 40).")
                .defineInRange("rockfallMaxDamage", 20, 0, 100);
        perchHeight = builder.comment("Blocks it climbs up a wall before it starts a rock fall from there.")
                .defineInRange("perchHeight", 5.0, 1.0, 32.0);
        backDamageMultiplier = builder.comment("Damage factor on its armoured back. Its belly takes full damage: a hit from below",
                        "it (it crawls over you on a wall or a ledge), a hit on its head, or any hit while it lies on its back.")
                .defineInRange("backDamageMultiplier", 0.4, 0.0, 1.0);
        fallImpactDamage = builder.comment("Damage it takes when it lands after falling from a wall (fire in its fur, the",
                        "dazzle of a sunstone flash).")
                .defineInRange("fallImpactDamage", 12.0, 0.0, 1000.0);
        fallenSeconds = builder.comment("Seconds it lies helpless on its back after such a fall (0: it gets up at once).")
                .defineInRange("fallenSeconds", 4, 0, 60);
        dazzleFlipsOnGround = builder.comment("Whether the dazzle also throws it on its back when it stands on the ground.")
                .define("dazzleFlipsOnGround", true);
        dazzleFlipCooldownSeconds = builder.comment("Seconds after getting up before a dazzle on the ground can throw it on its",
                        "back again, so sunstone flashes cannot keep it down (a fall from a wall always does).")
                .defineInRange("dazzleFlipCooldownSeconds", 8, 0, 600);
        specialGapTicks = builder.comment("Ticks after the end of a grab, rock fall, pounce, burrow or spit before the next of",
                        "these can start (the bite does not wait).")
                .defineInRange("specialGapTicks", 30, 0, 600);
        pounceCooldownMin = builder.comment("The pounce from a wall or a ledge: seconds between two, drawn between min and max.")
                .defineInRange("pounceCooldownMin", 8, 1, 600);
        pounceCooldownMax = builder.defineInRange("pounceCooldownMax", 12, 1, 600);
        pounceWindupTicks = builder.comment("Ticks it crouches (bones rattling, dust falling) before it leaps: the time to run.",
                        "It lands where its target stood when it leapt.")
                .defineInRange("pounceWindupTicks", 16, 1, 100);
        pounceMinDrop = builder.comment("From a ledge (not a wall) it only pounces on a target at least this many blocks below it.")
                .defineInRange("pounceMinDrop", 3.0, 0.0, 32.0);
        pounceRange = builder.comment("Blocks at most (sideways) it pounces; at least 3.")
                .defineInRange("pounceRange", 14.0, 3.0, 32.0);
        pounceRadius = builder.comment("Blocks around where it lands that the impact hits.")
                .defineInRange("pounceRadius", 3.0, 0.5, 8.0);
        pounceDamage = builder.comment("Damage of the impact.")
                .defineInRange("pounceDamage", 10.0, 0.0, 100.0);
        pounceKnockback = builder.comment("How hard the impact throws those it hits away (blocks per tick).")
                .defineInRange("pounceKnockback", 0.8, 0.0, 4.0);
        burrowEnabled = builder.comment("Whether it burrows into sand or gravel and bursts up under its target (the block tag",
                        "corruptedadventure:layered_burrow). No block changes; false: it never uses this attack.")
                .define("burrowEnabled", true);
        burrowCooldownMin = builder.comment("Seconds between two burrows, drawn between min and max.")
                .defineInRange("burrowCooldownMin", 18, 1, 600);
        burrowCooldownMax = builder.defineInRange("burrowCooldownMax", 26, 1, 600);
        burrowDigTicks = builder.comment("Ticks it digs itself in (sand spraying) before it is gone under the ground.")
                .defineInRange("burrowDigTicks", 30, 1, 200);
        burrowRange = builder.comment("Blocks at most to a target standing on sand or gravel for it to burrow toward them.")
                .defineInRange("burrowRange", 20.0, 2.0, 64.0);
        burrowSpeed = builder.comment("Blocks per tick it moves under the sand (a sprinting player does about 0.28).")
                .defineInRange("burrowSpeed", 0.3, 0.05, 2.0);
        burrowMaxSeconds = builder.comment("Seconds at most it moves under the sand before it bursts up wherever it is.")
                .defineInRange("burrowMaxSeconds", 8, 1, 60);
        burrowWarnTicks = builder.comment("Ticks the sand heaves where it will burst up: the time to step away.")
                .defineInRange("burrowWarnTicks", 20, 1, 100);
        burrowRadius = builder.comment("Blocks around where it bursts up that it hits.")
                .defineInRange("burrowRadius", 2.5, 0.5, 8.0);
        burrowDamage = builder.comment("Damage of bursting up under someone.")
                .defineInRange("burrowDamage", 12.0, 0.0, 100.0);
        burrowLaunch = builder.comment("How hard bursting up throws those it hits up (blocks per tick).")
                .defineInRange("burrowLaunch", 0.9, 0.0, 4.0);
        spitCooldownMin = builder.comment("The dead heads spit dust and bone splinters: seconds between two spits, drawn between",
                        "min and max.")
                .defineInRange("spitCooldownMin", 9, 1, 600);
        spitCooldownMax = builder.defineInRange("spitCooldownMax", 14, 1, 600);
        spitWindupTicks = builder.comment("Ticks the dead heads rear back before they spit. They aim where the target was when",
                        "they reared back, so a step aside dodges it.")
                .defineInRange("spitWindupTicks", 18, 1, 100);
        spitMinRange = builder.comment("Blocks: closer than this it bites instead.")
                .defineInRange("spitMinRange", 5.0, 0.0, 32.0);
        spitRange = builder.comment("Blocks at most to its target for a spit (the clouds fly 2 blocks further).")
                .defineInRange("spitRange", 16.0, 2.0, 48.0);
        spitSpeed = builder.comment("Blocks per tick a cloud flies.")
                .defineInRange("spitSpeed", 1.2, 0.1, 4.0);
        spitRadius = builder.comment("Blocks around a cloud that it hits.")
                .defineInRange("spitRadius", 1.0, 0.1, 4.0);
        spitDamage = builder.comment("Damage of one cloud (there are two, one per dead head).")
                .defineInRange("spitDamage", 3.0, 0.0, 100.0);
        spitBlindSeconds = builder.comment("Seconds of blindness from a cloud (0: none).")
                .defineInRange("spitBlindSeconds", 3, 0, 60);
        builder.pop();
    }

    public int grabCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(grabCooldownMin, grabCooldownMax, roll);
    }

    public int rockfallCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(rockfallCooldownMin, rockfallCooldownMax, roll);
    }

    public int pounceCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(pounceCooldownMin, pounceCooldownMax, roll);
    }

    public int burrowCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(burrowCooldownMin, burrowCooldownMax, roll);
    }

    public int spitCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(spitCooldownMin, spitCooldownMax, roll);
    }
}
