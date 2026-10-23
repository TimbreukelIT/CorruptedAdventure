package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * The fog of Nether ring 5 (package 98, V98-7 A): white and far. The biome's white fog colour reaches
 * {@link #FAR_SHARE} of the view (at most {@link #MAX_VIEW} blocks), near at {@link #NEAR_SHARE} of that, instead of
 * vanilla's short Nether fog, so the grids of columns dissolve into the white: endless and empty. No pulse. Eases in over
 * ~2.5 s on the biome tag {@code is_nether_ring_5}, like {@link NetherRing4Fog}. Client only; water, lava and blindness
 * keep vanilla's fog.
 */
public final class NetherRing5Fog {
    static final float FAR_SHARE = 0.8F;
    static final float NEAR_SHARE = 0.4F;
    static final float MAX_VIEW = 192.0F;
    /** Change of the blend per tick: from 0 to 1 in 2.5 s. */
    private static final float EASE = 0.02F;

    private static float presence;

    private NetherRing5Fog() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NetherRing5Fog::onClientTick);
        // Also when NetherRing4Fog (registered before, same priority) cancelled the event: between the two rings both
        // fade, each from the planes the other left.
        gameBus.addListener(EventPriority.NORMAL, true, NetherRing5Fog::onRenderFog);
        gameBus.addListener(NetherRing5Fog::onLoggingOut);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        boolean inside = level.dimension() == Level.NETHER && level.getBiome(
                BlockPos.containing(minecraft.gameRenderer.getMainCamera().getPosition())).is(ModTags.Biomes.IS_NETHER_RING_5);
        presence = SkyExposure.approach(presence, inside ? 1.0F : 0.0F, EASE);
    }

    /** The far and near planes for a view distance (blocks), {far, near}. Pure. */
    static float[] planes(float view) {
        float far = Math.min(view, MAX_VIEW) * FAR_SHARE;
        return new float[] {far, far * NEAR_SHARE};
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        if (presence <= 0.0F || !RingWeatherRenderer.clearView(event.getCamera())
                || event.getCamera().getEntity().level().dimension() != Level.NETHER) {
            return;
        }
        float[] planes = planes(Minecraft.getInstance().gameRenderer.getRenderDistance());
        float far = Math.max(event.getFarPlaneDistance(), planes[0]);
        float near = event.getMode() == FogRenderer.FogMode.FOG_SKY ? event.getNearPlaneDistance()
                : Math.max(event.getNearPlaneDistance(), planes[1]);
        event.setFarPlaneDistance(Mth.lerp(presence, event.getFarPlaneDistance(), far));
        event.setNearPlaneDistance(Mth.lerp(presence, event.getNearPlaneDistance(), near));
        // Applied only when cancelled.
        event.setCanceled(true);
    }

    /** The next world starts without the fog of this one. */
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        presence = 0.0F;
    }
}
