package nl.timtoys.corruptedadventure.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code [weakness_items]}: the items against the Nightmares' weaknesses (phase 4, stream I): the sunstone flash (ring 2,
 * "dazzled") and the volcanite horn (ring 3, "stunned"). The durations are those on ordinary mobs and players; a
 * Nightmare's own reaction has its own numbers in its section. Read live.
 */
public final class WeaknessItemsConfig {
    public final ModConfigSpec.DoubleValue flashRange;
    public final ModConfigSpec.DoubleValue flashConeDegrees;
    public final ModConfigSpec.IntValue dazzledSeconds;
    public final ModConfigSpec.IntValue flashCooldownSeconds;
    public final ModConfigSpec.DoubleValue hornRadius;
    public final ModConfigSpec.IntValue stunnedSeconds;
    public final ModConfigSpec.IntValue hornCooldownSeconds;

    WeaknessItemsConfig(ModConfigSpec.Builder builder) {
        builder.comment("Items against the Nightmares' weaknesses (phase 4).").push("weakness_items");
        flashRange = builder.comment("Blocks the sunstone flash reaches (from the eyes, only with a clear line of sight).")
                .defineInRange("flashRange", 16.0, 1.0, 64.0);
        flashConeDegrees = builder.comment("Half the opening angle of the flash's cone round the look direction, in degrees.")
                .defineInRange("flashConeDegrees", 30.0, 1.0, 90.0);
        dazzledSeconds = builder.comment("Seconds of 'dazzled' on what the flash hits (ordinary mobs and other players).",
                        "Players are only hit where they can be harmed (the server's pvp setting and teams).")
                .defineInRange("dazzledSeconds", 3, 1, 60);
        flashCooldownSeconds = builder.comment("Seconds before the sunstone flash can be used again.")
                .defineInRange("flashCooldownSeconds", 15, 1, 3600);
        hornRadius = builder.comment("Blocks round the one who blows the volcanite horn within which it stuns (no line of sight needed).")
                .defineInRange("hornRadius", 12.0, 1.0, 64.0);
        stunnedSeconds = builder.comment("Seconds of 'stunned' on what the horn reaches (ordinary mobs; never players).")
                .defineInRange("stunnedSeconds", 2, 1, 60);
        hornCooldownSeconds = builder.comment("Seconds before the volcanite horn can be blown again.")
                .defineInRange("hornCooldownSeconds", 30, 1, 3600);
        builder.pop();
    }
}
