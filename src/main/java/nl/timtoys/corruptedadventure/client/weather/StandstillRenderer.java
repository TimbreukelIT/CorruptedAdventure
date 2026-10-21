package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;
import nl.timtoys.corruptedadventure.weather.RingWeatherMix;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;

/**
 * Standstill (ring 5, package 92): rain streaks that hang still in mid-air (a {@link RainSheet} at a fixed time from
 * the weather's seed), clouds that slow to a halt ({@link StandstillClouds}), every world sound and the music drained
 * to ~15 % ({@link Ring5SoundDamping}; not the UI), and a slightly whiter, flat (colourless) fog. Streaks and fog ×
 * {@link SkyExposure}; the clouds, the sky and the silence follow the strength only (they reach indoors).
 */
final class StandstillRenderer implements RingWeatherRenderer {
    /** Pale grey streaks #C4C8D2, thinner than rain. */
    private static final int RAIN_COLOUR = 0xC4C8D2;
    private static final float RAIN_SHARE = 0.55F;
    /** Whitish grey #D6D7DB. */
    private static final float FOG_RED = 0xD6 / 255.0F;
    private static final float FOG_GREEN = 0xD7 / 255.0F;
    private static final float FOG_BLUE = 0xDB / 255.0F;
    private static final float MIN_LIGHT = 0.2F;
    private static final float FOG_SHARE = 0.35F;
    private static final float SKY_SHARE = 0.3F;
    /** Terrain fog distance factor of a full standstill in the open. */
    static final float FOG_FAR_FACTOR = 0.8F;
    /** Volume of the near-silent hum (its file is soft already). */
    private static final float MAX_VOLUME = 1.0F;
    /** The frozen time lies within the first this many ticks (float precision of the sheet's scroll). */
    private static final int FROZEN_TIME_RANGE = 4096;

    private final WeatherLoop hum = new WeatherLoop(ModSoundEvents.RING_5_STANDSTILL, MAX_VOLUME);
    private final RainSheet rain = new RainSheet(RAIN_COLOUR, 1.0F);
    private float frozenTime;

    @Override
    public void clientTick(ClientLevel level, LocalPlayer player, float strength) {
        StandstillClouds.feed(strength);
        Ring5SoundDamping.feedStandstill(strength);
        hum.play(Minecraft.getInstance(), level.getGameTime(), strength * SkyExposure.get());
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        RingWeatherClientHooks.felt(level, camera.x, camera.z, RingWeatherType.STANDSTILL)
                .map(RingWeatherMix.Share::state)
                .ifPresent(state -> frozenTime = frozenTime(state.seed()));
    }

    @Override
    public void renderFog(ViewportEvent.RenderFog event, float strength) {
        RingWeatherFog.terrainScaled(event, FOG_FAR_FACTOR, strength * SkyExposure.get());
    }

    @Override
    public void fogColor(ViewportEvent.ComputeFogColor event, float strength) {
        RingWeatherFog.colour(event, FOG_RED, FOG_GREEN, FOG_BLUE, MIN_LIGHT, strength * SkyExposure.get() * FOG_SHARE);
    }

    @Override
    public Vec3 skyColor(ClientLevel level, Vec3 colour, float partialTick, float strength) {
        return RingWeatherFog.sky(level, colour, partialTick, FOG_RED, FOG_GREEN, FOG_BLUE, MIN_LIGHT, strength * SKY_SHARE);
    }

    @Override
    public void renderLevel(RenderLevelStageEvent event, float strength) {
        rain.render(event, strength * SkyExposure.get() * RAIN_SHARE, Vec3.ZERO, frozenTime);
    }

    /** The streaks' frozen time for a standstill with {@code seed}: the same for its whole length, on every client. */
    static float frozenTime(int seed) {
        return Math.floorMod(seed, FROZEN_TIME_RANGE);
    }
}
