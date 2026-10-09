package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.client.ClientRingLayout;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherMix;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;

/**
 * Ring 3 ambience outside the weather (package 50, G2-A4 = A), client only and per player (not synced, by design):
 * <ul>
 *   <li>a deep rumble of the volcanoes every 2–5 minutes, one in three with a light camera shake of 0.35°; on the peaks
 *       ({@code volcanic_peaks}) every 1–2.5 minutes, two in three shaking with 0.6°. One timer counts down all the time
 *       in ring 3, whatever the biome or weather, so rugged terrain does not keep restarting it; the biome at the moment
 *       of the rumble picks its strength and the next pause. While an eruption or an ash cloud is felt the rumble is
 *       skipped: they rumble by themselves. Indoors and underground it is softer and shakes less, down to no shake
 *       without sky ({@link SkyExposure}). The shake follows vanilla's "Screen Effects";</li>
 *   <li>sparks drifting up on the peaks next to the biome's falling ash (a biome has one ambient particle only).</li>
 * </ul>
 * The client knows the biome, not the volcano positions, so "near a volcano" is "on the peaks".
 */
public final class Ring3Ambience {
    /** Pause between two rumbles on the plains and on the peaks, ticks. */
    static final int PLAINS_MIN = 2400;
    static final int PLAINS_MAX = 6000;
    static final int PEAKS_MIN = 1200;
    static final int PEAKS_MAX = 3000;
    /** Share of the rumbles that shake, and their shake in the open, degrees. */
    static final float PLAINS_SHAKE_CHANCE = 1.0F / 3.0F;
    static final float PEAKS_SHAKE_CHANCE = 2.0F / 3.0F;
    static final float PLAINS_SHAKE_DEGREES = 0.35F;
    static final float PEAKS_SHAKE_DEGREES = 0.6F;
    /** Volume of a rumble in the open: softer than an eruption's on the plains. */
    private static final float PLAINS_VOLUME = 0.65F;
    private static final float PEAKS_VOLUME = 0.9F;
    /** Volume without any sky, as a share of the volume in the open. */
    private static final float INDOOR_VOLUME = 0.5F;
    /** Sparks per tick on the peaks, around the camera. */
    private static final float PEAK_SPARKS = 0.6F;

    private static final CameraShake SHAKE = new CameraShake();
    /** Ticks until the next rumble; negative = not drawn yet. Pauses outside ring 3. */
    private static int untilNext = -1;

    private Ring3Ambience() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(Ring3Ambience::onClientTick);
        gameBus.addListener(Ring3Ambience::onComputeCameraAngles);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        SHAKE.tick();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        if (level.dimension() != Level.OVERWORLD || !ClientRingLayout.ringAt(level.dimension(), camera.x, camera.z)
                .map(position -> position.ring() == Ring.RING_3).orElse(false)) {
            return;
        }
        RandomSource random = level.getRandom();
        boolean peaks = level.getBiome(BlockPos.containing(camera)).is(ModBiomes.VOLCANIC_PEAKS);
        if (peaks) {
            WeatherParticles.spawn(level, camera, ModParticleTypes.EMBER.get(), PEAK_SPARKS, 14.0, 4.0, 10.0,
                    new Vec3(0.0, 0.04, 0.0), 0.015);
        }
        if (untilNext < 0) {
            untilNext = pause(random, peaks);
        }
        if (untilNext-- > 0) {
            return;
        }
        untilNext = pause(random, peaks);
        if (stormRumbles(level, camera)) {
            return;
        }
        float exposure = SkyExposure.get();
        float volume = (peaks ? PEAKS_VOLUME : PLAINS_VOLUME) * (INDOOR_VOLUME + (1.0F - INDOOR_VOLUME) * exposure);
        minecraft.getSoundManager().play(new SimpleSoundInstance(ModSoundEvents.RING_3_RUMBLE.get().getLocation(),
                SoundSource.AMBIENT, volume, 1.0F, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
        if (random.nextFloat() < (peaks ? PEAKS_SHAKE_CHANCE : PLAINS_SHAKE_CHANCE)) {
            SHAKE.kick((peaks ? PEAKS_SHAKE_DEGREES : PLAINS_SHAKE_DEGREES) * exposure);
        }
    }

    private static int pause(RandomSource random, boolean peaks) {
        return peaks ? PEAKS_MIN + random.nextInt(PEAKS_MAX - PEAKS_MIN + 1) : PLAINS_MIN + random.nextInt(PLAINS_MAX - PLAINS_MIN + 1);
    }

    /** An eruption or ash cloud felt here rumbles by itself. */
    private static boolean stormRumbles(ClientLevel level, Vec3 camera) {
        for (RingWeatherMix.Share share : RingWeatherClientHooks.shares(level, camera.x, camera.z)) {
            RingWeatherType type = share.state().type();
            if (share.strength() > 0.0F && (type == RingWeatherType.ERUPTION || type == RingWeatherType.ASH_CLOUD)) {
                return true;
            }
        }
        return false;
    }

    private static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        SHAKE.apply(event);
    }
}
