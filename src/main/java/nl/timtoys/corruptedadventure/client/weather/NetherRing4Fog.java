package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * The fog of Nether ring 4 (change 80, design: "pulserend kristallicht", "ver zicht"): the near white-beige fog of the
 * biome data pulses softly towards the crystals' light, strongest among the crystal spires and in the geodes, and it
 * reaches further than vanilla's short Nether fog. Both ease in over a few seconds when the camera enters Nether ring 4
 * (biome tag {@code is_nether_ring_4}), so crossing in from Nether ring 3 never jumps. Client only; water, lava and
 * blindness keep vanilla's fog.
 */
public final class NetherRing4Fog {
    /** One pulse of the crystal light, ticks. */
    static final int PULSE_TICKS = 160;
    /** How far towards {@link #CRYSTAL} the fog goes at the top of a pulse: dunes, spires and geodes. */
    static final float DUNES_PULSE = 0.08F;
    static final float CRYSTAL_PULSE = 0.18F;
    /** Pale, slightly cool crystal light. */
    private static final float CRYSTAL_R = 0xF4 / 255.0F;
    private static final float CRYSTAL_G = 0xF2 / 255.0F;
    private static final float CRYSTAL_B = 0xFF / 255.0F;
    /** Far plane as a share of the view distance (vanilla's Nether: half of at most 192 blocks), near as a share of far. */
    static final float FAR_SHARE = 0.85F;
    static final float NEAR_SHARE = 0.3F;
    /** Change of the blend per tick: from 0 to 1 in 2.5 s. */
    private static final float EASE = 0.02F;

    /** 0 outside Nether ring 4, 1 well inside; eased per tick. */
    private static float presence;
    private static float pulseStrength;

    private NetherRing4Fog() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NetherRing4Fog::onClientTick);
        gameBus.addListener(NetherRing4Fog::onRenderFog);
        gameBus.addListener(NetherRing4Fog::onComputeFogColor);
        gameBus.addListener(NetherRing4Fog::onLoggingOut);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        float target = 0.0F;
        if (level.dimension() == Level.NETHER) {
            Holder<Biome> biome = level.getBiome(BlockPos.containing(minecraft.gameRenderer.getMainCamera().getPosition()));
            if (biome.is(ModTags.Biomes.IS_NETHER_RING_4)) {
                target = 1.0F;
                // Eased too, so the border dunes | spires never pops.
                pulseStrength = SkyExposure.approach(pulseStrength, biome.is(ModBiomes.BONE_DUNES) ? DUNES_PULSE : CRYSTAL_PULSE,
                        EASE * CRYSTAL_PULSE);
            }
        }
        presence = SkyExposure.approach(presence, target, EASE);
    }

    /** The next world starts without the fog of this one. */
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        presence = 0.0F;
        pulseStrength = 0.0F;
    }

    private static void onRenderFog(ViewportEvent.RenderFog event) {
        if (presence <= 0.0F || !RingWeatherRenderer.clearView(event.getCamera())
                || event.getCamera().getEntity().level().dimension() != Level.NETHER) {
            return;
        }
        float view = Math.min(Minecraft.getInstance().gameRenderer.getRenderDistance(), 192.0F);
        float far = Math.max(event.getFarPlaneDistance(), view * FAR_SHARE);
        float near = Math.max(event.getNearPlaneDistance(), far * NEAR_SHARE);
        if (event.getMode() == FogRenderer.FogMode.FOG_SKY) {
            near = event.getNearPlaneDistance();
        }
        event.setFarPlaneDistance(Mth.lerp(presence, event.getFarPlaneDistance(), far));
        event.setNearPlaneDistance(Mth.lerp(presence, event.getNearPlaneDistance(), near));
        // Applied only when cancelled.
        event.setCanceled(true);
    }

    private static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (presence <= 0.0F || !RingWeatherRenderer.clearView(event.getCamera())
                || !(event.getCamera().getEntity().level() instanceof ClientLevel level) || level.dimension() != Level.NETHER) {
            return;
        }
        float time = (level.getGameTime() % PULSE_TICKS + (float) event.getPartialTick()) / PULSE_TICKS;
        float pulse = 0.5F + 0.5F * Mth.sin(time * Mth.TWO_PI);
        float amount = presence * pulseStrength * pulse;
        event.setRed(Mth.lerp(amount, event.getRed(), CRYSTAL_R));
        event.setGreen(Mth.lerp(amount, event.getGreen(), CRYSTAL_G));
        event.setBlue(Mth.lerp(amount, event.getBlue(), CRYSTAL_B));
    }
}
