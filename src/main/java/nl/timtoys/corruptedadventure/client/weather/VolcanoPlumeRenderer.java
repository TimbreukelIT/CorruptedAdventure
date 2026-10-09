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
import net.minecraft.client.renderer.culling.Frustum;
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
 * Package 50c: the billows are no particles but an own render pass with a shader without fog, so a plume beyond the
 * render distance is a plume and not a fog-coloured silhouette. Their haze follows the weather's fog where that is
 * shorter than the view (an eruption, an ash cloud), else a soft haze of our own ({@link #haze}); nothing is drawn with
 * the view under water or lava, blinded or in darkness.
 */
public final class VolcanoPlumeRenderer {
    /** Horizontal distance up to which a plume is drawn: the server's search radius. */
    static final double DRAW_DISTANCE = VolcanoPlumes.RADIUS;
    /** Budget: plumes drawn at once. */
    static final int MAX_DRAWN = 8;
    /**
     * Cap on all billows alive (the full budget at an eruption, 8 great plumes close by, stays below it). A plume that
     * comes into view is only filled when its whole column fits; until then it does not count as drawn and tries again.
     */
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
    /**
     * How much shorter than the view the terrain fog must be to count fully as the weather's fog: at a fog end of 75 %
     * of the render distance or less a plume is fogged like the terrain; in between it blends in.
     */
    private static final float WEATHER_FOG_SHORTENING = 0.25F;
    /** Billows are drawn no further than this share of the projection's far plane (render distance × 4). */
    private static final double REACH = 0.85;

    private static volatile List<Plume> plumes = List.of();
    /** The plumes drawn last tick: a plume not in it comes into view and is filled at once. */
    private static final Set<Plume> drawn = new HashSet<>();
    /** All billows alive (main thread only). */
    private static final List<PlumeBillow> billows = new ArrayList<>();
    /** The billows in view this frame (reused). */
    private static final List<PlumeBillow> visible = new ArrayList<>();
    private static final Comparator<PlumeBillow> BACK_TO_FRONT = Comparator.comparingDouble(PlumeBillow::depth).reversed();
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
        visible.clear();
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
        Set<Plume> filled = new HashSet<>();
        for (Plume plume : inView) {
            float far = far(Math.sqrt(horizontalDistanceSquared(plume, camera)));
            float rate = (plume.great() ? GREAT_RATE : RATE) * density * (1.0F + eruption) * Mth.lerp(far, 1.0F, FAR_RATE);
            float size = Mth.lerp(far, 1.0F, FAR_SIZE);
            boolean entering = !drawn.contains(plume);
            int count = entering ? Math.round(rate * MEAN_LIFE) : (int) rate + (random.nextFloat() < rate - (int) rate ? 1 : 0);
            if (billows.size() + count > MAX_BILLOWS) {
                // No room for the whole column: an entering plume stays undrawn and is filled once there is.
                if (!entering) {
                    filled.add(plume);
                }
                continue;
            }
            for (int i = 0; i < count; i++) {
                PlumeBillow billow = spawn(random, sheet, plume, size, eruption);
                if (entering) {
                    billow.prewarm(random.nextFloat());
                }
                billows.add(billow);
            }
            filled.add(plume);
        }
        drawn.clear();
        drawn.addAll(filled);
    }

    /** 0 close by, 1 far away: how far a plume has gone to its cheap far form (smooth, so nothing jumps). */
    static float far(double distance) {
        float t = (float) Mth.clamp((distance - FAR_START) / (FAR_END - FAR_START), 0.0, 1.0);
        return t * t * (3.0F - 2.0F * t);
    }

    /**
     * The fog colour's share in a billow at a (cylindrical) fog distance. Our own soft haze ({@link #MAX_HAZE} at
     * {@link #DRAW_DISTANCE}) is the minimum. Where the weather shortened the terrain fog ({@code fogEnd} below
     * {@code viewEnd}, the render distance), the billow follows vanilla's fog curve like the terrain, fully fogged
     * beyond {@code fogEnd}: the plume stands in the weather. The edge of the render distance alone never fogs it.
     */
    static float haze(double distance, float fogStart, float fogEnd, float viewEnd) {
        float own = MAX_HAZE * (float) Mth.clamp(distance / DRAW_DISTANCE, 0.0, 1.0);
        float weather = viewEnd <= 0.0F ? 0.0F : Mth.clamp((viewEnd - fogEnd) / (viewEnd * WEATHER_FOG_SHORTENING), 0.0F, 1.0F);
        if (weather <= 0.0F) {
            return own;
        }
        float vanilla;
        if (distance <= fogStart) {
            vanilla = 0.0F;
        } else if (distance >= fogEnd) {
            vanilla = 1.0F;
        } else {
            // The smoothstep of vanilla's fog shader.
            float t = (float) ((distance - fogStart) / (fogEnd - fogStart));
            vanilla = t * t * (3.0F - 2.0F * t);
        }
        return Math.max(own, weather * vanilla);
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
     * The pass: translucent, back to front, no depth writes (the quads are sorted on their centres only), shader
     * {@code position_tex_color} (no fog). Fast/Fancy: after the weather, so clouds and rain are in the depth buffer and
     * a plume passes behind them. "Fabulous!": after the particles, into the particle layer, whose depth is the level's,
     * so terrain in front hides a plume and the layer is sorted against the level per pixel; clouds lie on top there.
     */
    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        RenderLevelStageEvent.Stage stage = Minecraft.useShaderTransparency()
                ? RenderLevelStageEvent.Stage.AFTER_PARTICLES : RenderLevelStageEvent.Stage.AFTER_WEATHER;
        Camera camera = event.getCamera();
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (event.getStage() != stage || billows.isEmpty() || level == null || !RingWeatherRenderer.clearView(camera)) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 position = camera.getPosition();
        double reach = minecraft.gameRenderer.getDepthFar() * REACH;
        Frustum frustum = event.getFrustum();
        visible.clear();
        for (PlumeBillow billow : billows) {
            if (billow.prepare(position, partialTick, reach, frustum)) {
                visible.add(billow);
            }
        }
        if (visible.isEmpty()) {
            return;
        }
        visible.sort(BACK_TO_FRONT);
        float daylight = RingWeatherRenderer.daylight(level, partialTick);
        float[] fog = RenderSystem.getShaderFogColor();
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd = RenderSystem.getShaderFogEnd();
        float viewEnd = minecraft.gameRenderer.getRenderDistance();

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (PlumeBillow billow : visible) {
            billow.render(buffer, camera.rotation(), SCRATCH, partialTick, daylight,
                    haze(billow.fogDistance(), fogStart, fogEnd, viewEnd), fog);
        }
        MeshData mesh = buffer.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        visible.clear();
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
