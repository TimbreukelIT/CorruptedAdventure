package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * Reverse rain (ring 5, mostly the outer band, package 92): black-violet streaks that rise from the ground into the sky
 * ({@link RainSheet} with a negative speed; also in biomes without precipitation, and per column never under a roof), a
 * darker sky, a slightly closer haze, a low, wrong rain and, since 98, black drops shooting up around the camera
 * ({@code reverse_raindrop}, only in the open air). Rain, haze and sound × {@link SkyExposure}; the sky follows
 * the strength only. Visual values are constants on purpose; the void factor is server-side (stream C).
 */
final class ReverseRainRenderer implements RingWeatherRenderer {
    /** Black-violet #1A1022 (plan 92). */
    static final int RAIN_COLOUR = 0x1A1022;
    /** Rises a little slower than vanilla rain falls. */
    static final float SPEED = -0.8F;
    /** Dark violet haze #2B1E38. */
    private static final float HAZE_RED = 0x2B / 255.0F;
    private static final float HAZE_GREEN = 0x1E / 255.0F;
    private static final float HAZE_BLUE = 0x38 / 255.0F;
    private static final float MIN_LIGHT = 0.2F;
    private static final float HAZE_SHARE = 0.45F;
    private static final float SKY_SHARE = 0.45F;
    /** Terrain fog distance factor of full reverse rain in the open. */
    static final float FOG_FAR_FACTOR = 0.75F;
    /** Volume of the rain loop in full reverse rain in the open. */
    private static final float MAX_VOLUME = 0.4F;
    /** Reverse raindrops per tick in full reverse rain in the open (98). */
    private static final float DROPS = 3.0F;

    private final WeatherLoop patter = new WeatherLoop(ModSoundEvents.RING_5_REVERSE_RAIN, MAX_VOLUME);
    private final RainSheet rain = new RainSheet(RAIN_COLOUR, SPEED);

    @Override
    public void clientTick(ClientLevel level, LocalPlayer player, float strength) {
        Minecraft minecraft = Minecraft.getInstance();
        float open = strength * SkyExposure.get();
        patter.play(minecraft, level.getGameTime(), open);
        // Mostly below the eyes, so they are seen rising past the camera.
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        WeatherParticles.spawn(level, camera, ModParticleTypes.REVERSE_RAINDROP.get(), DROPS * open, 10.0, 6.0, 2.0, Vec3.ZERO, 0.0);
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
