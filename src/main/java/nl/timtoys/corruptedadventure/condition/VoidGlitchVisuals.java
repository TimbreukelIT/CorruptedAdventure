package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;

/**
 * The first void threshold: glitch visuals. Client only (stream A, package 98): the client reads the synced meter and
 * the synced threshold, so the server does nothing here.
 */
final class VoidGlitchVisuals implements ThresholdEffect {
    static final VoidGlitchVisuals VISUALS = new VoidGlitchVisuals();

    private VoidGlitchVisuals() {
    }

    @Override
    public void apply(ServerPlayer player) {
    }

    @Override
    public void clear(ServerPlayer player) {
    }
}
