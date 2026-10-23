package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.world.entity.player.Player;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.condition.ConditionDefinitions;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModAttachments;

import java.util.Arrays;
import java.util.Optional;

/**
 * The ring 5 meters as the client sees them (package 98): the synced value of the local player (the existing
 * {@code condition_sync}, in his attachment) and the thresholds from the synced SERVER config. Void ({@link Condition#VOID},
 * package 91) and silence (contract §5: {@code Condition.SILENCE}, name {@code silence}, package 97). The silence is
 * looked up by its name, so this compiles and runs before stream C's 97 is merged (then there is simply no silence).
 * Client only.
 */
final class Ring5Meters {
    /** Contract §5: the name of the silence condition (97). */
    static final String SILENCE_NAME = "silence";
    private static final Optional<Condition> SILENCE = Arrays.stream(Condition.values())
            .filter(condition -> condition.getSerializedName().equals(SILENCE_NAME))
            .findFirst();

    private Ring5Meters() {
    }

    /** The silence condition, empty until stream C's 97 adds it. */
    static Optional<Condition> silence() {
        return SILENCE;
    }

    /** The synced meter of {@code condition}, 0–100. */
    static int value(Player player, Condition condition) {
        return player.getData(ModAttachments.CONDITION_METERS).rounded(condition);
    }

    /**
     * The thresholds of {@code condition} in rising order (30/60/90 by default), or empty while the synced SERVER config
     * is not there (before joining) or the list is shorter than {@code needed}.
     */
    static Optional<int[]> thresholds(Condition condition, int needed) {
        if (!ServerConfig.SPEC.isLoaded()) {
            return Optional.empty();
        }
        int[] levels = ConditionDefinitions.thresholdLevels(condition);
        return levels.length >= needed ? Optional.of(levels) : Optional.empty();
    }
}
