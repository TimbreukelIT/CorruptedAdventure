package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * Eruption (ring 3): a red sky that keeps glowing at night, red-brown haze on the terrain, falling embers, and a deep
 * rumble now and then with a light camera shake. The lava bombs are server entities ({@code weather.LavaBombs}), not
 * drawn here. The sky follows the strength only (it is red through a window too); haze and embers also follow
 * {@link SkyExposure}; the rumble is heard indoors, softer.
 */
final class EruptionRenderer implements RingWeatherRenderer {
    /** Volcanic red #B3361A. */
    private static final float SKY_RED = 0xB3 / 255.0F;
    private static final float SKY_GREEN = 0x36 / 255.0F;
    private static final float SKY_BLUE = 0x1A / 255.0F;
    /** How far the sky moves towards the red in a full eruption. */
    private static final float SKY_SHARE = 0.85F;
    /** The glow of the sky never drops below this at night. */
    private static final float NIGHT_GLOW = 0.45F;
    /** Dark red-brown haze #7A2A14. */
    private static final float HAZE_RED = 0x7A / 255.0F;
    private static final float HAZE_GREEN = 0x2A / 255.0F;
    private static final float HAZE_BLUE = 0x14 / 255.0F;
    private static final float HAZE_SHARE = 0.7F;
    /** Terrain fog distance factor of a full eruption in the open. */
    static final float FOG_FAR_FACTOR = 0.6F;
    private static final float EMBERS_PER_TICK = 8.0F;
    private static final Vec3 EMBER_FALL = new Vec3(0.0, -0.04, 0.0);
    /** Rumble volume indoors, as a share of the volume outside. */
    private static final float INDOOR_RUMBLE = 0.6F;

    private final Rumble rumble = new Rumble(ModSoundEvents.RING_3_ERUPTION, 100, 300, 0.8F);

    @Override
    public void clientTick(ClientLevel level, LocalPlayer player, float strength) {
        Minecraft minecraft = Minecraft.getInstance();
        float exposure = SkyExposure.get();
        rumble.tick(minecraft, level.getRandom(), level.getGameTime(),
                strength * (INDOOR_RUMBLE + (1.0F - INDOOR_RUMBLE) * exposure));
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        WeatherParticles.spawn(level, camera, ModParticleTypes.EMBER.get(), EMBERS_PER_TICK * strength * exposure,
                20.0, 2.0, 16.0, EMBER_FALL, 0.02);
    }

    @Override
    public Vec3 skyColor(ClientLevel level, Vec3 colour, float partialTick, float strength) {
        return RingWeatherFog.sky(level, colour, partialTick, SKY_RED, SKY_GREEN, SKY_BLUE, NIGHT_GLOW, strength * SKY_SHARE);
    }

    @Override
    public void renderFog(ViewportEvent.RenderFog event, float strength) {
        RingWeatherFog.terrainScaled(event, FOG_FAR_FACTOR, strength * SkyExposure.get());
    }

    @Override
    public void fogColor(ViewportEvent.ComputeFogColor event, float strength) {
        RingWeatherFog.colour(event, HAZE_RED, HAZE_GREEN, HAZE_BLUE, NIGHT_GLOW, strength * SkyExposure.get() * HAZE_SHARE);
    }

    @Override
    public void cameraAngles(ViewportEvent.ComputeCameraAngles event, float strength) {
        rumble.shakeCamera(event);
    }
}
