package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

/**
 * One renderer's weather loop: starts a {@link WeatherLoopSound} when needed (again right after it faded out and
 * stopped, or every {@link #RETRY_TICKS} if the sound engine had no free channel) and feeds it the wanted volume each
 * tick.
 */
final class WeatherLoop {
    /** Ticks between two attempts to start the loop. */
    private static final long RETRY_TICKS = 100L;

    private final Supplier<SoundEvent> event;
    private final float maxVolume;
    private WeatherLoopSound sound;
    private long lastAttempt = -RETRY_TICKS;

    WeatherLoop(Supplier<SoundEvent> event, float maxVolume) {
        this.event = event;
        this.maxVolume = maxVolume;
    }

    /** Called every client tick while the weather is felt; {@code amount} 0–1 (strength × exposure). */
    void play(Minecraft minecraft, long gameTime, float amount) {
        if (sound == null || !minecraft.getSoundManager().isActive(sound)) {
            // Only a start that never played waits: a loop that faded out and stopped may start again at once.
            boolean failed = sound != null && !sound.isStopped();
            if (amount <= 0.0F || failed && gameTime - lastAttempt < RETRY_TICKS && gameTime >= lastAttempt) {
                return;
            }
            lastAttempt = gameTime;
            sound = new WeatherLoopSound(event.get());
            minecraft.getSoundManager().play(sound);
        }
        sound.feed(amount * maxVolume);
    }
}
