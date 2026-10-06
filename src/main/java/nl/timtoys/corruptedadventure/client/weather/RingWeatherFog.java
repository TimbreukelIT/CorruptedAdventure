package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Fog, fog colour and sky colour steps shared by the ring 3 renderers, on top of {@link FogBlend}. Fog and fog colour
 * only with a {@link RingWeatherRenderer#clearView clear view} (water, lava and blindness keep vanilla's fog).
 */
final class RingWeatherFog {
    private RingWeatherFog() {
    }

    /** Far plane towards {@code targetFar} (terrain and sky alike), near plane towards 0. */
    static void towards(ViewportEvent.RenderFog event, float targetFar, float amount) {
        if (amount <= 0.0F || !RingWeatherRenderer.clearView(event.getCamera())) {
            return;
        }
        float far = FogBlend.far(event.getFarPlaneDistance(), targetFar, amount);
        event.setNearPlaneDistance(FogBlend.near(event.getNearPlaneDistance(), far, amount));
        event.setFarPlaneDistance(far);
        // Applied only when cancelled.
        event.setCanceled(true);
    }

    /** Terrain fog only (the sky keeps its own colour): far plane × {@code factor}. */
    static void terrainScaled(ViewportEvent.RenderFog event, float factor, float amount) {
        if (event.getMode() == FogRenderer.FogMode.FOG_TERRAIN) {
            towards(event, event.getFarPlaneDistance() * factor, amount);
        }
    }

    /**
     * Fog colour towards {@code r, g, b} × the daylight, which never drops below {@code minLight} (a glow that stays
     * at night, or a darkness that does not need the night).
     */
    static void colour(ViewportEvent.ComputeFogColor event, float r, float g, float b, float minLight, float amount) {
        if (amount <= 0.0F || !RingWeatherRenderer.clearView(event.getCamera())
                || !(event.getCamera().getEntity().level() instanceof ClientLevel level)) {
            return;
        }
        float light = light(level, (float) event.getPartialTick(), minLight);
        event.setRed(FogBlend.colour(event.getRed(), r * light, amount));
        event.setGreen(FogBlend.colour(event.getGreen(), g * light, amount));
        event.setBlue(FogBlend.colour(event.getBlue(), b * light, amount));
    }

    /** Sky colour towards {@code r, g, b} × the daylight (at least {@code minLight}). */
    static Vec3 sky(ClientLevel level, Vec3 colour, float partialTick, float r, float g, float b, float minLight, float amount) {
        if (amount <= 0.0F) {
            return colour;
        }
        float light = light(level, partialTick, minLight);
        float clamped = Mth.clamp(amount, 0.0F, 1.0F);
        return new Vec3(Mth.lerp(clamped, colour.x, r * light), Mth.lerp(clamped, colour.y, g * light),
                Mth.lerp(clamped, colour.z, b * light));
    }

    private static float light(ClientLevel level, float partialTick, float minLight) {
        return Math.max(minLight, RingWeatherRenderer.daylight(level, partialTick));
    }
}
