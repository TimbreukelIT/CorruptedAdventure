package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.client.ClientRingLayout;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.weather.RingWeatherMix;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * The one place that decides which ring weathers are felt where on the client: {@link ClientRingLayout} at the
 * position → {@link RingWeatherMix} (time fade × spatial share, void → ring 5) → the registered renderers. Ticks and
 * fog use the camera (the player in first person). Weather changes only write a renderer.
 */
public final class RingWeatherClientHooks {
    private RingWeatherClientHooks() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(RingWeatherClientHooks::onClientTick);
        gameBus.addListener(RingWeatherClientHooks::onRenderFog);
        gameBus.addListener(RingWeatherClientHooks::onComputeFogColor);
        gameBus.addListener(RingWeatherClientHooks::onRenderLevelStage);
        gameBus.addListener(ClientRingWeather::onLoggingOut);
    }

    /** The rings felt at x, z in {@code level}; empty without a layout or without ring weather in that dimension. */
    public static List<RingWeatherMix.Share> shares(Level level, double x, double z) {
        Optional<RingPosition> position = ClientRingLayout.ringAt(level.dimension(), x, z);
        Optional<Map<Ring, RingWeatherState>> states = ClientRingWeather.states(level);
        if (position.isEmpty() || states.isEmpty()) {
            return List.of();
        }
        Map<Ring, RingWeatherState> known = states.get();
        return RingWeatherMix.at(position.get(), ring -> known.getOrDefault(ring, RingWeatherState.STILL));
    }

    /**
     * The F3 text, e.g. {@code ring_2 sandstorm 0.83 (412 s)} or, in a transition zone,
     * {@code ring_2 sandstorm 0.40 (weight 0.48, 412 s) | ring_3 clear (weight 0.52, 1200 s)}.
     */
    public static String describe(Level level, double x, double z) {
        if (ClientRingLayout.get().isEmpty() || ClientRingWeather.states(level).isEmpty()) {
            return "—";
        }
        String text = shares(level, x, z).stream().map(RingWeatherClientHooks::describe).collect(Collectors.joining(" | "));
        return ClientRingWeather.cycleRunning() ? text : text + " (cycle paused)";
    }

    private static String describe(RingWeatherMix.Share share) {
        String ring = share.ring().getSerializedName();
        RingWeatherState state = share.state();
        if (state.isStill()) {
            return ring + " none";
        }
        String weight = share.weight() < 1.0F ? String.format(Locale.ROOT, "weight %.2f, ", share.weight()) : "";
        String timing = weight + (state.remainingTicks() + 19) / 20 + " s";
        if (state.type() == RingWeatherType.CLEAR) {
            return ring + " clear (" + timing + ")";
        }
        return String.format(Locale.ROOT, "%s %s %.2f (%s)", ring, state.type().getSerializedName(), share.strength(), timing);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (RingWeatherRenderers.isEmpty() || minecraft.level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        forEachFelt(minecraft.gameRenderer.getMainCamera().getPosition(),
                (renderer, strength) -> renderer.clientTick(minecraft.level, minecraft.player, strength));
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!RingWeatherRenderers.isEmpty()) {
            forEachFelt(event.getCamera().getPosition(), (renderer, strength) -> renderer.renderFog(event, strength));
        }
    }

    private static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (!RingWeatherRenderers.isEmpty()) {
            forEachFelt(event.getCamera().getPosition(), (renderer, strength) -> renderer.fogColor(event, strength));
        }
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!RingWeatherRenderers.isEmpty()) {
            forEachFelt(event.getCamera().getPosition(), (renderer, strength) -> renderer.renderLevel(event, strength));
        }
    }

    private static void forEachFelt(Vec3 position, BiConsumer<RingWeatherRenderer, Float> action) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (RingWeatherMix.Share share : shares(level, position.x, position.z)) {
            RingWeatherRenderer renderer = RingWeatherRenderers.get(share.state().type());
            if (renderer != null && share.strength() > 0.0F) {
                action.accept(renderer, share.strength());
            }
        }
    }
}
