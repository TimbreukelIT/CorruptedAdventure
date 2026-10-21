package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The clouds of a ring 5 standstill (package 92): they slow down with the standstill's strength and stand still at its
 * full strength, then drift on from where they stopped (no jump). Kept as a lag behind vanilla's cloud clock; the
 * {@code LevelRendererCloudsMixin} takes it off vanilla's drift ({@link #drift}) and leaves everything else in
 * {@code renderClouds} alone. A renderer that replaces vanilla's clouds (Sodium, Embeddium) just keeps them drifting.
 * Client only, main and render thread (the same thread in 1.21.1).
 */
public final class StandstillClouds {
    /** Vanilla's drift in blocks per tick ({@code (ticks + partialTick) × 0.03}). */
    static final double DRIFT_PER_TICK = 0.03;

    private static float feed;
    /** The standstill's strength of the last tick: 0 drifts as vanilla, 1 stands still. */
    private static float freeze;
    /** Ticks the clouds have fallen behind vanilla's clock. */
    private static double lag;
    /** The lag of this frame, taken at the start of {@code renderClouds}. */
    private static double frameLag;

    private StandstillClouds() {
    }

    /** The standstill's strength this tick (not × sky exposure: the clouds are sky). */
    static void feed(float strength) {
        feed = Math.max(feed, Math.clamp(strength, 0.0F, 1.0F));
    }

    /** Each client tick, before the weather hooks (so with the feed of the tick before). Not while paused. */
    static void onClientTick(ClientTickEvent.Post event) {
        if (!Minecraft.getInstance().isPaused()) {
            advance();
        }
    }

    /** One tick: the tick that just passed lost {@code freeze} ticks of drift, then the new feed becomes the freeze. */
    static void advance() {
        lag += freeze;
        freeze = feed;
        feed = 0.0F;
    }

    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    static void reset() {
        feed = 0.0F;
        freeze = 0.0F;
        lag = 0.0;
        frameLag = 0.0;
    }

    /** At the start of {@code renderClouds}: the lag of this frame, the current tick slowed by the freeze. */
    public static void beginFrame(float partialTick) {
        frameLag = frameLag(lag, freeze, partialTick);
    }

    /** Vanilla's drift of this frame minus the lag; exactly vanilla's value while the clouds never stood still. */
    public static double drift(double vanilla) {
        return frameLag == 0.0 ? vanilla : vanilla - DRIFT_PER_TICK * frameLag;
    }

    /** The lag at {@code partialTick} into the current tick. */
    static double frameLag(double lag, float freeze, float partialTick) {
        return lag + freeze * (double) partialTick;
    }

    static double lag() {
        return lag;
    }

    static float freeze() {
        return freeze;
    }
}
