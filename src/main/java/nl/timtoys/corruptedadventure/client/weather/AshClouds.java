package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.client.ClientRingLayout;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherMix;

/**
 * The clouds of ring 3 are dark with ash (package 50, decision 39f): vanilla's cloud colour (day/night, rain) times a
 * dark ash tint, by ring 3's share at the camera, so the clouds darken smoothly across the zones 2→3 and 3→4 like the
 * sky does. Called by {@code ClientLevelSkyColorMixin} on {@code ClientLevel#getCloudColor} (render thread).
 */
public final class AshClouds {
    /** Multiplier on vanilla's colour inside ring 3: dark warm grey (white clouds at noon become #5C5652). */
    private static final double RED = 0x5C / 255.0;
    private static final double GREEN = 0x56 / 255.0;
    private static final double BLUE = 0x52 / 255.0;

    private AshClouds() {
    }

    /** {@code colour} itself (same instance) outside ring 3's reach. */
    public static Vec3 cloudColor(ClientLevel level, Vec3 colour) {
        if (level.dimension() != Level.OVERWORLD) {
            return colour;
        }
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double share = ClientRingLayout.ringAt(level.dimension(), camera.x, camera.z)
                .map(position -> (double) RingWeatherMix.weight(position, Ring.RING_3))
                .orElse(0.0);
        if (share <= 0.0) {
            return colour;
        }
        return new Vec3(colour.x * (1.0 + (RED - 1.0) * share), colour.y * (1.0 + (GREEN - 1.0) * share),
                colour.z * (1.0 + (BLUE - 1.0) * share));
    }
}
