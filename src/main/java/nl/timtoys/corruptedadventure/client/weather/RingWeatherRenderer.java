package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
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

    /**
     * Whether weather fog and screen effects may apply: the camera is in air and its entity has no blindness or
     * darkness. Otherwise vanilla's fog (water, lava, powder snow, blindness) stays untouched.
     */
    static boolean clearView(Camera camera) {
        if (camera.getFluidInCamera() != FogType.NONE) {
            return false;
        }
        return !(camera.getEntity() instanceof LivingEntity living)
                || !living.hasEffect(MobEffects.BLINDNESS) && !living.hasEffect(MobEffects.DARKNESS);
    }

    /** Daylight factor of the fog colour, 0.2 at night to 1 at noon (vanilla's own sky darkening, incl. rain). */
    static float daylight(ClientLevel level, float partialTick) {
        return level.getSkyDarken(partialTick);
    }
}
