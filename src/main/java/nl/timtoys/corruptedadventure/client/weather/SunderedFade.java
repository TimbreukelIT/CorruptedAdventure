package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.client.ClientRingLayout;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.biome.BiomeAmbience;

/**
 * Zone 4→5 drains to grey (Q17 = A, change 80): sky, fog and water lose their colour gradually across the zone, from
 * nothing at its inner edge to {@link #MAX_GREY} at its outer edge, read from the synced ring layout (the same zone as
 * the server's {@code ringAt}). Biome colours alone would change at the zone's edge in one step; {@code sundered_reach}
 * therefore carries the blighted band's colours and this hook does the fading. Client only: the sky through
 * {@code ClientLevelSkyColorMixin}, the water through {@code BiomeColorsWaterMixin}, the fog (also under water) here.
 */
public final class SunderedFade {
    /** How grey the outer edge of the zone gets: almost, not quite. */
    static final float MAX_GREY = 0.9F;
    /** Index of the boundary 4|5 in the ring settings (0 = 1|2). */
    private static final int BOUNDARY_4_5 = 3;

    private SunderedFade() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SunderedFade::onComputeFogColor);
    }

    /** 0 outside zone 4→5 (and outside the overworld), rising linearly across the zone to {@link #MAX_GREY}. */
    public static float amount(Level level, double x, double z) {
        if (level == null || level.dimension() != Level.OVERWORLD) {
            return 0.0F;
        }
        RingLayout layout = ClientRingLayout.get().orElse(null);
        // The water tint asks for every meshed water block: skip the boundary noise far from the zone.
        if (layout == null || !nearZone(layout.settings(), x, z)) {
            return 0.0F;
        }
        return amount(layout.sampleOverworld(x, z));
    }

    /** Whether x, z can lie in zone 4→5: within half its width plus the full boundary noise of the 4|5 radius. */
    static boolean nearZone(RingSettings settings, double x, double z) {
        double reach = settings.transitionWidth(BOUNDARY_4_5) / 2.0 + settings.boundaryNoise() + 1.0;
        return Math.abs(Math.sqrt(x * x + z * z) - settings.ringOuterRadii().get(BOUNDARY_4_5)) <= reach;
    }

    static float amount(RingPosition position) {
        return position.inner() == Ring.RING_4 && position.outer() == Ring.RING_5 ? (float) position.blend() * MAX_GREY : 0.0F;
    }

    /** The sky colour at {@code position} (the camera), greyed; {@code colour} itself when nothing changes. */
    public static Vec3 skyColor(ClientLevel level, Vec3 position, Vec3 colour) {
        float amount = amount(level, position.x, position.z);
        return amount <= 0.0F ? colour : grey(colour, amount);
    }

    /**
     * A water colour at block x, z, greyed. Runs on the chunk-meshing threads: only reads the client level reference
     * and the immutable ring layout.
     */
    public static int waterColor(int rgb, int x, int z) {
        float amount = amount(Minecraft.getInstance().level, x + 0.5, z + 0.5);
        return amount <= 0.0F ? rgb : BiomeAmbience.desaturate(rgb, amount);
    }

    /**
     * The fog colour, greyed by the same amount. Vanilla mixes part of the (already greyed) sky colour into it, so
     * that share ends a little greyer than the rest (1 − (1 − a)²); the luma stays, so it only reads as a touch less
     * colour in the fog.
     */
    private static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Vec3 position = event.getCamera().getPosition();
        float amount = amount(event.getCamera().getEntity().level(), position.x, position.z);
        if (amount <= 0.0F) {
            return;
        }
        Vec3 grey = grey(new Vec3(event.getRed(), event.getGreen(), event.getBlue()), amount);
        event.setRed((float) grey.x);
        event.setGreen((float) grey.y);
        event.setBlue((float) grey.z);
    }

    /** Each channel towards the colour's luma ({@link BiomeAmbience#luma}, as {@link BiomeAmbience#desaturate}). */
    static Vec3 grey(Vec3 colour, float amount) {
        double luma = BiomeAmbience.luma(colour.x, colour.y, colour.z);
        return new Vec3(colour.x + (luma - colour.x) * amount, colour.y + (luma - colour.y) * amount,
                colour.z + (luma - colour.z) * amount);
    }
}
