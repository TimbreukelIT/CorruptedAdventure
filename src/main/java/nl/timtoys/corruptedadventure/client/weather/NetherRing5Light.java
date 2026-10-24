package nl.timtoys.corruptedadventure.client.weather;

import org.joml.Vector3f;

/**
 * The light of Nether ring 5 (walk 1, question 1 = A, 2026-10-24): the Nether's lightmap tints the white halls a warm
 * grey-brown (the block light's orange, the forced bright lightmap's tint, no sky), so they read as wood. In Nether ring 5
 * every lightmap colour turns neutral (each channel the brightest one: white light) and is lifted by {@link #LIFT} of the
 * way to full, both times the fog's {@link NetherRing5Fog#presence}. Only the picture: the light levels (spawning, the
 * silence meter, everything on the server) stay as they are. Called from the client mixin on the Nether's
 * special effects ({@code NetherEffectsLightMixin}).
 */
public final class NetherRing5Light {
    static final float LIFT = 0.18F;

    private NetherRing5Light() {
    }

    /** Adjusts one lightmap colour (0..1 per channel) in place. */
    public static void adjust(Vector3f colors) {
        adjust(colors, NetherRing5Fog.presence());
    }

    /** {@link #adjust(Vector3f)} for a given presence (0..1). Pure. */
    static void adjust(Vector3f colors, float presence) {
        if (presence <= 0.0F) {
            return;
        }
        float bright = Math.min(1.0F, Math.max(colors.x, Math.max(colors.y, colors.z)));
        float lifted = bright + (1.0F - bright) * LIFT;
        colors.lerp(new Vector3f(lifted, lifted, lifted), presence);
    }
}
