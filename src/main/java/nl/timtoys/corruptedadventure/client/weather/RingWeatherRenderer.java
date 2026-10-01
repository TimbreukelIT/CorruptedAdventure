package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Rendering of one ring weather type, registered in {@link RingWeatherRenderers} by the weather changes (28, 45, 66,
 * 85). Only default methods, so a later method does not break existing renderers. Called only while the type is felt
 * at the camera/player: {@code strength} in (0, 1] = time fade × spatial share (see {@code RingWeatherMix}).
 */
public interface RingWeatherRenderer {
    /** Every client tick (not while paused): particles, sounds. */
    default void clientTick(ClientLevel level, LocalPlayer player, float strength) {
    }

    /** Fog distance and shape. */
    default void renderFog(ViewportEvent.RenderFog event, float strength) {
    }

    /** Fog colour, on top of the biome's colours (those are biome data). */
    default void fogColor(ViewportEvent.ComputeFogColor event, float strength) {
    }

    /** World rendering; check {@code event.getStage()} (e.g. {@code AFTER_WEATHER}). */
    default void renderLevel(RenderLevelStageEvent event, float strength) {
    }
}
