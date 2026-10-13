package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * Toxic rain (ring 4, blighted band, package 70): straight poison-green rain streaks, a sickly green haze on the
 * terrain, a slightly green sky and a rain loop. Haze, rain and sound follow {@link SkyExposure} (and the streaks
 * stop at every roof by themselves: they start at the top of each column); the sky follows the strength only. Visual values are constants on purpose. The poison it
 * causes is server-side (stream C, {@code toxicRainPoisonMultiplier}).
 */
final class ToxicRainRenderer implements RingWeatherRenderer {
    /** Poison green #7FD13B. */
    private static final int RAIN_COLOUR = 0x7FD13B;
    /** Sickly green haze #6F8A3C. */
    private static final float HAZE_RED = 0x6F / 255.0F;
    private static final float HAZE_GREEN = 0x8A / 255.0F;
    private static final float HAZE_BLUE = 0x3C / 255.0F;
    private static final float MIN_LIGHT = 0.2F;
    private static final float HAZE_SHARE = 0.6F;
    private static final float SKY_SHARE = 0.35F;
    /** Terrain fog distance factor of full toxic rain in the open. */
    static final float FOG_FAR_FACTOR = 0.7F;
    /** Volume of the rain loop in full toxic rain in the open. */
    private static final float MAX_VOLUME = 0.4F;

    private final WeatherLoop patter = new WeatherLoop(ModSoundEvents.RING_4_TOXIC_RAIN, MAX_VOLUME);
    private final RainSheet rain = new RainSheet(RAIN_COLOUR, 1.0F);

    @Override
    public void clientTick(ClientLevel level, LocalPlayer player, float strength) {
        patter.play(Minecraft.getInstance(), level.getGameTime(), strength * SkyExposure.get());
    }

    @Override
    public void renderFog(ViewportEvent.RenderFog event, float strength) {
        RingWeatherFog.terrainScaled(event, FOG_FAR_FACTOR, strength * SkyExposure.get());
    }

    @Override
    public void fogColor(ViewportEvent.ComputeFogColor event, float strength) {
        RingWeatherFog.colour(event, HAZE_RED, HAZE_GREEN, HAZE_BLUE, MIN_LIGHT, strength * SkyExposure.get() * HAZE_SHARE);
    }

    @Override
    public Vec3 skyColor(ClientLevel level, Vec3 colour, float partialTick, float strength) {
        return RingWeatherFog.sky(level, colour, partialTick, HAZE_RED, HAZE_GREEN, HAZE_BLUE, MIN_LIGHT, strength * SKY_SHARE);
    }

    @Override
    public void renderLevel(RenderLevelStageEvent event, float strength) {
        rain.render(event, strength * SkyExposure.get(), Vec3.ZERO);
    }
}
