package nl.timtoys.corruptedadventure.client.weather;

/**
 * How much of ring 5's glitch visuals shows: the glitch fog's stutter and violet ({@link Ring5Fog}) and the screen's
 * distortion ({@link GlitchStormOverlay}: noise, scanlines, the hotbar hiccup, from the storm and the void meter alike).
 * 1 = all of it; the sky glass lens damps it while you look through it (phase 2 golf 5, {@code client/item/LensView}).
 * Client only.
 */
public final class GlitchVisuals {
    private static float scale = 1.0F;

    private GlitchVisuals() {
    }

    public static float scale() {
        return scale;
    }

    public static void setScale(float value) {
        scale = Math.clamp(value, 0.0F, 1.0F);
    }
}
