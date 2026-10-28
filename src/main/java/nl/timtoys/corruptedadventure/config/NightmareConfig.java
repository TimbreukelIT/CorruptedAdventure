package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The keys every Nightmare has, inside its own section ({@code [nightmares.<name>]}; the caller pushes and pops it):
 * health and its scaling per player, the fight radius, the main attack, armor and speed, the retreat and the experience.
 * Read live by {@code entity/nightmare/Nightmare} once a second. A Nightmare's own config class makes one of these with
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
    }

    /** A random whole number of ticks between two second values; {@code min} when {@code max} is below it. */
    public static int ticksBetween(ModConfigSpec.IntValue minSeconds, ModConfigSpec.IntValue maxSeconds, int roll) {
        int lo = minSeconds.get();
        int hi = Math.max(lo, maxSeconds.get());
        return (lo + Math.floorMod(roll, hi - lo + 1)) * 20;
    }
}
