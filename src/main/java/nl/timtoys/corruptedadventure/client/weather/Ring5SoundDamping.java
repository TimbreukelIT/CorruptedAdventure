package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The ring 5 weather's hold on the volume (package 92): the standstill drains every world sound and the music (not the
 * UI: {@code MASTER}, which the menus use, is never touched) down to {@code 1 − 0.85 × strength}; the glitch storm dips
 * the weather and ambient sounds for a few ticks now and then. The renderers feed it every tick while their weather is
 * felt; without a feed it returns to full volume. Applied through {@code SoundEngineVolumeMixin} on the category
 * volume, so sounds that already play follow too (a refresh of the channels whenever the factor changes). Client only,
 * main thread.
 */
public final class Ring5SoundDamping {
    /** How deep a full standstill pulls the volume: to 15 %. */
    static final float STANDSTILL_DEPTH = 0.85F;
    /** Changes smaller than this do not refresh the playing channels. */
    private static final float REFRESH_STEP = 0.01F;

    private static float standstillFeed;
    private static float dipFeed;
    private static float all = 1.0F;
    private static float weatherAndAmbient = 1.0F;
    private static float refreshedAll = 1.0F;
    private static float refreshedWeather = 1.0F;

    private Ring5SoundDamping() {
    }

    /** The standstill's strength this tick (not × sky exposure: the silence reaches indoors too). */
    static void feedStandstill(float strength) {
        standstillFeed = Math.max(standstillFeed, strength);
    }

    /** A glitch storm dip this tick, 0–1 of the weather and ambient volume taken away. */
    static void feedDip(float depth) {
        dipFeed = Math.max(dipFeed, depth);
    }

    /** The volume factor for {@code source}: 1 for {@code MASTER} (the UI) and outside the ring 5 weather. */
    public static float volume(@Nullable SoundSource source) {
        if (source == null || source == SoundSource.MASTER) {
            return 1.0F;
        }
        return source == SoundSource.WEATHER || source == SoundSource.AMBIENT ? weatherAndAmbient : all;
    }

    /** The factor of a standstill at {@code strength} (intensity × band share), at least 1 − {@link #STANDSTILL_DEPTH}. */
    static float standstillFactor(float strength) {
        return 1.0F - STANDSTILL_DEPTH * Math.clamp(strength, 0.0F, 1.0F);
    }

    /**
     * Each client tick (registered before the weather hooks, so it takes the feeds of the tick before): the new factors,
     * and a refresh of the playing channels if they changed. Not while paused (the feeds pause too).
     */
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.isPaused()) {
            return;
        }
        if (advance()) {
            refresh(minecraft);
        }
    }

    /** One tick: the feeds become the factors; true if the playing channels need a refresh. Pure, for tests. */
    static boolean advance() {
        all = standstillFactor(standstillFeed);
        weatherAndAmbient = all * (1.0F - Math.clamp(dipFeed, 0.0F, 1.0F));
        standstillFeed = 0.0F;
        dipFeed = 0.0F;
        return takeRefresh();
    }

    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (reset()) {
            refresh(Minecraft.getInstance());
        }
    }

    /** Back to full volume; true if the playing channels need a refresh. */
    static boolean reset() {
        standstillFeed = 0.0F;
        dipFeed = 0.0F;
        all = 1.0F;
        weatherAndAmbient = 1.0F;
        return takeRefresh();
    }

    /** Whether the factors moved far enough from the last refreshed ones; if so they become the refreshed ones. */
    private static boolean takeRefresh() {
        if (!changed(refreshedAll, all) && !changed(refreshedWeather, weatherAndAmbient)) {
            return false;
        }
        refreshedAll = all;
        refreshedWeather = weatherAndAmbient;
        return true;
    }

    private static void refresh(Minecraft minecraft) {
        // A non-MASTER category recalculates every playing channel (through our factor); the volume passed is unused there.
        minecraft.getSoundManager().updateSourceVolume(SoundSource.AMBIENT, minecraft.options.getSoundSourceVolume(SoundSource.AMBIENT));
    }

    /** A change worth a refresh: a real step, or the last bit back to exactly full volume. */
    static boolean changed(float refreshed, float now) {
        return Math.abs(refreshed - now) >= REFRESH_STEP || now == 1.0F && refreshed != 1.0F;
    }
}
