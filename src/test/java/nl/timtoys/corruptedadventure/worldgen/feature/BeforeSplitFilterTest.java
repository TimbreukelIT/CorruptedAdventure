package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandFieldTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code corruptedadventure:before_split} on the test layout (zone 3→4 of 256, split at 60): a steam_fens pool is kept
 * only where nothing within its reach lies on the descent of verdant_falls; its shortcut agrees with the full look.
 */
class BeforeSplitFilterTest {
    private static final RingLayout LAYOUT = IslandFieldTest.LAYOUT;
    private static final BeforeSplitFilter POOLS = BeforeSplitFilter.of(Ring.RING_3, IslandField.VERDANT_EDGE_SHARE, SteamFensFeatures.POOL_REACH);

    @Test
    void keepsOnlyWhatStaysBeforeTheSplit() {
        int kept = 0;
        int dropped = 0;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.0071) {
            for (double distance = 5600.0; distance < 6200.0; distance += 3.0) {
                int x = (int) Math.floor(Math.cos(angle) * distance);
                int z = (int) Math.floor(Math.sin(angle) * distance);
                boolean clear = POOLS.clear(LAYOUT, Level.OVERWORLD, x, z);
                assertEquals(fullLook(x, z), clear, "at " + x + " " + z);
                kept += clear ? 1 : 0;
                dropped += clear ? 0 : 1;
            }
        }
        assertTrue(kept > 0 && dropped > 0, "kept " + kept + ", dropped " + dropped);
    }

    /** Right before the split it drops what a pool would reach past it, a pool's width further in it keeps it. */
    @Test
    void theReachCounts() {
        int samples = 0;
        int near = 0;
        int far = 0;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.013) {
            samples++;
            int[] closeIn = nearSplit(angle, 1.5);
            near += POOLS.clear(LAYOUT, Level.OVERWORLD, closeIn[0], closeIn[1]) ? 0 : 1;
            int[] wellIn = nearSplit(angle, 40.0);
            far += POOLS.clear(LAYOUT, Level.OVERWORLD, wellIn[0], wellIn[1]) ? 1 : 0;
        }
        // Almost everywhere: where the boundary noise flattens the zone's axis, 6 blocks may stay within 1.5 of the split.
        assertTrue(near >= samples - samples / 100, "dropped right before the split: " + near + " of " + samples);
        assertEquals(samples, far, "kept well before it");
    }

    @Test
    void theCodecChecksTheSplit() {
        assertTrue(BeforeSplitFilter.CODEC.codec().parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"ring\":\"ring_3\",\"at\":0.234375,\"reach\":6}")).result().isPresent());
        assertFalse(BeforeSplitFilter.CODEC.codec().parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"ring\":\"ring_3\",\"at\":1.0,\"reach\":6}")).result().isPresent());
        assertFalse(BeforeSplitFilter.CODEC.codec().parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"ring\":\"void\",\"at\":0.5,\"reach\":6}")).result().isPresent());
    }

    /** The column on the ray at {@code angle} whose blend lies {@code blocks} before the split. */
    private static int[] nearSplit(double angle, double blocks) {
        double lo = 5000.0;
        double hi = 6400.0;
        double target = IslandField.VERDANT_EDGE_SHARE - blocks / 256.0;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2.0;
            RingPosition p = LAYOUT.sampleOverworld(Math.cos(angle) * mid, Math.sin(angle) * mid);
            double progress = p.inTransition() && p.inner() == Ring.RING_3 ? p.blend() : p.ring().ordinal() > Ring.RING_3.ordinal() ? 2.0 : -1.0;
            if (progress < target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return new int[]{(int) Math.floor(Math.cos(angle) * lo), (int) Math.floor(Math.sin(angle) * lo)};
    }

    /** Every column within the reach before the split, without the shortcut. */
    private static boolean fullLook(int x, int z) {
        int reach = SteamFensFeatures.POOL_REACH;
        for (int dz = -reach; dz <= reach; dz++) {
            for (int dx = -reach; dx <= reach; dx++) {
                if (dx * dx + dz * dz > reach * reach) {
                    continue;
                }
                RingPosition p = LAYOUT.sample(Level.OVERWORLD, x + dx, z + dz);
                boolean past = p.inTransition()
                        ? p.inner() == Ring.RING_3 ? p.blend() >= IslandField.VERDANT_EDGE_SHARE : p.inner().ordinal() > Ring.RING_3.ordinal()
                        : p.inner().ordinal() > Ring.RING_3.ordinal();
                if (past) {
                    return false;
                }
            }
        }
        return true;
    }
}
