package nl.timtoys.corruptedadventure.worldgen.feature;

import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring4Tunnels;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Package 77, the tunnel net's lining: crystals by band, ores, claw marks in side walls, tracks on the floors. */
class TunnelLiningFeatureTest {
    @Test
    void theCrystalFollowsTheBand() {
        assertEquals(TunnelLiningFeature.Lining.TOXIC_CRYSTAL, TunnelLiningFeature.lining(Ring4Tunnels.Fill.DRY_AIR, true, 20, 0.0F));
        assertEquals(TunnelLiningFeature.Lining.TOXIC_CRYSTAL, TunnelLiningFeature.lining(Ring4Tunnels.Fill.GAS, false, 20, 0.0F));
        assertEquals(TunnelLiningFeature.Lining.TOXIC_CRYSTAL, TunnelLiningFeature.lining(Ring4Tunnels.Fill.POOL, false, 20, 0.0F));
        assertEquals(TunnelLiningFeature.Lining.AQUAMARINE, TunnelLiningFeature.lining(Ring4Tunnels.Fill.WATER, true, 20, 0.0F));
        assertEquals(TunnelLiningFeature.Lining.AQUAMARINE, TunnelLiningFeature.lining(Ring4Tunnels.Fill.DOME_AIR, true, 20, 0.0F));
    }

    /** Jade only deep in the flooded net; claw marks only in side walls; the rest stays rock. */
    @Test
    void jadeDeepAndClawsInTheSideWalls() {
        float afterOres = TunnelLiningFeature.AQUAMARINE + TunnelLiningFeature.TIDESILVER + 0.001F;
        assertEquals(TunnelLiningFeature.Lining.JADE, TunnelLiningFeature.lining(Ring4Tunnels.Fill.WATER, true, -10, afterOres));
        assertTrue(TunnelLiningFeature.lining(Ring4Tunnels.Fill.WATER, true, 10, afterOres) != TunnelLiningFeature.Lining.JADE);
        assertTrue(TunnelLiningFeature.lining(Ring4Tunnels.Fill.DRY_AIR, true, -10, TunnelLiningFeature.TOXIC_CRYSTAL
                + TunnelLiningFeature.TIDESILVER + 0.001F) != TunnelLiningFeature.Lining.JADE, "no jade in the dry net");
        float claw = TunnelLiningFeature.TOXIC_CRYSTAL + TunnelLiningFeature.TIDESILVER + 0.001F;
        assertEquals(TunnelLiningFeature.Lining.CLAW_MARKS, TunnelLiningFeature.lining(Ring4Tunnels.Fill.DRY_AIR, true, 20, claw));
        assertEquals(TunnelLiningFeature.Lining.NONE, TunnelLiningFeature.lining(Ring4Tunnels.Fill.DRY_AIR, false, 20, claw), "not in a floor");
        assertEquals(TunnelLiningFeature.Lining.NONE, TunnelLiningFeature.lining(Ring4Tunnels.Fill.DRY_AIR, true, 20, 0.5F));
    }

    /** Dry floors: trails, bones, nests; flooded floors only bones; nothing elsewhere. */
    @Test
    void tracksOnTheFloors() {
        assertTrue(TunnelLiningFeature.track(true, false, 0.0F, 0.2F).getBlock().getDescriptionId().contains("slime_trail"));
        assertTrue(TunnelLiningFeature.track(true, false, TunnelLiningFeature.SLIME_TRAIL + 0.001F, 0.2F).getBlock().getDescriptionId().contains("bone_block"));
        assertTrue(TunnelLiningFeature.track(true, false, TunnelLiningFeature.SLIME_TRAIL + TunnelLiningFeature.BONES + 0.001F, 0.2F)
                .getBlock().getDescriptionId().contains("creature_nest"));
        assertNull(TunnelLiningFeature.track(true, false, 0.9F, 0.2F));
        assertTrue(TunnelLiningFeature.track(false, true, 0.0F, 0.7F).getBlock().getDescriptionId().contains("bone_block"));
        assertNull(TunnelLiningFeature.track(false, true, TunnelLiningFeature.BONES + 0.001F, 0.7F));
        assertNull(TunnelLiningFeature.track(false, false, 0.0F, 0.7F), "gas, pools and domes get no tracks");
    }

    /** The position hash spreads evenly enough for the small chances. */
    @Test
    void thePositionHashIsEven() {
        Map<Integer, Integer> buckets = new HashMap<>();
        int n = 0;
        for (int x = 0; x < 64; x++) {
            for (int y = -20; y < 20; y++) {
                for (int z = 0; z < 16; z++) {
                    float u = TunnelLiningFeature.unit(x, y, z, 0);
                    assertTrue(u >= 0.0F && u < 1.0F);
                    buckets.merge((int) (u * 10), 1, Integer::sum);
                    n++;
                }
            }
        }
        for (int b = 0; b < 10; b++) {
            double share = buckets.getOrDefault(b, 0) / (double) n;
            assertTrue(Math.abs(share - 0.1) < 0.02, "bucket " + b + ": " + share);
        }
    }
}
