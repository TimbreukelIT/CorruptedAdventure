package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandFieldTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code corruptedadventure:before_split} on the test layout (zone 3→4 of 256, split at 60): a steam_fens pool is kept
 * only where nothing within its reach lies on the descent of verdant_falls; its shortcut agrees with the full look.
 */
class BeforeSplitFilterTest {
    private static final RingLayout LAYOUT = IslandFieldTest.LAYOUT;
    private static final BeforeSplitFilter POOLS = BeforeSplitFilter.of(Ring.RING_3, IslandField.VERDANT_EDGE_SHARE, SteamFensFeatures.MUD_POOL_REACH);

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

    /**
     * Every block a boiling pool changes lies within {@link SteamFensFeatures#BOILING_POOL_REACH} of its origin, also
     * where {@code PoolLayout} shifts the pool into its chunk (origins 1–14 into the chunk, grid margin 1), and more than
     * a mud pool's reach away for some: the filter must look that far. Flat ground, the configured radius 1.5–3.5, edge
     * jitter 0.35 and rim 1.
     */
    @Test
    void aBoilingPoolStaysWithinItsReach() {
        PoolLayout.Terrain flat = new PoolLayout.Terrain() {
            @Override
            public int surface(int x, int z) {
                return 70;
            }

            @Override
            public boolean ground(int x, int y, int z) {
                return true;
            }

            @Override
            public boolean solid(int x, int y, int z) {
                return y <= 70;
            }
        };
        Random random = new Random(4242L);
        double farthest = 0.0;
        int layouts = 0;
        for (int trial = 0; trial < 12; trial++) {
            float[] edge = new float[PoolLayout.EDGE_POINTS];
            for (int i = 0; i < edge.length; i++) {
                edge[i] = trial == 0 ? 1.0F : 1.0F - 0.35F * random.nextFloat();
            }
            for (float radius = 1.5F; radius <= 3.5F; radius += 0.5F) {
                for (int oz = 1; oz <= 14; oz++) {
                    for (int ox = 1; ox <= 14; ox++) {
                        Optional<PoolLayout.Layout> layout = PoolLayout.build(flat, 0, 0, ox, oz,
                                new PoolLayout.Params(radius, edge, 1, 1, 1));
                        if (layout.isEmpty()) {
                            continue;
                        }
                        layouts++;
                        for (List<BlockPos> blocks : List.of(layout.get().carve(),
                                layout.get().water(), layout.get().rim())) {
                            for (BlockPos pos : blocks) {
                                farthest = Math.max(farthest, Math.hypot(pos.getX() - ox, pos.getZ() - oz));
                            }
                        }
                    }
                }
            }
        }
        System.out.printf("boiling pools: %d layouts, farthest block %.2f from the origin%n", layouts, farthest);
        assertTrue(layouts > 1000, "layouts " + layouts);
        assertTrue(farthest <= SteamFensFeatures.BOILING_POOL_REACH, "a boiling pool reaches " + farthest);
        assertTrue(farthest > SteamFensFeatures.MUD_POOL_REACH, "a boiling pool reaches " + farthest);
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
        int reach = SteamFensFeatures.MUD_POOL_REACH;
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
