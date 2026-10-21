package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The clouds of a ring 5 standstill (package 92): they slow down with the standstill's strength and stand still at its
 * full strength, then drift on from where they stopped (no jump). Kept as a lag behind vanilla's cloud clock; the
 * {@code LevelRendererCloudsMixin} feeds vanilla's {@code renderClouds} the slowed time ({@link #partialTick},
 * {@link #ticks}). Client only, main and render thread (the same thread in 1.21.1).
 */
public final class StandstillClouds {
    private static float feed;
    /** The standstill's strength of the last tick: 0 drifts as vanilla, 1 stands still. */
    private static float freeze;
    /** Ticks the clouds have fallen behind vanilla's clock. */
    private static double lag;
    private static int frameTicks;

    private StandstillClouds() {
    }

    /** The standstill's strength this tick (not × sky exposure: the clouds are sky). */
    static void feed(float strength) {
        feed = Math.max(feed, Math.clamp(strength, 0.0F, 1.0F));
    }

    /** Each client tick, before the weather hooks (so with the feed of the tick before): the time lost this tick. */
    static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        lag += freeze;
        freeze = feed;
        feed = 0.0F;
    }

    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        feed = 0.0F;
        freeze = 0.0F;
        lag = 0.0;
    }

    /**
     * At the start of {@code renderClouds}: the partial tick of the slowed cloud clock (and {@link #ticks} its whole
     * ticks). Exactly vanilla's values while the clouds never stood still.
     */
    public static float partialTick(int ticks, float partialTick) {
        if (lag == 0.0 && freeze == 0.0F) {
            frameTicks = ticks;
            return partialTick;
        }
        double time = cloudTime(ticks, partialTick, freeze, lag);
        frameTicks = (int) Math.floor(time);
        return (float) (time - frameTicks);
    }

    /** The whole ticks of the slowed cloud clock of this frame, for vanilla's reads of its tick counter. */
    public static int ticks() {
        return frameTicks;
    }

    /** Vanilla's clock {@code ticks + partialTick} minus the lag, the current tick slowed by {@code freeze}. */
    static double cloudTime(int ticks, float partialTick, float freeze, double lag) {
        return ticks + partialTick * (1.0 - freeze) - lag;
    }
}
