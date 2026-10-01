package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;

/**
 * What happens while a meter is at or above a threshold. Thresholds are cumulative: at 75 the effects of 50 and 75
 * are both active.
 */
public interface ThresholdEffect {
    /** Called every second (every handler tick) while the threshold is reached. */
    void apply(ServerPlayer player);

    /** Called when the threshold is no longer reached (also once after loading, to clean up); may be a no-op. */
    void clear(ServerPlayer player);
}
