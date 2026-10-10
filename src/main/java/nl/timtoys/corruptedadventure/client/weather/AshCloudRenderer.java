package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * Ash cloud (ring 3, pyroclastic): near-black fog down to {@link #FOG_FAR} blocks, a dark sky, whirling embers and ash,
 * a roaring loop and rumbles. Its fade-in is short (20 s, {@code RingWeatherType#fadeTicks}): those first seconds of
 * rumble and darkening are the warning to find shelter. Fog, particles and the roar follow {@link SkyExposure}, so a
 * roof really helps; the sky follows the strength only.
 */
final class AshCloudRenderer implements RingWeatherRenderer {
    /** Far plane of a full ash cloud in the open. */
    static final float FOG_FAR = 16.0F;
    /** Near-black ash #241F1C. */
    private static final float CLOUD_RED = 0x24 / 255.0F;
    private static final float CLOUD_GREEN = 0x1F / 255.0F;
    private static final float CLOUD_BLUE = 0x1C / 255.0F;
    /** The cloud colour follows the daylight down to this share: near-black by day, darker still at night. */
    private static final float MIN_LIGHT = 0.3F;
    private static final float SKY_SHARE = 0.95F;
    private static final float EMBERS_PER_TICK = 10.0F;
    private static final float FLAKES_PER_TICK = 25.0F;
    /** Embers and ash whirl rather than fall. */
    private static final Vec3 WHIRL = new Vec3(0.0, -0.01, 0.0);
    /** Volume of the roar loop in a full ash cloud in the open; a background sound (teststop 4c: ambience softer). */
    private static final float MAX_VOLUME = 0.5F;
    private static final float INDOOR_RUMBLE = 0.6F;

    private final WeatherLoop roar = new WeatherLoop(ModSoundEvents.RING_3_ASH_CLOUD, MAX_VOLUME);
    private final Rumble rumble = new Rumble(ModSoundEvents.RING_3_ERUPTION, 80, 200, 0.6F);

    @Override
    public void clientTick(ClientLevel level, LocalPlayer player, float strength) {
        Minecraft minecraft = Minecraft.getInstance();
        float exposure = SkyExposure.get();
        float amount = strength * exposure;
        roar.play(minecraft, level.getGameTime(), amount);
        rumble.tick(minecraft, level.getRandom(), level.getGameTime(),
                strength * (INDOOR_RUMBLE + (1.0F - INDOOR_RUMBLE) * exposure));
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        WeatherParticles.spawn(level, camera, ModParticleTypes.EMBER.get(), EMBERS_PER_TICK * amount,
                12.0, 4.0, 8.0, WHIRL, 0.06);
        WeatherParticles.spawn(level, camera, ModParticleTypes.ASH_FLAKE.get(), FLAKES_PER_TICK * amount,
                12.0, 4.0, 10.0, WHIRL, 0.05);
    }

    @Override
    public Vec3 skyColor(ClientLevel level, Vec3 colour, float partialTick, float strength) {
        return RingWeatherFog.sky(level, colour, partialTick, CLOUD_RED, CLOUD_GREEN, CLOUD_BLUE, MIN_LIGHT, strength * SKY_SHARE);
    }

    @Override
    public void renderFog(ViewportEvent.RenderFog event, float strength) {
        RingWeatherFog.towards(event, FOG_FAR, strength * SkyExposure.get());
    }

    @Override
    public void fogColor(ViewportEvent.ComputeFogColor event, float strength) {
        RingWeatherFog.colour(event, CLOUD_RED, CLOUD_GREEN, CLOUD_BLUE, MIN_LIGHT, strength * SkyExposure.get());
    }

    @Override
    public void cameraAngles(ViewportEvent.ComputeCameraAngles event, float strength) {
        rumble.shakeCamera(event);
    }
}
