package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.util.Mth;

/**
 * Pure fog arithmetic of the weather renderers: from vanilla's values towards a weather's, by {@code amount} 0–1
 * (strength × sky exposure). Never further than vanilla, never a near plane beyond the far plane.
 */
final class FogBlend {
    private FogBlend() {
    }

    /** Far plane from vanilla's towards {@code targetFar} (never beyond vanilla's own distance). */
    static float far(float vanillaFar, float targetFar, float amount) {
        return Mth.lerp(clamp(amount), vanillaFar, Math.min(vanillaFar, targetFar));
    }

    /** Near plane from vanilla's towards 0, at most {@code far}. */
    static float near(float vanillaNear, float far, float amount) {
        return Math.min(Mth.lerp(clamp(amount), vanillaNear, 0.0F), far);
    }

    /** One colour channel from vanilla's towards {@code target}. */
    static float colour(float vanilla, float target, float amount) {
        return Mth.lerp(clamp(amount), vanilla, target);
    }

    private static float clamp(float amount) {
        return Mth.clamp(amount, 0.0F, 1.0F);
    }
}
