package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * A light camera shake that dies away in about two seconds (ring 3 rumbles). Its owner calls {@link #tick} every
 * client tick and {@link #apply} on every camera update; vanilla's "Screen Effects" option scales it (0 = none).
 */
final class CameraShake {
    /** Shake left after one tick. */
    private static final float DECAY = 0.93F;
    /** Shake smaller than this is not applied, degrees. */
    private static final float NO_SHAKE = 0.01F;

    private float degrees;

    /** Starts a shake of {@code degrees} at most (a stronger one in progress is kept). */
    void kick(float degrees) {
        this.degrees = Math.max(this.degrees, degrees);
    }

    /** Every client tick: the shake dies away. */
    void tick() {
        degrees *= DECAY;
    }

    void clear() {
        degrees = 0.0F;
    }

    /** Adds the shake to the camera. */
    void apply(ViewportEvent.ComputeCameraAngles event) {
        float amount = degrees * Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        if (amount < NO_SHAKE) {
            return;
        }
        double time = event.getCamera().getEntity().tickCount + event.getPartialTick();
        event.setPitch(event.getPitch() + (float) Math.sin(time * 2.3) * amount);
        event.setRoll(event.getRoll() + (float) Math.sin(time * 1.7 + 1.3) * amount * 0.6F);
    }
}
