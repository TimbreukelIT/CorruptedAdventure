package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Heat wave (ring 2): a light warm haze (fog a bit warmer and closer) and shimmering air through
 * {@link HeatHazeEffect}, all scaled by strength × {@link SkyExposure}. The shimmer also follows vanilla's
 * accessibility option "Distortion Effects" (0 = none).
 */
final class HeatWaveRenderer implements RingWeatherRenderer {
    /** Far plane factor of a full heat wave in the open. */
    static final float FOG_FAR_FACTOR = 0.85F;
    /** How far the fog colour moves towards the warm colour in a full heat wave. */
    private static final float COLOUR_SHARE = 0.3F;
    /** Warm white-yellow #F2DDAA. */
    private static final float WARM_RED = 0xF2 / 255.0F;
    private static final float WARM_GREEN = 0xDD / 255.0F;
    private static final float WARM_BLUE = 0xAA / 255.0F;

    @Override
    public void renderFog(ViewportEvent.RenderFog event, float strength) {
        if (!RingWeatherRenderer.clearView(event.getCamera())) {
            return;
        }
        float amount = strength * SkyExposure.get();
        if (amount <= 0.0F) {
            return;
        }
        event.setFarPlaneDistance(FogBlend.far(event.getFarPlaneDistance(), event.getFarPlaneDistance() * FOG_FAR_FACTOR, amount));
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), event.getFarPlaneDistance()));
        event.setCanceled(true);
    }

    @Override
    public void fogColor(ViewportEvent.ComputeFogColor event, float strength) {
        if (!RingWeatherRenderer.clearView(event.getCamera()) || !(event.getCamera().getEntity().level() instanceof ClientLevel level)) {
            return;
        }
        float amount = strength * SkyExposure.get() * COLOUR_SHARE;
        float daylight = RingWeatherRenderer.daylight(level, (float) event.getPartialTick());
        event.setRed(FogBlend.colour(event.getRed(), WARM_RED * daylight, amount));
        event.setGreen(FogBlend.colour(event.getGreen(), WARM_GREEN * daylight, amount));
        event.setBlue(FogBlend.colour(event.getBlue(), WARM_BLUE * daylight, amount));
    }

    @Override
    public void renderLevel(RenderLevelStageEvent event, float strength) {
        // After the level, before the hand and the GUI: those stay sharp.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !RingWeatherRenderer.clearView(event.getCamera())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        float amount = strength * SkyExposure.get() * minecraft.options.screenEffectScale().get().floatValue();
        HeatHazeEffect.render(minecraft, amount, event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }
}
