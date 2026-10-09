package nl.timtoys.corruptedadventure.client.weather;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.timtoys.corruptedadventure.network.VolcanoPlumesPayload.Plume;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import nl.timtoys.corruptedadventure.weather.VolcanoPlumes;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Draws the smoke plumes of the volcanoes the server sent ({@code VolcanoPlumesPayload}, package 50b): above every
 * crater within {@link #DRAW_DISTANCE} (as far as the server looks) a steady column of huge, slow billows
 * ({@link PlumeBillow}). The nearest {@link #MAX_DRAWN} only; far plumes get fewer, larger billows. A plume that comes
 * into view starts as a full column. Thicker and glowing at the foot while ring 3 has an eruption. Vanilla's
 * "Particles" option thins them out.
 * <p>
 * Package 50c: the billows are no particles but an own render pass after the weather, with a shader without fog, so a
 * plume beyond the render distance is a plume and not a fog-coloured silhouette. A soft haze of our own takes them
 * towards the fog colour with distance; nothing is drawn with the view under water or lava, blinded or in darkness.
 */
public final class VolcanoPlumeRenderer {
    /** Horizontal distance up to which a plume is drawn: the server's search radius. */
    static final double DRAW_DISTANCE = VolcanoPlumes.RADIUS;
    /** Budget: plumes drawn at once. */
    static final int MAX_DRAWN = 8;
    /** Safety cap on all billows alive (a full budget at an eruption stays well below). */
    private static final int MAX_BILLOWS = 2000;
    /** Billows per tick of an ordinary cone and a great volcano, close by. */
    private static final float RATE = 0.15F;
    private static final float GREAT_RATE = 0.25F;
    /**
     * Level of detail: from {@link #FAR_START} to {@link #FAR_END} blocks a plume goes to {@link #FAR_RATE} of the
     * billows at {@link #FAR_SIZE} times the size, about the same mass of smoke for less than half the quads.
     */
    private static final double FAR_START = 160.0;
    private static final double FAR_END = 288.0;
    private static final float FAR_RATE = 0.4F;
    private static final float FAR_SIZE = 1.6F;
    /** Mean life of a billow in ticks ({@link PlumeBillow}): how many a full column holds per billow per tick. */
    private static final int MEAN_LIFE = 400;
    /** Share of the fog colour in a billow at {@link #DRAW_DISTANCE}: far plumes pale a little, like a distant view. */
    private static final float MAX_HAZE = 0.45F;
    /** Billows are drawn no further than this share of the projection's far plane (render distance × 4). */
    private static final double REACH = 0.85;

    private static volatile List<Plume> plumes = List.of();
    /** The plumes drawn last tick: a plume not in it comes into view and is filled at once. */
    private static final Set<Plume> drawn = new HashSet<>();
    /** All billows alive (main thread only). */
    private static final List<PlumeBillow> billows = new ArrayList<>();
    private static final Vector3f SCRATCH = new Vector3f();
    /** The steam puff's sprites (the particle engine rebinds them on every resource reload). */
    @Nullable
    private static SpriteSet sprites;

    private VolcanoPlumeRenderer() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(VolcanoPlumeRenderer::onClientTick);
        gameBus.addListener(VolcanoPlumeRenderer::onRenderLevelStage);
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
        clear();
    }

    private static void clear() {
        drawn.clear();
        billows.clear();
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || level.dimension() != Level.OVERWORLD) {
            clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        // The billows of a plume that went out of the list live out their life.
        billows.forEach(PlumeBillow::tick);
        billows.removeIf(billow -> !billow.alive());
        List<Plume> known = plumes;
        SpriteSet sheet = sprites;
        if (known.isEmpty() || sheet == null) {
            drawn.clear();
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
            boolean entering = !drawn.contains(plume);
            int count = entering ? Math.round(rate * MEAN_LIFE) : (int) rate + (random.nextFloat() < rate - (int) rate ? 1 : 0);
            for (int i = 0; i < count && billows.size() < MAX_BILLOWS; i++) {
                PlumeBillow billow = spawn(random, sheet, plume, size, eruption);
                if (entering) {
                    billow.prewarm(random.nextFloat());
                }
                billows.add(billow);
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

    /** The fog colour's share in a billow at a horizontal distance: none close by, {@link #MAX_HAZE} at the edge. */
    static float haze(double horizontalDistance) {
        return MAX_HAZE * (float) Mth.clamp(horizontalDistance / DRAW_DISTANCE, 0.0, 1.0);
    }

    private static PlumeBillow spawn(RandomSource random, SpriteSet sheet, Plume plume, float sizeFactor, float eruption) {
        double spread = plume.radius() * 0.35;
        double x = plume.x() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * spread;
        double z = plume.z() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * spread;
        double y = plume.y() + random.nextDouble() * 4.0;
        float size = ((plume.great() ? 7.0F : 4.0F) + plume.radius() * 0.05F) * sizeFactor;
        double rise = (plume.great() ? 0.55 : 0.4) * (1.0 + 0.5 * eruption) * (0.8 + random.nextDouble() * 0.4);
        return new PlumeBillow(random, x, y, z, sheet.get(random), size, rise,
                random.nextGaussian() * 0.01, random.nextGaussian() * 0.01, eruption * random.nextFloat());
    }

    /**
     * After the weather (clouds and rain are in the depth buffer, so a plume passes behind them), translucent, back to
     * front. The shader {@code position_tex_color} has no fog. Depth writes as vanilla's rain: only with "Fabulous!"
     * graphics, whose weather layer is cleared and then sorted against the level per pixel by its depth.
     */
    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        Camera camera = event.getCamera();
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || billows.isEmpty() || level == null
                || !RingWeatherRenderer.clearView(camera)) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 position = camera.getPosition();
        float daylight = RingWeatherRenderer.daylight(level, partialTick);
        float[] fog = RenderSystem.getShaderFogColor();
        double reach = minecraft.gameRenderer.getDepthFar() * REACH;
        billows.sort(Comparator.comparingDouble((PlumeBillow billow) -> billow.distanceSquared(position)).reversed());

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        RenderSystem.disableCull();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (PlumeBillow billow : billows) {
            float haze = haze(billow.horizontalDistance(position));
            billow.render(buffer, position, camera.rotation(), SCRATCH, partialTick, reach, daylight, haze, fog);
        }
        MeshData mesh = buffer.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
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
