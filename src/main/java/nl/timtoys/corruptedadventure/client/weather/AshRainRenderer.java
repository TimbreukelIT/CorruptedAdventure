package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * Ash rain (ring 3): grey fog down to {@link #FOG_FAR} blocks (terrain and sky, so the sky turns ash-grey), slowly
 * falling {@code ash_flake}s and a soft crackling loop, all scaled by strength × {@link SkyExposure}. Visual values are
 * constants on purpose, like the sandstorm.
 */
final class AshRainRenderer implements RingWeatherRenderer {
    /** Far plane of full ash rain in the open. */
    static final float FOG_FAR = 48.0F;
    /** Ash grey #8A8580. */
    private static final float ASH_RED = 0x8A / 255.0F;
    private static final float ASH_GREEN = 0x85 / 255.0F;
    private static final float ASH_BLUE = 0x80 / 255.0F;
    /** Flakes per tick in full ash rain in the open (before vanilla's "Particles" setting). */
    private static final float FLAKES_PER_TICK = 30.0F;
    /** Falling speed of a flake, blocks per tick. */
    private static final Vec3 FALL = new Vec3(0.0, -0.06, 0.0);
    /** Volume of the crackle loop in full ash rain in the open; a background sound (teststop 4c: ambience softer). */
    private static final float MAX_VOLUME = 0.35F;

    private final WeatherLoop crackle = new WeatherLoop(ModSoundEvents.RING_3_ASH_RAIN, MAX_VOLUME);

    @Override
    public void clientTick(ClientLevel level, LocalPlayer player, float strength) {
        Minecraft minecraft = Minecraft.getInstance();
        float amount = strength * SkyExposure.get();
        crackle.play(minecraft, level.getGameTime(), amount);
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        WeatherParticles.spawn(level, camera, ModParticleTypes.ASH_FLAKE.get(), FLAKES_PER_TICK * amount,
                20.0, 4.0, 16.0, FALL, 0.01);
    }

    @Override
    public void renderFog(ViewportEvent.RenderFog event, float strength) {
        RingWeatherFog.towards(event, FOG_FAR, strength * SkyExposure.get());
    }

    @Override
    public void fogColor(ViewportEvent.ComputeFogColor event, float strength) {
        RingWeatherFog.colour(event, ASH_RED, ASH_GREEN, ASH_BLUE, 0.2F, strength * SkyExposure.get());
    }
}
