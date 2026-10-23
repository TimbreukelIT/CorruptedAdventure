package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.timtoys.corruptedadventure.client.ClientRingLayout;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.weather.RingWeatherMix;

/**
 * Ring 5's sky (package 98, V98-1 A and V98-2 A), from where the camera stands once per client tick: the inner band
 * keeps its grey-violet biome sky, the outer band goes black. The blackness is 0 on the band line ({@code Ring5Band},
 * from the synced layout, so no biome steps), {@link #EDGE_BLACKNESS} at ring 5's outer boundary and 1 from
 * {@link #VOID_RUN} blocks beyond it over the void. The sky colour goes to black by it, the sun, moon and sunrise glow
 * dim to {@link #MIN_SUN} of themselves, and the stars shine by it (by day too; vanilla draws a full sphere of stars, so
 * they show below the horizon when looking down into the void). Light levels are untouched: only the drawing changes.
 * {@link #innerFog} tells {@link Ring5Fog} how much of the inner band's thick fog is left. Nothing is synced.
 * Render-thread only.
 */
public final class Ring5Sky {
    /** The blackness at ring 5's outer boundary (9,700 by default). */
    static final float EDGE_BLACKNESS = 0.85F;
    /** Blocks past ring 5's outer boundary at which the sky is fully black (9,900 by default). */
    static final double VOID_RUN = 200.0;
    /** What is left of the sun and the moon (and the sunrise glow) under a fully black sky. */
    static final float MIN_SUN = 0.4F;
    /** Vanilla's star brightness at midnight ({@code ClientLevel#getStarBrightness}: at most 0.5). */
    static final float FULL_STARS = 0.5F;

    /** Our own scratch array for the dimmed glow (render thread only; vanilla shares its array the same way). */
    private static final float[] SUNRISE = new float[4];

    private static float ring5;
    private static float blackness;
    private static float innerFog;

    private Ring5Sky() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(Ring5Sky::onClientTick);
        gameBus.addListener(Ring5Sky::onLoggingOut);
    }

    /** Ring 5's weight at the camera (the void rim follows ring 5), 0 outside it and outside the overworld. */
    static float ring5() {
        return ring5;
    }

    /** How black the sky is at the camera, 0–1 (already × ring 5's weight). */
    static float blackness() {
        return blackness;
    }

    /** How much of the inner band's thick fog is left at the camera, 0–1 (already × ring 5's weight). */
    static float innerFog() {
        return innerFog;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        RingLayout layout = Level.OVERWORLD.equals(level.dimension()) ? ClientRingLayout.get().orElse(null) : null;
        if (layout == null) {
            clear();
            return;
        }
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        ring5 = RingWeatherMix.weight(layout.sampleOverworld(camera.x, camera.z), Ring.RING_5);
        if (ring5 <= 0.0F) {
            clear();
            return;
        }
        double signed = Ring5Band.of(layout).signedDistance(camera.x, camera.z);
        double distance = layout.effectiveDistance(camera.x, camera.z);
        double edge = layout.settings().ringOuterRadii().get(Ring.RING_5.ordinal());
        float outward = outward(signed, distance, edge);
        blackness = ring5 * blackness(outward, distance - edge);
        innerFog = ring5 * (1.0F - smoothstep(outward));
    }

    private static void clear() {
        ring5 = 0.0F;
        blackness = 0.0F;
        innerFog = 0.0F;
    }

    /**
     * How far a position lies from the band line towards ring 5's outer boundary, 0–1: 0 on and inside the line, 1 at
     * and beyond the boundary. {@code signed} is {@code Ring5Band.signedDistance} (the line with its noise), so the
     * remaining way to the boundary at this position is {@code edge − distance}. Pure.
     */
    static float outward(double signed, double distance, double edge) {
        if (signed <= 0.0) {
            return 0.0F;
        }
        double left = edge - distance;
        if (left <= 0.0) {
            return 1.0F;
        }
        return (float) Mth.clamp(signed / (signed + left), 0.0, 1.0);
    }

    /**
     * The blackness for {@link #outward} progress and the distance past ring 5's outer boundary: a smoothstep to
     * {@link #EDGE_BLACKNESS} at the boundary, then on to 1 over {@link #VOID_RUN} blocks of void. Pure.
     */
    static float blackness(float outward, double pastEdge) {
        float inside = EDGE_BLACKNESS * smoothstep(outward);
        if (pastEdge <= 0.0) {
            return inside;
        }
        return Mth.lerp(smoothstep((float) (pastEdge / VOID_RUN)), inside, 1.0F);
    }

    /** The sky colour towards black, for {@code ClientLevelSkyColorMixin}; {@code colour} itself outside ring 5. */
    public static Vec3 skyColor(Vec3 colour) {
        if (blackness <= 0.0F) {
            return colour;
        }
        return colour.scale(1.0F - blackness);
    }

    /** The star brightness, for {@code ClientLevelSkyColorMixin}: vanilla's or the blackness's, whichever is brighter. */
    public static float starBrightness(float vanilla) {
        return stars(vanilla, blackness);
    }

    /** Pure: at least {@link #FULL_STARS} × {@code blackness}. */
    static float stars(float vanilla, float blackness) {
        return Math.max(vanilla, FULL_STARS * blackness);
    }

    /** The factor on the sun's and moon's alpha (and the sunrise glow's), for the sky mixins. */
    public static float sunFactor() {
        return sunFactor(blackness);
    }

    /** Pure: 1 without blackness, {@link #MIN_SUN} under a fully black sky. */
    static float sunFactor(float blackness) {
        return Mth.lerp(blackness, 1.0F, MIN_SUN);
    }

    /**
     * The sunrise glow with its alpha dimmed like the sun, for {@code DimensionSpecialEffectsMixin}; {@code colour}
     * itself (vanilla's shared array, never written) outside ring 5's outer band. The result is a shared array too.
     */
    public static float[] sunriseColor(float[] colour) {
        if (colour == null || blackness <= 0.0F) {
            return colour;
        }
        SUNRISE[0] = colour[0];
        SUNRISE[1] = colour[1];
        SUNRISE[2] = colour[2];
        SUNRISE[3] = colour[3] * sunFactor();
        return SUNRISE;
    }

    static float smoothstep(float t) {
        float clamped = Mth.clamp(t, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }

    /** The next world starts clear. */
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }
}
