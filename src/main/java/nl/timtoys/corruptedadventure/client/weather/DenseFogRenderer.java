package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * Dense fog (ring 4, mythical band, package 70): soft white-grey fog down to {@link #FOG_FAR} blocks (terrain and sky,
 * so the islands loom out of it) and a muffled hum, scaled by strength × {@link SkyExposure}. The fog stays a little
 * light at night, like mist in moonlight. Visual values are constants on purpose, like the sandstorm.
 */
final class DenseFogRenderer implements RingWeatherRenderer {
    /** Far plane of full dense fog in the open (decision 2026-10-13: ~32 blocks). */
    static final float FOG_FAR = 32.0F;
    /** Mist white-grey #D9DFE4. */
    private static final float FOG_RED = 0xD9 / 255.0F;
    private static final float FOG_GREEN = 0xDF / 255.0F;
    private static final float FOG_BLUE = 0xE4 / 255.0F;
    /** The fog colour follows the daylight down to this share. */
    private static final float MIN_LIGHT = 0.15F;
    /** How far the sky moves towards the fog colour in full fog (the far plane hides it mostly anyway). */
    private static final float SKY_SHARE = 0.8F;
    /** Volume of the hum in full fog in the open; a background sound. */
    private static final float MAX_VOLUME = 0.3F;

    private final WeatherLoop hum = new WeatherLoop(ModSoundEvents.RING_4_DENSE_FOG, MAX_VOLUME);

    @Override
    public void clientTick(ClientLevel level, LocalPlayer player, float strength) {
        hum.play(Minecraft.getInstance(), level.getGameTime(), strength * SkyExposure.get());
    }

    @Override
    public void renderFog(ViewportEvent.RenderFog event, float strength) {
        RingWeatherFog.towards(event, FOG_FAR, strength * SkyExposure.get());
    }

    @Override
    public void fogColor(ViewportEvent.ComputeFogColor event, float strength) {
        RingWeatherFog.colour(event, FOG_RED, FOG_GREEN, FOG_BLUE, MIN_LIGHT, strength * SkyExposure.get());
    }

    @Override
    public Vec3 skyColor(ClientLevel level, Vec3 colour, float partialTick, float strength) {
        return RingWeatherFog.sky(level, colour, partialTick, FOG_RED, FOG_GREEN, FOG_BLUE, MIN_LIGHT, strength * SKY_SHARE);
    }
}
