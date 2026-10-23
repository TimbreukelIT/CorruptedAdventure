package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;

/**
 * The first two silence thresholds (package 97): sound muffled, then the HUD hidden except the hotbar. Client only
 * (stream A, package 98): the client reads the synced meter and the synced thresholds, so the server does nothing here.
 */
enum SilenceCue implements ThresholdEffect {
    MUFFLED,
    HUD_HIDDEN;

    @Override
    public void apply(ServerPlayer player) {
    }

    @Override
    public void clear(ServerPlayer player) {
    }
}
