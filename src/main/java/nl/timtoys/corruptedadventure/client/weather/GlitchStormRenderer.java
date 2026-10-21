package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * Glitch storm (ring 5, mostly the inner band, package 92): image noise and hotbar hiccups on the screen
 * ({@link GlitchStormOverlay}), a stuttering hum whose weather and ambient sounds dip now and then
 * ({@link Ring5SoundDamping}), violet-black flashes in the fog colour, a closer dark violet haze and glitch specks in the
 * open air (the ring 4 {@code glitch_speck} until {@code glitch_pixel} comes in 98). Everything × {@link SkyExposure}
 * except the sky. Visual values are constants on purpose; the void factor is server-side (stream C).
 */
final class GlitchStormRenderer implements RingWeatherRenderer {
    /** Dark violet-grey haze #2A2233. */
    private static final float HAZE_RED = 0x2A / 255.0F;
    private static final float HAZE_GREEN = 0x22 / 255.0F;
    private static final float HAZE_BLUE = 0x33 / 255.0F;
    /** Violet-black of a flash #120818. */
    private static final float FLASH_RED = 0x12 / 255.0F;
    private static final float FLASH_GREEN = 0x08 / 255.0F;
    private static final float FLASH_BLUE = 0x18 / 255.0F;
    private static final float MIN_LIGHT = 0.25F;
    private static final float HAZE_SHARE = 0.55F;
    private static final float SKY_SHARE = 0.5F;
    /** Terrain fog distance factor of a full storm in the open. */
    static final float FOG_FAR_FACTOR = 0.6F;
    /** Volume of the hum in a full storm in the open. */
    private static final float MAX_VOLUME = 0.4F;
    /** Chance per tick of a flash, and of a sound dip, in a full storm. */
    private static final float FLASH_CHANCE = 0.03F;
    private static final float DIP_CHANCE = 0.05F;
    /** How much of the weather and ambient volume a dip takes away in a full storm. */
    private static final float DIP_DEPTH = 0.7F;
    /** Glitch specks per tick in a full storm in the open. */
    private static final float SPECKS = 1.2F;

    private final WeatherLoop hum = new WeatherLoop(ModSoundEvents.RING_5_GLITCH_STORM, MAX_VOLUME);
    private int flashTicks;
    private int dipTicks;

    @Override
    public void clientTick(ClientLevel level, LocalPlayer player, float strength) {
        Minecraft minecraft = Minecraft.getInstance();
        float open = strength * SkyExposure.get();
        RandomSource random = level.getRandom();
        hum.play(minecraft, level.getGameTime(), open);
        GlitchStormOverlay.feed(level.getGameTime(), open, random);
        flashTicks = flashTicks > 0 ? flashTicks - 1 : random.nextFloat() < FLASH_CHANCE * open ? 2 + random.nextInt(3) : 0;
        dipTicks = dipTicks > 0 ? dipTicks - 1 : random.nextFloat() < DIP_CHANCE * open ? 2 + random.nextInt(4) : 0;
        if (dipTicks > 0) {
            Ring5SoundDamping.feedDip(DIP_DEPTH * open);
        }
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        WeatherParticles.spawn(level, camera, ModParticleTypes.GLITCH_SPECK.get(),
                SPECKS * open, 14.0, 4.0, 10.0, Vec3.ZERO, 0.0);
    }

    @Override
    public void renderFog(ViewportEvent.RenderFog event, float strength) {
        RingWeatherFog.terrainScaled(event, FOG_FAR_FACTOR, strength * SkyExposure.get());
    }

    @Override
    public void fogColor(ViewportEvent.ComputeFogColor event, float strength) {
        float open = strength * SkyExposure.get();
        RingWeatherFog.colour(event, HAZE_RED, HAZE_GREEN, HAZE_BLUE, MIN_LIGHT, open * HAZE_SHARE);
        if (flashTicks > 0) {
            // A flash ignores the daylight: violet-black at noon too.
            RingWeatherFog.colour(event, FLASH_RED, FLASH_GREEN, FLASH_BLUE, 1.0F, open);
        }
    }

    @Override
    public Vec3 skyColor(ClientLevel level, Vec3 colour, float partialTick, float strength) {
        return RingWeatherFog.sky(level, colour, partialTick, HAZE_RED, HAZE_GREEN, HAZE_BLUE, MIN_LIGHT, strength * SKY_SHARE);
    }
}
