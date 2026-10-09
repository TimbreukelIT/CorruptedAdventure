package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.timtoys.corruptedadventure.client.particle.VolcanoPlumeParticle;
import nl.timtoys.corruptedadventure.network.VolcanoPlumesPayload.Plume;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import nl.timtoys.corruptedadventure.weather.VolcanoPlumes;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Draws the smoke plumes of the volcanoes the server sent ({@code VolcanoPlumesPayload}, package 50b): above every
 * crater within {@link #DRAW_DISTANCE} (as far as the server looks) a steady column of huge, slow billows
 * ({@link VolcanoPlumeParticle}), created directly so vanilla's 32-block particle limit does not apply. The nearest
 * {@link #MAX_DRAWN} only; far plumes get fewer, larger billows. A plume that comes into view starts as a full column.
 * Thicker and glowing at the foot while ring 3 has an eruption. Vanilla's "Particles" option thins them out.
 */
public final class VolcanoPlumeRenderer {
    /** Horizontal distance up to which a plume is drawn: the server's search radius. */
    static final double DRAW_DISTANCE = VolcanoPlumes.RADIUS;
    /** Budget: plumes drawn at once. */
    static final int MAX_DRAWN = 8;
    /** Billows per tick of an ordinary cone and a great volcano, close by. */
    private static final float RATE = 0.15F;
    private static final float GREAT_RATE = 0.25F;
    /**
     * Level of detail: from {@link #FAR_START} to {@link #FAR_END} blocks a plume goes to {@link #FAR_RATE} of the
     * billows at {@link #FAR_SIZE} times the size, about the same mass of smoke for less than half the particles.
     */
    private static final double FAR_START = 160.0;
    private static final double FAR_END = 288.0;
    private static final float FAR_RATE = 0.4F;
    private static final float FAR_SIZE = 1.6F;
    /** Mean life of a billow in ticks ({@link VolcanoPlumeParticle}): how many a full column holds per billow per tick. */
    private static final int MEAN_LIFE = 400;

    private static volatile List<Plume> plumes = List.of();
    /** The plumes drawn last tick: a plume not in it comes into view and is filled at once. */
    private static final Set<Plume> drawn = new HashSet<>();
    /** The steam puff's sprites (the particle engine rebinds them on every resource reload). */
    @Nullable
    private static SpriteSet sprites;

    private VolcanoPlumeRenderer() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(VolcanoPlumeRenderer::onClientTick);
        gameBus.addListener(VolcanoPlumeRenderer::onLoggingOut);
    }

    /** The sprites of the billows: those of {@code steam_puff}, handed over when its provider is registered. */
    public static void useSprites(SpriteSet steamPuff) {
        sprites = steamPuff;
    }

    /** The server's latest list (main thread). */
    public static void set(List<Plume> received) {
        plumes = List.copyOf(received);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        plumes = List.of();
        drawn.clear();
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        List<Plume> known = plumes;
        SpriteSet billows = sprites;
        if (known.isEmpty() || billows == null || level == null || minecraft.player == null
                || level.dimension() != Level.OVERWORLD) {
            drawn.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        float eruption = eruption(level);
        float density = switch (minecraft.options.particles().get()) {
            case ALL -> 1.0F;
            case DECREASED -> 0.6F;
            case MINIMAL -> 0.35F;
        };
        RandomSource random = level.getRandom();
        List<Plume> inView = known.stream()
                .filter(plume -> horizontalDistanceSquared(plume, camera) < DRAW_DISTANCE * DRAW_DISTANCE)
                .sorted(Comparator.comparingDouble(plume -> horizontalDistanceSquared(plume, camera)))
                .limit(MAX_DRAWN)
                .toList();
        for (Plume plume : inView) {
            float far = far(Math.sqrt(horizontalDistanceSquared(plume, camera)));
            float rate = (plume.great() ? GREAT_RATE : RATE) * density * (1.0F + eruption) * Mth.lerp(far, 1.0F, FAR_RATE);
            float size = Mth.lerp(far, 1.0F, FAR_SIZE);
            if (drawn.contains(plume)) {
                int count = (int) rate + (random.nextFloat() < rate - (int) rate ? 1 : 0);
                for (int i = 0; i < count; i++) {
                    spawn(minecraft, level, random, billows, plume, size, eruption);
                }
            } else {
                int count = Math.round(rate * MEAN_LIFE);
                for (int i = 0; i < count; i++) {
                    spawn(minecraft, level, random, billows, plume, size, eruption).prewarm(random.nextFloat());
                }
            }
        }
        drawn.clear();
        drawn.addAll(inView);
    }

    /** 0 close by, 1 far away: how far a plume has gone to its cheap far form (smooth, so nothing jumps). */
    static float far(double distance) {
        float t = (float) Mth.clamp((distance - FAR_START) / (FAR_END - FAR_START), 0.0, 1.0);
        return t * t * (3.0F - 2.0F * t);
    }

    private static VolcanoPlumeParticle spawn(Minecraft minecraft, ClientLevel level, RandomSource random, SpriteSet billows,
                                              Plume plume, float sizeFactor, float eruption) {
        double spread = plume.radius() * 0.35;
        double x = plume.x() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * spread;
        double z = plume.z() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * spread;
        double y = plume.y() + random.nextDouble() * 4.0;
        float size = ((plume.great() ? 7.0F : 4.0F) + plume.radius() * 0.05F) * sizeFactor;
        double rise = (plume.great() ? 0.55 : 0.4) * (1.0 + 0.5 * eruption) * (0.8 + random.nextDouble() * 0.4);
        VolcanoPlumeParticle billow = new VolcanoPlumeParticle(level, x, y, z, billows.get(random), size, rise,
                random.nextGaussian() * 0.01, random.nextGaussian() * 0.01, eruption * random.nextFloat());
        minecraft.particleEngine.add(billow);
        return billow;
    }

    /** Strength of ring 3's eruption as the server sent it (0 without one); the plumes stand in ring 3 whatever the camera sees. */
    private static float eruption(ClientLevel level) {
        return ClientRingWeather.states(level)
                .map(states -> states.getOrDefault(Ring.RING_3, RingWeatherState.STILL))
                .filter(state -> state.type() == RingWeatherType.ERUPTION)
                .map(RingWeatherState::intensity)
                .orElse(0.0F);
    }

    private static double horizontalDistanceSquared(Plume plume, Vec3 camera) {
        double dx = plume.x() + 0.5 - camera.x;
        double dz = plume.z() + 0.5 - camera.z;
        return dx * dx + dz * dz;
    }
}
