package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code [nightmares.layered]}: The Layered, the ring 2 Nightmare (phase 4, fase-4-nightmares-concept.md "Ring 2 — De
 * Gelaagde"). The shared keys in {@link #common} (its main attack is the bite of its beak), then its own: the grab and
 * the climb with a player, the rock fall (it changes the world, so it can be switched off), the armoured back and the fall
 * from the wall. Read live, so edits apply to a Layered that is already fighting.
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
        builder.pop();
    }

    public int grabCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(grabCooldownMin, grabCooldownMax, roll);
    }

    public int rockfallCooldownTicks(int roll) {
        return NightmareConfig.ticksBetween(rockfallCooldownMin, rockfallCooldownMax, roll);
    }
}
