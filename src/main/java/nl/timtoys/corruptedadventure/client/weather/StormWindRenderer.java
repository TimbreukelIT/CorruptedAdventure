package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;
import nl.timtoys.corruptedadventure.weather.RingWeatherMix;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import nl.timtoys.corruptedadventure.weather.SandstormWind;

import java.util.Locale;
import java.util.Optional;

/**
 * Storm wind (ring 4, mythical band, package 70): a grey sky, a darker haze on the terrain, rain slanting with the wind,
 * foam blowing off the water and a howling loop. The wind direction comes from the synced weather (the same function
 * as the server's push, {@code weather.StormWind}), so every client sees the same storm. Haze, rain, foam and wind
 * follow {@link SkyExposure}; the sky follows the strength only. Visual values are constants on purpose.
 */
final class StormWindRenderer implements RingWeatherRenderer {
    /** Storm grey #5F6A73. */
    private static final float STORM_RED = 0x5F / 255.0F;
    private static final float STORM_GREEN = 0x6A / 255.0F;
    private static final float STORM_BLUE = 0x73 / 255.0F;
    private static final float MIN_LIGHT = 0.2F;
    private static final float SKY_SHARE = 0.85F;
    private static final float HAZE_SHARE = 0.7F;
    /** Terrain fog distance factor of a full storm in the open. */
    static final float FOG_FAR_FACTOR = 0.6F;
    /** Pale blue-white rain. */
    private static final int RAIN_COLOUR = 0xC8D4E0;
    /** Horizontal blocks per block of fall in a full storm: the rain slants clearly with the wind. */
    static final float SLANT = 0.6F;
    /** Water further than this above or below the camera gets no foam (it would not be seen). */
    private static final int FOAM_MAX_HEIGHT = 32;
    /** Foam particles per tick on open water in a full storm (before vanilla's "Particles" setting). */
    private static final float FOAM_PER_TICK = 12.0F;
    private static final double FOAM_RADIUS = 16.0;
    private static final double FOAM_SPEED = 0.25;
    /** Volume of the wind loop in a full storm in the open. */
    private static final float MAX_VOLUME = 0.5F;

    private final WeatherLoop wind = new WeatherLoop(ModSoundEvents.RING_4_STORM_WIND, MAX_VOLUME);
    private final RainSheet rain = new RainSheet(RAIN_COLOUR, 1.4F);

    @Override
    public void clientTick(ClientLevel level, LocalPlayer player, float strength) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Optional<RingWeatherMix.Share> share = RingWeatherClientHooks.felt(level, camera.x, camera.z, RingWeatherType.STORM_WIND);
        if (share.isEmpty()) {
            return;
        }
        float amount = strength * SkyExposure.get();
        wind.play(minecraft, level.getGameTime(), amount);
        spawnFoam(level, camera, SandstormWind.direction(share.get().state()), amount);
    }

    @Override
    public void renderFog(ViewportEvent.RenderFog event, float strength) {
        RingWeatherFog.terrainScaled(event, FOG_FAR_FACTOR, strength * SkyExposure.get());
    }

    @Override
    public void fogColor(ViewportEvent.ComputeFogColor event, float strength) {
        RingWeatherFog.colour(event, STORM_RED, STORM_GREEN, STORM_BLUE, MIN_LIGHT, strength * SkyExposure.get() * HAZE_SHARE);
    }

    @Override
    public Vec3 skyColor(ClientLevel level, Vec3 colour, float partialTick, float strength) {
        return RingWeatherFog.sky(level, colour, partialTick, STORM_RED, STORM_GREEN, STORM_BLUE, MIN_LIGHT, strength * SKY_SHARE);
    }

    @Override
    public void renderLevel(RenderLevelStageEvent event, float strength) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        Level level = event.getCamera().getEntity().level();
        RingWeatherClientHooks.felt(level, camera.x, camera.z, RingWeatherType.STORM_WIND)
                .ifPresent(share -> rain.render(event, strength * SkyExposure.get(), slant(share.state(), strength)));
    }

    /** The slant of the rain: along the wind, steeper as the storm fades. */
    static Vec3 slant(RingWeatherState state, float strength) {
        return SandstormWind.direction(state).scale(SLANT * Math.clamp(strength, 0.0F, 1.0F));
    }

    /** F3: {@code 225° (from SW), strength 0.83, exposure 0.85}, or empty without a storm here. */
    static Optional<String> describeWind(Level level, double x, double z) {
        return RingWeatherClientHooks.felt(level, x, z, RingWeatherType.STORM_WIND)
                .map(share -> String.format(Locale.ROOT, "storm %s, strength %.2f, exposure %.2f",
                        SandstormWind.describe(share.state()), share.strength(), SkyExposure.get()));
    }

    /** Spray blown off open water around the camera (only where water is the top of the column). */
    private static void spawnFoam(ClientLevel level, Vec3 camera, Vec3 wind, float amount) {
        RandomSource random = level.getRandom();
        float expected = FOAM_PER_TICK * amount;
        int count = (int) expected + (random.nextFloat() < expected - (int) expected ? 1 : 0);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            int x = (int) Math.floor(camera.x + (random.nextDouble() * 2.0 - 1.0) * FOAM_RADIUS);
            int z = (int) Math.floor(camera.z + (random.nextDouble() * 2.0 - 1.0) * FOAM_RADIUS);
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (Math.abs(surface - camera.y) > FOAM_MAX_HEIGHT || !level.getFluidState(pos.set(x, surface - 1, z)).is(FluidTags.WATER)) {
                continue;
            }
            double speed = FOAM_SPEED * (0.5 + random.nextDouble());
            level.addParticle(ParticleTypes.SPLASH, x + random.nextDouble(), surface + 0.05, z + random.nextDouble(),
                    wind.x * speed, 0.1 + random.nextDouble() * 0.1, wind.z * speed);
        }
    }
}
