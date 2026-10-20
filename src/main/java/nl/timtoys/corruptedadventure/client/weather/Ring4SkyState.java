package nl.timtoys.corruptedadventure.client.weather;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.timtoys.corruptedadventure.client.ClientRingLayout;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.weather.RingWeatherMix;

import java.util.List;

/**
 * Where the camera stands in ring 4, once per client tick, for the sky and fog hooks of the sparring of 2026-10-20
 * ({@link Ring4Dusk}, {@link Ring4Clouds}, {@link Ring4Fog}): ring 4's weight (smooth over the zones 3→4 and 4→5, as
 * the ring weather mixes), the band (0 mythical, 1 blighted, from the synced layout like the server's band), how far
 * the camera lies towards ring 5 and the low mist of the biome under it. Nothing here is synced: biome, position and
 * layout are known to the client. Render-thread only (client tick and render run on the main thread).
 */
public final class Ring4SkyState {
    /** The band line lies at 60 % of ring 4 on average ({@code Ring4Band}); the blighted haze thickens beyond it. */
    static final double BAND_LINE = 0.6;
    /** Mist presence and look ease over ~3 s, so the edge of a mist biome never pops. */
    static final float MIST_EASE = 1.0F / 60.0F;
    private static final float LOOK_EASE = 0.05F;

    /**
     * The low mist per biome (C2-1): fog far plane in the open at sea level and its colour. {@code mist_fjords} is the
     * thick grey-white one of the design; the swamps a lighter, paler mist (mythical green-aqua, blighted sickly).
     */
    static final List<Mist> MISTS = List.of(
            new Mist(ModBiomes.MIST_FJORDS, 56.0F, 0xC9D3D8),
            new Mist(ModBiomes.GLOWROOT_SWAMP, 104.0F, 0xC6E2D6),
            new Mist(ModBiomes.PEAT_MOORS, 96.0F, 0xC4C894),
            new Mist(ModBiomes.DROWNED_FOREST, 96.0F, 0xC0C690),
            new Mist(ModBiomes.REED_MARSH, 112.0F, 0xC8CA98));

    private static float ring4;
    private static float blighted;
    private static float towardsRing5;
    private static float cameraY;
    private static float mist;
    private static float mistFar = MISTS.getFirst().far();
    private static float mistRed = 1.0F;
    private static float mistGreen = 1.0F;
    private static float mistBlue = 1.0F;

    private Ring4SkyState() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(Ring4SkyState::onClientTick);
        gameBus.addListener(Ring4SkyState::onLoggingOut);
    }

    /** Ring 4's weight at the camera, 0 outside ring 4 and its zones (and outside the overworld). */
    static float ring4() {
        return ring4;
    }

    /** 0 mythical, 1 blighted, running over the band transition (the {@code Ring4Band} axis mapped to 0–1). */
    static float blighted() {
        return blighted;
    }

    /** 0 on the band line (60 % of ring 4), 1 at ring 4's outer boundary and beyond it in zone 4→5. */
    static float towardsRing5() {
        return towardsRing5;
    }

    static float cameraY() {
        return cameraY;
    }

    /** Eased presence of a mist biome under the camera, 0–1 (without the camera height). */
    static float mist() {
        return mist;
    }

    static float mistFar() {
        return mistFar;
    }

    static float mistRed() {
        return mistRed;
    }

    static float mistGreen() {
        return mistGreen;
    }

    static float mistBlue() {
        return mistBlue;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        RingLayout layout = Level.OVERWORLD.equals(level.dimension()) ? ClientRingLayout.get().orElse(null) : null;
        Mist target = null;
        if (layout == null) {
            ring4 = 0.0F;
        } else {
            RingPosition position = layout.sampleOverworld(camera.x, camera.z);
            ring4 = RingWeatherMix.weight(position, Ring.RING_4);
            if (ring4 > 0.0F) {
                blighted = (float) ((Ring4Band.of(layout).value(camera.x, camera.z) + 1.0) / 2.0);
                towardsRing5 = towardsRing5(position, layout.ringProgress(Ring.RING_4, camera.x, camera.z).orElse(-1.0));
                target = mistAt(level.getBiome(BlockPos.containing(camera.x, Ring4Fog.SEA_LEVEL, camera.z)));
            }
        }
        cameraY = (float) camera.y;
        mist = SkyExposure.approach(mist, target == null ? 0.0F : 1.0F, MIST_EASE);
        if (target != null) {
            // The look follows the biome while the presence stays: crossing fjord → swamp blends instead of jumping.
            mistFar = Mth.lerp(LOOK_EASE, mistFar, target.far());
            mistRed = Mth.lerp(LOOK_EASE, mistRed, target.red());
            mistGreen = Mth.lerp(LOOK_EASE, mistGreen, target.green());
            mistBlue = Mth.lerp(LOOK_EASE, mistBlue, target.blue());
        }
    }

    /**
     * {@code progress} across ring 4 (0–1, negative outside it) → 0 on the band line, 1 at the outer boundary; zone 4→5
     * past the boundary (outside ring 4's own progress) counts as 1.
     */
    static float towardsRing5(RingPosition position, double progress) {
        if (progress < 0.0) {
            return position.inner() == Ring.RING_4 && position.outer() == Ring.RING_5 ? 1.0F : 0.0F;
        }
        return (float) Mth.clamp((progress - BAND_LINE) / (1.0 - BAND_LINE), 0.0, 1.0);
    }

    private static Mist mistAt(Holder<Biome> biome) {
        for (Mist mist : MISTS) {
            if (biome.is(mist.biome())) {
                return mist;
            }
        }
        return null;
    }

    /** The next world starts clear. */
    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ring4 = 0.0F;
        mist = 0.0F;
    }

    /** One biome's low mist: far plane at full presence (blocks) and colour (RGB, before the daylight). */
    record Mist(ResourceKey<Biome> biome, float far, int rgb) {
        float red() {
            return (rgb >> 16 & 0xFF) / 255.0F;
        }

        float green() {
            return (rgb >> 8 & 0xFF) / 255.0F;
        }

        float blue() {
            return (rgb & 0xFF) / 255.0F;
        }
    }
}
