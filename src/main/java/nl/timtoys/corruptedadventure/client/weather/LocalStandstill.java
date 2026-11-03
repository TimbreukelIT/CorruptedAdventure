package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.timtoys.corruptedadventure.network.UnfinishedStandstillPayload;

/**
 * A standstill of one place, not of the ring (phase 4 step 2: The Unfinished's arrival, {@link UnfinishedStandstillPayload}).
 * The server sends it only to the players near the place; this eases it in and out and gives its strength at a position.
 * {@link RingWeatherClientHooks} feeds it to the ring 5 {@link StandstillRenderer} (frozen rain, still clouds, the drained
 * sound, the pale fog) and holds the other ring weather back by the same share, so the standstill takes over where it is
 * felt. Client only, main thread.
 */
public final class LocalStandstill {
    /** Per tick the shown strength moves this much toward full (3 s) or none (4 s). */
    private static final float EASE_IN = 1.0F / 60.0F;
    private static final float EASE_OUT = 1.0F / 80.0F;
    /** Share of the radius felt in full; it thins out over the rest. */
    private static final double FULL_SHARE = 0.75;

    private static ClientLevel level;
    private static double x;
    private static double z;
    private static float radius;
    private static long until;
    private static int seed;
    private static float strength;

    private LocalStandstill() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(ClientTickEvent.Post.class, event -> tick());
        gameBus.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> reset());
    }

    public static void handle(UnfinishedStandstillPayload payload, IPayloadContext context) {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null || !Double.isFinite(payload.x()) || !Double.isFinite(payload.z())) {
            return;
        }
        if (current != level) {
            reset();
            level = current;
        }
        x = payload.x();
        z = payload.z();
        radius = Math.max(1.0F, payload.radius());
        until = current.getGameTime() + Math.max(0, payload.ticks());
        seed = payload.seed();
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) {
            return;
        }
        if (minecraft.level != level) {
            reset();
            return;
        }
        boolean holds = level != null && level.getGameTime() < until;
        strength = holds ? Math.min(1.0F, strength + EASE_IN) : Math.max(0.0F, strength - EASE_OUT);
    }

    /** The standstill felt at x, z in {@code at} (0–1); 0 in any other level. */
    static float strengthAt(Level at, double px, double pz) {
        if (strength <= 0.0F || at != level) {
            return 0.0F;
        }
        double distance = Math.sqrt((px - x) * (px - x) + (pz - z) * (pz - z));
        return strength * falloff(distance, radius);
    }

    /** 1 within {@link #FULL_SHARE} of the radius, then linear down to 0 at the radius. Pure, for tests. */
    static float falloff(double distance, double radius) {
        double full = radius * FULL_SHARE;
        if (distance <= full) {
            return 1.0F;
        }
        return (float) Math.max(0.0, 1.0 - (distance - full) / (radius - full));
    }

    /** Fixes the frozen streaks of this standstill (the same on every client it is sent to). */
    static int seed() {
        return seed;
    }

    private static void reset() {
        level = null;
        strength = 0.0F;
        until = 0L;
    }
}
