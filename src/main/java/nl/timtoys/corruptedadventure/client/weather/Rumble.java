package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.function.Supplier;

/**
 * Distant volcanic rumble (ring 3): a one-shot at random intervals, each followed by a light camera shake that dies
 * away in about two seconds. Per client and not synced: the rumble and the shake may differ between players.
 */
final class Rumble {
    /** Shake left after one tick. */
    private static final float DECAY = 0.93F;
    /** Shake smaller than this is not applied. */
    private static final float NO_SHAKE = 0.01F;
    /** Volume and shake of a rumble at the very start of the weather; full at full strength. */
    private static final float MIN_LOUDNESS = 0.5F;
    /** A pause longer than this in the ticks means the weather was not felt in between. */
    private static final long FELT_AGAIN_TICKS = 20L;

    private final Supplier<SoundEvent> event;
    private final int minInterval;
    private final int maxInterval;
    private final float maxDegrees;
    private int untilNext = -1;
    private float shake;
    private long lastTick = Long.MIN_VALUE / 2;

    /**
     * @param minInterval shortest time between two rumbles, ticks
     * @param maxInterval longest time between two rumbles, ticks
     * @param maxDegrees  camera shake of a full-volume rumble, degrees
     */
    Rumble(Supplier<SoundEvent> event, int minInterval, int maxInterval, float maxDegrees) {
        this.event = event;
        this.minInterval = minInterval;
        this.maxInterval = maxInterval;
        this.maxDegrees = maxDegrees;
    }

    /** Every client tick while the weather is felt; {@code volume} 0–1 also sets the strength of the shake. */
    void tick(Minecraft minecraft, RandomSource random, long gameTime, float volume) {
        if (gameTime - lastTick > FELT_AGAIN_TICKS || gameTime < lastTick) {
            // Felt again after a while (a new weather, back outside the zone): start over.
            untilNext = -1;
            shake = 0.0F;
        }
        lastTick = gameTime;
        shake *= DECAY;
        if (untilNext < 0) {
            // The first rumble of a weather comes soon: it is the warning.
            untilNext = random.nextInt(minInterval / 2 + 1);
        }
        if (untilNext-- > 0 || volume <= 0.0F) {
            return;
        }
        untilNext = minInterval + random.nextInt(maxInterval - minInterval + 1);
        // A rumble is loud from the start, so the first ones (the warning, early in the fade-in) are heard.
        float loudness = MIN_LOUDNESS + (1.0F - MIN_LOUDNESS) * volume;
        shake = Math.max(shake, loudness);
        minecraft.getSoundManager().play(new SimpleSoundInstance(event.get().getLocation(), SoundSource.WEATHER, loudness,
                1.0F, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true));
    }

    /** Adds the shake to the camera (× vanilla's "Screen Effects" option, 0 = none). */
    void shakeCamera(ViewportEvent.ComputeCameraAngles event) {
        float amount = shake * Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        if (amount < NO_SHAKE) {
            return;
        }
        double time = event.getCamera().getEntity().tickCount + event.getPartialTick();
        float degrees = amount * maxDegrees;
        event.setPitch(event.getPitch() + (float) Math.sin(time * 2.3) * degrees);
        event.setRoll(event.getRoll() + (float) Math.sin(time * 1.7 + 1.3) * degrees * 0.6F);
    }
}
