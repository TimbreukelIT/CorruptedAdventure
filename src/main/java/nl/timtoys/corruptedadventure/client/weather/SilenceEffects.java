package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

import java.util.Set;

/**
 * The silence of Nether ring 5 on the client (package 98; the meter, darkness and damage are stream C's, 97). Read from
 * the synced meter and its three thresholds (30/60/90 by default):
 * <ul>
 *     <li><b>Volume</b> (V98-9 A): from the first threshold every world sound and the music glide from 100 % down to
 *     {@link #LOW_VOLUME} at the third, then to nothing (at most {@link #GLIDE_PER_TICK} per tick, so ~1 s); the UI
 *     ({@code MASTER}) is never touched ({@link Ring5SoundDamping}).</li>
 *     <li><b>Heartbeat</b>: from the third threshold the only sound left, faster as the meter rises
 *     ({@link #SLOW_BEAT} → {@link #FAST_BEAT} ticks), in {@code MASTER} so the damping leaves it alone.</li>
 *     <li><b>HUD</b> (V98-10 A): from the second threshold the HUD goes, except the hotbar (with the item name), the
 *     crosshair, the condition bars and the chat; back {@link #HUD_HYSTERESIS} below it, so it never flickers.</li>
 * </ul>
 * Client only; nothing synced back.
 */
public final class SilenceEffects {
    static final float LOW_VOLUME = 0.35F;
    static final float GLIDE_PER_TICK = 0.05F;
    static final int SLOW_BEAT = 20;
    static final int FAST_BEAT = 10;
    static final int HUD_HYSTERESIS = 2;
    /** The HUD layers the silence takes away (V98-10 A); everything else stays. */
    static final Set<ResourceLocation> HIDDEN_LAYERS = Set.of(VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.AIR_LEVEL, VanillaGuiLayers.VEHICLE_HEALTH,
            VanillaGuiLayers.EXPERIENCE_BAR, VanillaGuiLayers.EXPERIENCE_LEVEL, VanillaGuiLayers.JUMP_METER,
            VanillaGuiLayers.EFFECTS, VanillaGuiLayers.BOSS_OVERLAY, VanillaGuiLayers.SCOREBOARD_SIDEBAR);

    private static float volume = 1.0F;
    private static boolean hudHidden;
    private static int beatIn;

    private SilenceEffects() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(SilenceEffects::onClientTick);
        gameBus.addListener(SilenceEffects::onRenderGuiLayer);
        gameBus.addListener(SilenceEffects::onLoggingOut);
    }

    /** The volume the silence aims at for {@code meter}: 1 below {@code low}, gliding to {@link #LOW_VOLUME} at {@code top}, then 0. Pure. */
    static float targetVolume(int meter, int low, int top) {
        if (meter < low) {
            return 1.0F;
        }
        if (meter >= top) {
            return 0.0F;
        }
        return Mth.lerp((meter - low) / (float) (top - low), 1.0F, LOW_VOLUME);
    }

    /** Ticks between two heartbeats at {@code meter} from {@code top} up to 100. Pure. */
    static int beatTicks(int meter, int top) {
        float t = top >= 100 ? 1.0F : Mth.clamp((meter - top) / (float) (100 - top), 0.0F, 1.0F);
        return Math.round(Mth.lerp(t, SLOW_BEAT, FAST_BEAT));
    }

    /** Whether the HUD is hidden at {@code meter}, with the hysteresis below {@code threshold}. Pure. */
    static boolean hidden(int meter, int threshold, boolean wasHidden) {
        return wasHidden ? meter >= threshold - HUD_HYSTERESIS : meter >= threshold;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        int meter = 0;
        int[] levels = null;
        Condition silence = Ring5Meters.silence().orElse(null);
        if (silence != null) {
            levels = Ring5Meters.thresholds(silence, 3).orElse(null);
            meter = levels == null ? 0 : Ring5Meters.value(player, silence);
        }
        float target = levels == null ? 1.0F : targetVolume(meter, levels[0], levels[2]);
        volume = SkyExposure.approach(volume, target, GLIDE_PER_TICK);
        if (volume < 1.0F) {
            Ring5SoundDamping.feedSilence(volume);
        }
        hudHidden = levels != null && hidden(meter, levels[1], hudHidden);
        if (levels != null && meter >= levels[2]) {
            if (--beatIn <= 0) {
                beatIn = beatTicks(meter, levels[2]);
                beat(minecraft, minecraft.level.getRandom());
            }
        } else {
            beatIn = 0;
        }
    }

    private static void beat(Minecraft minecraft, RandomSource random) {
        // Not positional, in MASTER: the one sound the silence leaves.
        SoundInstance beat = new SimpleSoundInstance(ModSoundEvents.NETHER_RING_5_HEARTBEAT.get().getLocation(), SoundSource.MASTER,
                0.8F, 1.0F, random, false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true);
        minecraft.getSoundManager().play(beat);
    }

    private static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (hudHidden && HIDDEN_LAYERS.contains(event.getName())) {
            event.setCanceled(true);
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        volume = 1.0F;
        hudHidden = false;
        beatIn = 0;
    }
}
