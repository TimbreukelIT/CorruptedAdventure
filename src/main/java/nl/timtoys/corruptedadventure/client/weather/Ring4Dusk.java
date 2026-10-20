package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.worldgen.biome.BiomeAmbience;

/**
 * Ring 4's dusk (sparring 2026-10-20, C1-2): sunrise and sunset pink and gold in the mythical band, a pale sickly green
 * in the blighted one. Three hooks, all scaled by ring 4's weight at the camera ({@link Ring4SkyState}) and by how
 * close the sun is to the horizon ({@link #duskness}): the glow around the sun (vanilla's sunrise colour, through
 * {@code DimensionSpecialEffectsMixin}), and a hue shift of the sky and the fog colour that keeps vanilla's
 * brightness, so dusk still darkens as before. Time of day is vanilla-synced: nothing to send.
 */
public final class Ring4Dusk {
    /** Vanilla's sunrise window: the glow exists while |cos(2π · time of day)| ≤ this. */
    static final float WINDOW = 0.4F;
    /** Glow around the sun, outer edge → centre: mythical pink → gold, blighted pale olive → pale yellow-green. */
    private static final float[] MYTHIC_OUTER = {1.0F, 0.42F, 0.60F};
    private static final float[] MYTHIC_INNER = {1.0F, 0.80F, 0.42F};
    private static final float[] BLIGHTED_OUTER = {0.66F, 0.74F, 0.42F};
    private static final float[] BLIGHTED_INNER = {0.88F, 0.92F, 0.60F};
    /** The blighted glow is fainter: a sick sun. */
    private static final float BLIGHTED_ALPHA = 0.75F;
    /** Hue of the sky and the fog at dusk (brightness stays vanilla's). */
    static final int MYTHIC_SKY = 0xC890D0;
    static final int MYTHIC_FOG = 0xF4B4A0;
    static final int BLIGHTED_SKY = 0xB4C88C;
    static final int BLIGHTED_FOG = 0xC8CC90;
    /** How far the sky and fog move to that hue at the height of dusk. */
    static final float SKY_SHARE = 0.55F;
    static final float FOG_SHARE = 0.3F;

    private static final float[] glow = new float[4];

    private Ring4Dusk() {
    }

    /**
     * 0 by day and by night, rising to 1 when the sun is on the horizon: vanilla's sunrise window, squared so it
     * eases in and out.
     */
    static float duskness(float timeOfDay) {
        float cos = Math.abs(Mth.cos(timeOfDay * Mth.TWO_PI));
        if (cos > WINDOW) {
            return 0.0F;
        }
        float d = 1.0F - cos / WINDOW;
        return d * d;
    }

    /**
     * The glow around the sun: vanilla's (may be null outside the window) blended to ring 4's by its weight. Returns
     * {@code vanilla} itself outside ring 4. Render thread only (one shared array, as vanilla's).
     */
    public static float[] sunriseColor(float[] vanilla, float timeOfDay) {
        float weight = Ring4SkyState.ring4();
        if (vanilla == null || weight <= 0.0F) {
            return vanilla;
        }
        // Vanilla's position across the glow (0 outer edge → 1 centre), recovered from its own formula.
        float cos = Mth.cos(timeOfDay * Mth.TWO_PI);
        float across = Mth.clamp(cos / WINDOW * 0.5F + 0.5F, 0.0F, 1.0F);
        float blight = Ring4SkyState.blighted();
        for (int i = 0; i < 3; i++) {
            float mythic = Mth.lerp(across, MYTHIC_OUTER[i], MYTHIC_INNER[i]);
            float blighted = Mth.lerp(across, BLIGHTED_OUTER[i], BLIGHTED_INNER[i]);
            glow[i] = Mth.lerp(weight, vanilla[i], Mth.lerp(blight, mythic, blighted));
        }
        glow[3] = vanilla[3] * Mth.lerp(weight * blight, 1.0F, BLIGHTED_ALPHA);
        return glow;
    }

    /** The sky colour with ring 4's dusk hue; {@code colour} itself when nothing changes. */
    public static Vec3 skyColor(Vec3 colour, float timeOfDay) {
        float amount = Ring4SkyState.ring4() * duskness(timeOfDay) * SKY_SHARE;
        if (amount <= 0.0F) {
            return colour;
        }
        return towardsHue(colour, bandColour(MYTHIC_SKY, BLIGHTED_SKY), amount);
    }

    /** The fog colour with ring 4's dusk hue, as {r, g, b} in place. */
    static void fogColor(float[] rgb, float timeOfDay) {
        float amount = Ring4SkyState.ring4() * duskness(timeOfDay) * FOG_SHARE;
        if (amount <= 0.0F) {
            return;
        }
        Vec3 shifted = towardsHue(new Vec3(rgb[0], rgb[1], rgb[2]), bandColour(MYTHIC_FOG, BLIGHTED_FOG), amount);
        rgb[0] = (float) shifted.x;
        rgb[1] = (float) shifted.y;
        rgb[2] = (float) shifted.z;
    }

    /** The mythical and the blighted colour mixed by the band at the camera. */
    private static Vec3 bandColour(int mythic, int blighted) {
        Vec3 a = Vec3.fromRGB24(mythic);
        Vec3 b = Vec3.fromRGB24(blighted);
        return a.lerp(b, Ring4SkyState.blighted());
    }

    /**
     * {@code colour} towards the hue of {@code hue} by {@code amount}, keeping {@code colour}'s luma: the target is
     * {@code hue} scaled to the same brightness (channels capped at 1).
     */
    static Vec3 towardsHue(Vec3 colour, Vec3 hue, float amount) {
        double hueLuma = BiomeAmbience.luma(hue.x, hue.y, hue.z);
        if (hueLuma <= 0.0) {
            return colour;
        }
        double scale = BiomeAmbience.luma(colour.x, colour.y, colour.z) / hueLuma;
        double a = Mth.clamp(amount, 0.0F, 1.0F);
        return new Vec3(Mth.lerp(a, colour.x, Math.min(1.0, hue.x * scale)), Mth.lerp(a, colour.y, Math.min(1.0, hue.y * scale)),
                Mth.lerp(a, colour.z, Math.min(1.0, hue.z * scale)));
    }
}
