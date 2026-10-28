package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.weather.RingWeatherMix;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import nl.timtoys.corruptedadventure.worldgen.biome.BiomeAmbience;

/**
 * Ring 5's fog outside the weather (package 98), from {@link Ring5Sky}:
 * <ul>
 *     <li><b>Inner band</b> (V98-3 A): thick and colourless: the far plane at {@link #FAR_SHARE} of the view (between
 *     {@link #MIN_FAR} and {@link #MAX_FAR} blocks), near at {@link #NEAR_SHARE} of that, the sky fog at
 *     {@link #SKY_FACTOR} × so the grey-violet sky overhead stays; the colour drained to its own grey (luma). It thins
 *     out towards ring 5's outer boundary, so the outer band looks far over the void.</li>
 *     <li><b>Outer band</b>: the fog colour goes black with the sky ({@link Ring5Sky#blackness}).</li>
 *     <li><b>Glitch fog</b> (V98-4 A): {@link GlitchFog} makes the edge stutter in both bands (from the middle of zone
 *     4→5 on), not during a glitch storm of its own (that has its flashes).</li>
 * </ul>
 * Combining as {@link Ring4Fog}: distances last ({@link EventPriority#LOWEST}, also after a cancelled event) and only
 * ever closer, so a denser weather fog wins; the drain to grey first ({@link EventPriority#HIGH}, the weather tints on
 * top), the black and the glitch colour after the weather ({@link EventPriority#LOW}). The thick fog and the black follow
 * {@link SkyExposure} (caves keep vanilla's); nothing under water, in lava or blind. Client only.
 */
public final class Ring5Fog {
    static final float FAR_SHARE = 0.45F;
    static final float MIN_FAR = 48.0F;
    static final float MAX_FAR = 112.0F;
    static final float NEAR_SHARE = 0.15F;
    static final float SKY_FACTOR = 2.0F;
    /** Violet-black of a glitch (the glitch storm's flash, #120818). */
    private static final float GLITCH_RED = 0x12 / 255.0F;
    private static final float GLITCH_GREEN = 0x08 / 255.0F;
    private static final float GLITCH_BLUE = 0x18 / 255.0F;
    /** The glitch fog runs from this ring 5 weight at the camera on (the middle of zone 4→5). */
    static final float GLITCH_FROM = 0.5F;
    /** Above this glitch storm strength the storm's own flashes take over. */
    static final float STORM_TAKES_OVER = 0.5F;

    private static final GlitchFog GLITCH = new GlitchFog();

    private Ring5Fog() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(Ring5Fog::onClientTick);
        gameBus.addListener(EventPriority.LOWEST, true, Ring5Fog::onRenderFog);
        gameBus.addListener(EventPriority.HIGH, Ring5Fog::onDrainFogColor);
        gameBus.addListener(EventPriority.LOW, Ring5Fog::onDarkenFogColor);
        gameBus.addListener(Ring5Fog::onLoggingOut);
    }

    /** The inner band's far plane for a view distance (blocks). Pure. */
    static float innerFar(float view) {
        return Mth.clamp(view * FAR_SHARE, MIN_FAR, MAX_FAR);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        boolean active = Ring5Sky.ring5() > GLITCH_FROM;
        float rate = 1.0F;
        if (active) {
            Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
            float storm = RingWeatherClientHooks.felt(level, camera.x, camera.z, RingWeatherType.GLITCH_STORM)
                    .map(RingWeatherMix.Share::strength).orElse(0.0F);
            active = storm < STORM_TAKES_OVER;
            rate = level.getBiome(BlockPos.containing(camera)).is(ModBiomes.GLITCH_FIELDS) ? 2.0F : 1.0F;
        }
        GLITCH.tick(level.getRandom(), active, rate);
    }

    private static boolean applies(ViewportEvent event) {
        return Ring5Sky.ring5() > 0.0F && RingWeatherRenderer.clearView(event.getCamera())
                && event.getCamera().getEntity().level().dimension() == Level.OVERWORLD;
    }

    /** Vanilla's "Screen Effect Scale" × what the sky glass lens leaves of the glitch visuals ({@link GlitchVisuals}). */
    private static float screenEffects() {
        return Minecraft.getInstance().options.screenEffectScale().get().floatValue() * GlitchVisuals.scale();
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!applies(event)) {
            return;
        }
        boolean sky = event.getMode() == FogRenderer.FogMode.FOG_SKY;
        float view = Minecraft.getInstance().gameRenderer.getRenderDistance();
        float baseFar = sky ? view : Math.max(view, 32.0F);
        float baseNear = sky ? 0.0F : baseFar - Mth.clamp(baseFar / 10.0F, 4.0F, 64.0F);
        float far = event.getFarPlaneDistance();
        float near = event.getNearPlaneDistance();
        float newFar = far;
        float newNear = near;
        float thick = Ring5Sky.innerFog() * SkyExposure.get();
        if (thick > 0.0F) {
            float target = innerFar(view) * (sky ? SKY_FACTOR : 1.0F);
            float innerFar = FogBlend.far(baseFar, target, thick);
            newFar = Math.min(newFar, innerFar);
            if (!sky) {
                newNear = Math.min(newNear, Mth.lerp(thick, baseNear, innerFar * NEAR_SHARE));
            }
        }
        float stutter = GLITCH.farFactor(screenEffects());
        if (stutter < 1.0F) {
            newFar *= stutter;
            newNear *= stutter;
        }
        if (newFar == far && newNear == near) {
            return;
        }
        event.setFarPlaneDistance(newFar);
        event.setNearPlaneDistance(Math.min(newNear, newFar));
        // Applied only when cancelled.
        event.setCanceled(true);
    }

    private static void onDrainFogColor(ViewportEvent.ComputeFogColor event) {
        if (!applies(event)) {
            return;
        }
        float amount = Ring5Sky.innerFog() * SkyExposure.get();
        if (amount <= 0.0F) {
            return;
        }
        float grey = (float) BiomeAmbience.luma(event.getRed(), event.getGreen(), event.getBlue());
        event.setRed(FogBlend.colour(event.getRed(), grey, amount));
        event.setGreen(FogBlend.colour(event.getGreen(), grey, amount));
        event.setBlue(FogBlend.colour(event.getBlue(), grey, amount));
    }

    private static void onDarkenFogColor(ViewportEvent.ComputeFogColor event) {
        if (!applies(event)) {
            return;
        }
        float black = Ring5Sky.blackness() * SkyExposure.get();
        if (black > 0.0F) {
            event.setRed(event.getRed() * (1.0F - black));
            event.setGreen(event.getGreen() * (1.0F - black));
            event.setBlue(event.getBlue() * (1.0F - black));
        }
        if (GLITCH.violet()) {
            float amount = Mth.clamp(screenEffects(), 0.0F, 1.0F);
            event.setRed(FogBlend.colour(event.getRed(), GLITCH_RED, amount));
            event.setGreen(FogBlend.colour(event.getGreen(), GLITCH_GREEN, amount));
            event.setBlue(FogBlend.colour(event.getBlue(), GLITCH_BLUE, amount));
        }
    }

    /** The next world starts without a stutter. */
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        GLITCH.reset();
    }
}
