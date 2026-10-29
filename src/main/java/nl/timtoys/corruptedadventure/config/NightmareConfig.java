package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The keys every Nightmare has, inside its own section ({@code [nightmares.<name>]}; the caller pushes and pops it):
 * health and its scaling per player, the fight radius, the main attack, armor and speed, the retreat and the experience;
 * then the shared reactions of the life round (stagger, pace, search, target switching; fase-4-nightmares-concept.md
 * "Leven-ronde"). Read live by {@code entity/nightmare/Nightmare}. A Nightmare's own config class makes one of these with
 * its defaults and adds its attack-specific keys after it.
 */
public final class NightmareConfig {
    public final ModConfigSpec.DoubleValue baseHealth;
    public final ModConfigSpec.DoubleValue healthPerExtraPlayer;
    public final ModConfigSpec.DoubleValue fightRadius;
    public final ModConfigSpec.DoubleValue armor;
    public final ModConfigSpec.DoubleValue movementSpeed;
    public final ModConfigSpec.DoubleValue attackDamage;
    public final ModConfigSpec.DoubleValue attackKnockback;
    public final ModConfigSpec.DoubleValue retreatRadius;
    public final ModConfigSpec.IntValue retreatSeconds;
    public final ModConfigSpec.IntValue experience;
    public final ModConfigSpec.DoubleValue staggerDamageShare;
    public final ModConfigSpec.IntValue staggerWindowSeconds;
    public final ModConfigSpec.IntValue staggerTicks;
    public final ModConfigSpec.IntValue staggerCooldownSeconds;
    public final ModConfigSpec.IntValue burstMinAttacks;
    public final ModConfigSpec.IntValue burstMaxAttacks;
    public final ModConfigSpec.IntValue restMinSeconds;
    public final ModConfigSpec.IntValue restMaxSeconds;
    public final ModConfigSpec.DoubleValue restBackOffChance;
    public final ModConfigSpec.DoubleValue restDistance;
    public final ModConfigSpec.IntValue searchAfterSeconds;
    public final ModConfigSpec.IntValue searchSeconds;
    public final ModConfigSpec.IntValue aggroHalfLifeSeconds;
    public final ModConfigSpec.DoubleValue targetSwitchMargin;
    public final ModConfigSpec.IntValue targetSwitchCooldownSeconds;
    public final ModConfigSpec.DoubleValue preyRadius;
    public final ModConfigSpec.DoubleValue preyHealthShare;
    public final ModConfigSpec.DoubleValue preyBonus;

    /** The defaults of one Nightmare (design doc: HP 300 / 450 / 600 / 800 / 1200 for rings 1–5). */
    public record Defaults(double baseHealth, double armor, double movementSpeed, double attackDamage,
                           double attackKnockback, int experience) {
    }

    public NightmareConfig(ModConfigSpec.Builder builder, Defaults defaults) {
        baseHealth = builder.comment("Max health with one player in the fight.")
                .defineInRange("baseHealth", defaults.baseHealth(), 1.0, 10000.0);
        healthPerExtraPlayer = builder.comment("Extra max health per further player in the fight, as a part of baseHealth",
                        "(0.5: two players 1.5x, three 2x). Rescaled when a player joins or leaves; the health ratio is kept.")
                .defineInRange("healthPerExtraPlayer", 0.5, 0.0, 10.0);
        fightRadius = builder.comment("Blocks within which a living player in survival or adventure is in the fight (health",
                        "scaling and attacks that pick players).")
                .defineInRange("fightRadius", 32.0, 4.0, 128.0);
        armor = builder.comment("Armor points (the Wither has 4).")
                .defineInRange("armor", defaults.armor(), 0.0, 30.0);
        movementSpeed = builder.comment("Movement speed attribute (a zombie has 0.23).")
                .defineInRange("movementSpeed", defaults.movementSpeed(), 0.0, 1.0);
        attackDamage = builder.comment("Damage (half hearts) of its main attack, before armor.")
                .defineInRange("attackDamage", defaults.attackDamage(), 0.0, 100.0);
        attackKnockback = builder.comment("Knockback of its main attack (attack_knockback attribute; a ravager has 1.5).")
                .defineInRange("attackKnockback", defaults.attackKnockback(), 0.0, 5.0);
        retreatRadius = builder.comment("Blocks within which a living, non-spectator player keeps it here.")
                .defineInRange("retreatRadius", 64.0, 8.0, 256.0);
        retreatSeconds = builder.comment("Seconds without such a player before it retreats (disappears, no drop).")
                .defineInRange("retreatSeconds", 30, 1, 3600);
        experience = builder.comment("Experience dropped on its defeat (the Wither drops 50).")
                .defineInRange("experience", defaults.experience(), 0, 100000);
        staggerDamageShare = builder.comment("Damage within staggerWindowSeconds, as a part of its full max health, that makes it",
                        "stagger (attacks break off, an open window). A hit on its weak spot staggers it at once.")
                .defineInRange("staggerDamageShare", 0.06, 0.0, 1.0);
        staggerWindowSeconds = builder.comment("Seconds over which the damage for a stagger adds up.")
                .defineInRange("staggerWindowSeconds", 4, 1, 60);
        staggerTicks = builder.comment("Ticks it staggers (20 = 1 s). 0: it never staggers.")
                .defineInRange("staggerTicks", 30, 0, 200);
        staggerCooldownSeconds = builder.comment("Seconds after a stagger in which nothing staggers it again (no stun lock).")
                .defineInRange("staggerCooldownSeconds", 10, 0, 600);
        burstMinAttacks = builder.comment("Attacks in a row before a rest window: at least this many ...")
                .defineInRange("burstMinAttacks", 2, 1, 20);
        burstMaxAttacks = builder.comment("... and at most this many (drawn per burst).")
                .defineInRange("burstMaxAttacks", 4, 1, 20);
        restMinSeconds = builder.comment("Seconds of a rest window after a burst (no new attack; it backs off or circles): at least ...")
                .defineInRange("restMinSeconds", 3, 0, 60);
        restMaxSeconds = builder.comment("... and at most.")
                .defineInRange("restMaxSeconds", 5, 0, 60);
        restBackOffChance = builder.comment("Chance that a rest window backs off from its target instead of circling it.")
                .defineInRange("restBackOffChance", 0.5, 0.0, 1.0);
        restDistance = builder.comment("Blocks from its target it keeps in a rest window.")
                .defineInRange("restDistance", 10.0, 2.0, 64.0);
        searchAfterSeconds = builder.comment("Seconds its target is out of sight before it searches: to the last place it saw",
                        "the target, sniffing and looking around.")
                .defineInRange("searchAfterSeconds", 3, 1, 600);
        searchSeconds = builder.comment("Seconds a search lasts; then it sniffs the target out (it knows where it is again).")
                .defineInRange("searchSeconds", 10, 1, 600);
        aggroHalfLifeSeconds = builder.comment("Seconds in which the damage a player did to it counts half for the target choice.")
                .defineInRange("aggroHalfLifeSeconds", 10, 1, 600);
        targetSwitchMargin = builder.comment("A player becomes its target when its score beats the current target's this many",
                        "times over (score: damage done recently, plus preyBonus when near and weak).")
                .defineInRange("targetSwitchMargin", 1.5, 1.0, 10.0);
        targetSwitchCooldownSeconds = builder.comment("Seconds after a target switch before it switches again.")
                .defineInRange("targetSwitchCooldownSeconds", 5, 0, 600);
        preyRadius = builder.comment("Blocks within which a weak player is prey.")
                .defineInRange("preyRadius", 8.0, 0.0, 64.0);
        preyHealthShare = builder.comment("Health share (of the player's max) at or below which a player is weak.")
                .defineInRange("preyHealthShare", 0.35, 0.0, 1.0);
        preyBonus = builder.comment("Score of being prey, in damage points.")
                .defineInRange("preyBonus", 15.0, 0.0, 10000.0);
    }

    /** A random whole number of ticks between two second values; {@code min} when {@code max} is below it. */
    public static int ticksBetween(ModConfigSpec.IntValue minSeconds, ModConfigSpec.IntValue maxSeconds, int roll) {
        int lo = minSeconds.get();
        int hi = Math.max(lo, maxSeconds.get());
        return (lo + Math.floorMod(roll, hi - lo + 1)) * 20;
    }
}
