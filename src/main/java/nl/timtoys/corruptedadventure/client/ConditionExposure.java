package nl.timtoys.corruptedadventure.client;

import net.minecraft.client.Minecraft;

/**
 * Whether the local player feels the condition meters: not in creative or spectator, like the food bar. The server
 * counts no thresholds there (the meter only stops rising), so the HUD bars and the client effects of the meters both
 * ask this one check.
 */
public final class ConditionExposure {
    private ConditionExposure() {
    }

    public static boolean exposed(Minecraft minecraft) {
        return minecraft.gameMode != null && minecraft.gameMode.canHurtPlayer();
    }
}
