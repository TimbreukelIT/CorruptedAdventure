package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Ring 4's fog outside the weather (sparring 2026-10-20), on top of the biome fog colours, all from
 * {@link Ring4SkyState} and scaled by ring 4's weight at the camera:
 * <ul>
 *     <li><b>Distance haze</b> (C2-2): the terrain fog starts at {@link #HAZE_NEAR} of the view instead of in its last
 *     tenth, so far islands fade into the (aqua) fog colour: aerial perspective. The far plane stays vanilla's.</li>
 *     <li><b>Blighted haze</b> (C1-4): in the blighted band a sickly yellow-green haze, thicker towards ring 5: far
 *     plane down to {@link #BLIGHT_FAR} of the view and the colour towards {@link #BLIGHT_COLOUR}.</li>
 *     <li><b>Low mist</b> (C2-1): over {@code mist_fjords} and the swamps the fog closes in to the biome's mist
 *     distance while the camera is low (full up to {@link #MIST_FULL_ABOVE} above the sea, gone at
 *     {@link #MIST_CLEAR_ABOVE}): into the mist in the fjords and over the water, open view from plateaus and
 *     islands.</li>
 * </ul>
 * Combining: the fog distance runs last ({@link EventPriority#LOWEST}, also after a weather renderer cancelled the event)
 * and only ever brings the planes closer, so the weather's fog wins where it is denser; the colour runs first
 * ({@link EventPriority#HIGH}), so a weather renderer's colour and the grey fade of zone 4→5 ({@link SunderedFade}) go
 * on top. Not under water, in lava, blind, or out of the sky ({@link SkyExposure}: caves keep vanilla's fog). Client only.
 */
public final class Ring4Fog {
    /** Ring 4's flat sea surface (design 2026-10-16: open water up to Y 62). */
    static final int SEA_LEVEL = 62;
    /** Terrain fog start in clear weather as a share of the far plane (vanilla: the last ~10 %). */
    static final float HAZE_NEAR = 0.7F;
    /** Far plane at the outer edge of the blighted band as a share of the view (terrain, sky); near plane share. */
    static final float BLIGHT_FAR = 0.6F;
    static final float BLIGHT_SKY_FAR = 0.75F;
    static final float BLIGHT_NEAR = 0.2F;
    /** The blighted haze on the band line, as a share of its strength at ring 5. */
    static final float BLIGHT_BASE = 0.3F;
    /** Sickly yellow-green; how far the fog colour goes there at full strength. */
    static final int BLIGHT_COLOUR = 0xC2C46A;
    static final float BLIGHT_COLOUR_SHARE = 0.4F;
    /** The sky towards the haze: a share of the fog colour's amount. */
    static final float BLIGHT_SKY_SHARE = 0.3F;
    /** Low mist: full up to this many blocks above the sea, gone at the second. */
    static final float MIST_FULL_ABOVE = 8.0F;
    static final float MIST_CLEAR_ABOVE = 38.0F;
    /** The sky fog in mist reaches this multiple of the mist's far plane: the sky overhead stays visible. */
    static final float MIST_SKY_FACTOR = 2.5F;
    static final float MIST_COLOUR_SHARE = 0.8F;
    static final float MIST_SKY_SHARE = 0.35F;
    /** The fog colours follow the daylight down to this share (mist in moonlight stays a little light). */
    static final float MIN_LIGHT = 0.1F;

    private Ring4Fog() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(EventPriority.LOWEST, true, Ring4Fog::onRenderFog);
        gameBus.addListener(EventPriority.HIGH, Ring4Fog::onComputeFogColor);
    }

    /** Low mist at the camera, 0–1: the biome's eased presence × the height factor × sky exposure × ring 4. */
    static float mistAmount() {
        return Ring4SkyState.ring4() * Ring4SkyState.mist() * mistHeight(Ring4SkyState.cameraY()) * SkyExposure.get();
    }

    /** 1 up to {@link #MIST_FULL_ABOVE} above the sea, smoothly 0 at {@link #MIST_CLEAR_ABOVE}. */
    static float mistHeight(float cameraY) {
        float t = Mth.clamp((cameraY - SEA_LEVEL - MIST_FULL_ABOVE) / (MIST_CLEAR_ABOVE - MIST_FULL_ABOVE), 0.0F, 1.0F);
        return 1.0F - t * t * (3.0F - 2.0F * t);
    }

    /** The blighted haze at the camera, 0–1: band × (base … 1 towards ring 5) × ring 4 × sky exposure. */
    static float blightAmount() {
        float band = Ring4SkyState.blighted();
        if (band <= 0.0F) {
            return 0.0F;
        }
        return Ring4SkyState.ring4() * band * Mth.lerp(Ring4SkyState.towardsRing5(), BLIGHT_BASE, 1.0F) * SkyExposure.get();
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        float ring4 = Ring4SkyState.ring4();
        if (ring4 <= 0.0F || !RingWeatherRenderer.clearView(event.getCamera())
                || event.getCamera().getEntity().level().dimension() != Level.OVERWORLD) {
            return;
        }
        boolean sky = event.getMode() == FogRenderer.FogMode.FOG_SKY;
        // Vanilla's own planes for this view, not the event's (a weather renderer may have moved those already): each
        // effect is measured from vanilla, then only the closest plane counts.
        float view = Minecraft.getInstance().gameRenderer.getRenderDistance();
        float baseFar = sky ? view : Math.max(view, 32.0F);
        float baseNear = sky ? 0.0F : baseFar - Mth.clamp(baseFar / 10.0F, 4.0F, 64.0F);
        float far = event.getFarPlaneDistance();
        float near = event.getNearPlaneDistance();
        float newFar = far;
        float newNear = near;
        if (!sky) {
            newNear = Math.min(newNear, Mth.lerp(ring4 * SkyExposure.get(), baseNear, baseFar * HAZE_NEAR));
        }
        float blight = blightAmount();
        if (blight > 0.0F) {
            float blightFar = baseFar * Mth.lerp(blight, 1.0F, sky ? BLIGHT_SKY_FAR : BLIGHT_FAR);
            newFar = Math.min(newFar, blightFar);
            if (!sky) {
                newNear = Math.min(newNear, Mth.lerp(blight, baseNear, blightFar * BLIGHT_NEAR));
            }
        }
        float mist = mistAmount();
        if (mist > 0.0F) {
            float mistFar = FogBlend.far(baseFar, Ring4SkyState.mistFar() * (sky ? MIST_SKY_FACTOR : 1.0F), mist);
            newFar = Math.min(newFar, mistFar);
            if (!sky) {
                newNear = Math.min(newNear, FogBlend.near(baseNear, mistFar, mist));
            }
        }
        if (newFar == far && newNear == near) {
            return;
        }
        event.setFarPlaneDistance(newFar);
        event.setNearPlaneDistance(Math.min(newNear, newFar));
        // Applied only when cancelled.
        event.setCanceled(true);
    }

    private static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (Ring4SkyState.ring4() <= 0.0F || !RingWeatherRenderer.clearView(event.getCamera())
                || !(event.getCamera().getEntity().level() instanceof ClientLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        float partialTick = (float) event.getPartialTick();
        float[] rgb = {event.getRed(), event.getGreen(), event.getBlue()};
        Ring4Dusk.fogColor(rgb, level.getTimeOfDay(partialTick));
        float light = Math.max(MIN_LIGHT, RingWeatherRenderer.daylight(level, partialTick));
        float blight = blightAmount() * BLIGHT_COLOUR_SHARE;
        if (blight > 0.0F) {
            Vec3 haze = Vec3.fromRGB24(BLIGHT_COLOUR);
            towards(rgb, (float) haze.x * light, (float) haze.y * light, (float) haze.z * light, blight);
        }
        float mist = mistAmount() * MIST_COLOUR_SHARE;
        if (mist > 0.0F) {
            towards(rgb, Ring4SkyState.mistRed() * light, Ring4SkyState.mistGreen() * light, Ring4SkyState.mistBlue() * light, mist);
        }
        event.setRed(rgb[0]);
        event.setGreen(rgb[1]);
        event.setBlue(rgb[2]);
    }

    /**
     * The sky colour with the blighted haze and the low mist (a part of the fog colour's amounts), called by
     * {@code ClientLevelSkyColorMixin} before the weather; {@code colour} itself when nothing changes.
     */
    public static Vec3 skyColor(ClientLevel level, Vec3 colour, float partialTick) {
        float blight = blightAmount() * BLIGHT_COLOUR_SHARE * BLIGHT_SKY_SHARE;
        float mist = mistAmount() * MIST_SKY_SHARE;
        if (blight <= 0.0F && mist <= 0.0F) {
            return colour;
        }
        float light = Math.max(MIN_LIGHT, RingWeatherRenderer.daylight(level, partialTick));
        float[] rgb = {(float) colour.x, (float) colour.y, (float) colour.z};
        if (blight > 0.0F) {
            Vec3 haze = Vec3.fromRGB24(BLIGHT_COLOUR);
            towards(rgb, (float) haze.x * light, (float) haze.y * light, (float) haze.z * light, blight);
        }
        if (mist > 0.0F) {
            towards(rgb, Ring4SkyState.mistRed() * light, Ring4SkyState.mistGreen() * light, Ring4SkyState.mistBlue() * light, mist);
        }
        return new Vec3(rgb[0], rgb[1], rgb[2]);
    }

    private static void towards(float[] rgb, float r, float g, float b, float amount) {
        rgb[0] = FogBlend.colour(rgb[0], r, amount);
        rgb[1] = FogBlend.colour(rgb[1], g, amount);
        rgb[2] = FogBlend.colour(rgb[2], b, amount);
    }
}
