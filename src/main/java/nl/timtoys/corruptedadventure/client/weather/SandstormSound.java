package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * The sandstorm's wind loop around the listener (weather volume slider). {@link SandstormRenderer} feeds the wanted
 * volume every tick while the storm is felt; without a feed the volume glides to 0 and the loop stops, so leaving the
 * storm (indoors, out of the ring, end of the weather) never cuts it off.
 */
final class SandstormSound extends AbstractTickableSoundInstance {
    /** Largest volume change per tick (one second from silent to full). */
    private static final float STEP_PER_TICK = 1.0F / 20.0F;

    private float wanted;

    SandstormSound() {
        super(ModSoundEvents.RING_2_SANDSTORM.get(), SoundSource.WEATHER, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
    }

    /** The volume wanted this tick; the highest feed of a tick wins. */
    void feed(float volume) {
        wanted = Math.max(wanted, volume);
    }

    @Override
    public void tick() {
        volume = SkyExposure.approach(volume, wanted, STEP_PER_TICK);
        boolean fed = wanted > 0.0F;
        wanted = 0.0F;
        if (!fed && volume <= 0.0F) {
            stop();
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
