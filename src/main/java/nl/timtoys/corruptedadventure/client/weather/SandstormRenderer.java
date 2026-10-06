package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;
import nl.timtoys.corruptedadventure.weather.RingWeatherMix;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import nl.timtoys.corruptedadventure.weather.SandstormWind;

import java.util.Locale;
import java.util.Optional;

/**
 * Sandstorm (ring 2): sand-coloured fog down to {@link #FOG_FAR} blocks, blown sand particles and a wind loop, all
 * scaled by strength × {@link SkyExposure}. The wind direction comes from the synced weather ({@link SandstormWind}),
 * so every client blows the same way. Visual values are constants on purpose: a client config would let a player clear
 * the storm away.
 */
final class SandstormRenderer implements RingWeatherRenderer {
    /** Far plane of a full storm in the open (decision 2026-10-01: ~24 blocks). */
    static final float FOG_FAR = 24.0F;
    /** Sand colour #C9A46B. */
    private static final float SAND_RED = 0xC9 / 255.0F;
    private static final float SAND_GREEN = 0xA4 / 255.0F;
    private static final float SAND_BLUE = 0x6B / 255.0F;
    /** Particles per tick in a full storm in the open (before vanilla's "Particles" setting). */
    private static final float PARTICLES_PER_TICK = 40.0F;
    /** Half the width of the box around the camera where particles start. */
    private static final double SPAWN_RADIUS = 16.0;
    /** Particle speed along the wind, blocks per tick. */
    private static final double WIND_SPEED = 0.6;
    /** Volume of the wind loop in a full storm in the open. */
    private static final float MAX_VOLUME = 0.6F;

    private final WeatherLoop wind = new WeatherLoop(ModSoundEvents.RING_2_SANDSTORM, MAX_VOLUME);

    @Override
    public void clientTick(ClientLevel level, LocalPlayer player, float strength) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Optional<RingWeatherMix.Share> share = RingWeatherClientHooks.felt(level, camera.x, camera.z, RingWeatherType.SANDSTORM);
        if (share.isEmpty()) {
            return;
        }
        float amount = strength * SkyExposure.get();
        wind.play(minecraft, level.getGameTime(), amount);
        spawnSand(level, camera, SandstormWind.direction(share.get().state()), amount);
    }

    @Override
    public void renderFog(ViewportEvent.RenderFog event, float strength) {
        if (!RingWeatherRenderer.clearView(event.getCamera())) {
            return;
        }
        float amount = strength * SkyExposure.get();
        if (amount <= 0.0F) {
            return;
        }
        // Terrain and sky alike, so the sky itself disappears into the sand. Applied only when cancelled.
        float far = FogBlend.far(event.getFarPlaneDistance(), FOG_FAR, amount);
        event.setNearPlaneDistance(FogBlend.near(event.getNearPlaneDistance(), far, amount));
        event.setFarPlaneDistance(far);
        event.setCanceled(true);
    }

    @Override
    public void fogColor(ViewportEvent.ComputeFogColor event, float strength) {
        if (!RingWeatherRenderer.clearView(event.getCamera()) || !(event.getCamera().getEntity().level() instanceof ClientLevel level)) {
            return;
        }
        float amount = strength * SkyExposure.get();
        float daylight = RingWeatherRenderer.daylight(level, (float) event.getPartialTick());
        event.setRed(FogBlend.colour(event.getRed(), SAND_RED * daylight, amount));
        event.setGreen(FogBlend.colour(event.getGreen(), SAND_GREEN * daylight, amount));
        event.setBlue(FogBlend.colour(event.getBlue(), SAND_BLUE * daylight, amount));
    }

    /** F3: {@code 225° (from SW), strength 0.83, exposure 0.85}, or {@code —} without a sandstorm here. */
    static String describeWind(Level level, double x, double z) {
        return RingWeatherClientHooks.felt(level, x, z, RingWeatherType.SANDSTORM)
                .map(share -> String.format(Locale.ROOT, "%s, strength %.2f, exposure %.2f",
                        SandstormWind.describe(share.state()), share.strength(), SkyExposure.get()))
                .orElse("—");
    }

    /**
     * Random spots in a box around the camera, shifted upwind so the sand blows past; only above the
     * {@code MOTION_BLOCKING} heightmap (never in houses or caves). {@code addParticle} honours the vanilla
     * "Particles" setting.
     */
    private static void spawnSand(ClientLevel level, Vec3 camera, Vec3 wind, float amount) {
        RandomSource random = level.getRandom();
        float expected = PARTICLES_PER_TICK * amount;
        int count = (int) expected + (random.nextFloat() < expected - (int) expected ? 1 : 0);
        double centreX = camera.x - wind.x * SPAWN_RADIUS * 0.5;
        double centreZ = camera.z - wind.z * SPAWN_RADIUS * 0.5;
        for (int i = 0; i < count; i++) {
            double x = centreX + (random.nextDouble() * 2.0 - 1.0) * SPAWN_RADIUS;
            double z = centreZ + (random.nextDouble() * 2.0 - 1.0) * SPAWN_RADIUS;
            double y = camera.y + random.nextDouble() * 12.0 - 5.0;
            if (y < level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z))) {
                continue;
            }
            double speed = WIND_SPEED * (0.75 + random.nextDouble() * 0.5);
            level.addParticle(ModParticleTypes.SAND_GUST.get(), x, y, z,
                    wind.x * speed + random.nextGaussian() * 0.03,
                    random.nextGaussian() * 0.02,
                    wind.z * speed + random.nextGaussian() * 0.03);
        }
    }
}
