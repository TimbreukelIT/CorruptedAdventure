package nl.timtoys.corruptedadventure.ring;

import net.minecraft.core.QuartPos;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingColumnCacheTest {
    private static final RingSettings DEFAULTS = new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES);
    private static final RingLayout LAYOUT = new RingLayout(DEFAULTS, 12345L);

    @Test
    void cachedResultsEqualTheLayoutIncludingEvictions() {
        RingColumnCache cache = new RingColumnCache();
        // Two passes over an area larger than the cache, so slots get hits, misses and evictions.
        for (int pass = 0; pass < 2; pass++) {
            for (int x = 9380; x < 9620; x += 3) {
                for (int z = -120; z < 120; z += 7) {
                    assertEquals(LAYOUT.sampleOverworld(x, z), cache.position(LAYOUT, 1, x, z));
                    assertEquals(LAYOUT.voidEdgeProgress(x, z), cache.voidEdgeProgress(LAYOUT, 1, x, z));
                    assertEquals(LAYOUT.effectiveDistance(x, z), cache.effectiveDistance(LAYOUT, 1, x, z));
                }
            }
        }
    }

    @Test
    void netherAndOverworldColumnsDoNotMixUp() {
        RingColumnCache cache = new RingColumnCache();
        for (int x = 2300; x < 2420; x++) {
            for (int z = -40; z < 40; z += 3) {
                assertEquals(LAYOUT.sampleOverworld(x * 4.0, z * 4.0), cache.position(LAYOUT, 4, x, z));
                assertEquals(LAYOUT.sampleOverworld(x, z), cache.position(LAYOUT, 1, x, z));
            }
        }
    }

    /**
     * The climate in the router (a {@code ring_blend} reading this cache at block columns) and {@code RingBiomeSource}
     * (sampling the layout at {@code QuartPos.toBlock}) see the same ring at every biome sample across every boundary,
     * overworld and Nether: a {@code switch_at} boundary switches exactly where the biome source does.
     */
    @Test
    void climateAndBiomeSourceSeeTheSameRingAtEveryQuart() {
        RingColumnCache cache = new RingColumnCache();
        int switches = 0;
        for (int radius : CommonConfig.DEFAULT_OUTER_RADII) {
            for (int scale : new int[]{1, RingLayout.NETHER_SCALE}) {
                var dimension = scale == 1 ? Level.OVERWORLD : Level.NETHER;
                int centre = QuartPos.fromBlock(radius / scale), reach = QuartPos.fromBlock(250 / scale) + 2;
                for (int qx = centre - reach; qx <= centre + reach; qx++) {
                    for (int qz = -60; qz < 60; qz += 7) {
                        int x = QuartPos.toBlock(qx), z = QuartPos.toBlock(qz);
                        RingPosition expected = LAYOUT.sample(dimension, x, z);
                        assertEquals(expected.ring(), cache.position(LAYOUT, scale, x, z).ring(), dimension + " " + x + " " + z);
                        switches += expected.inTransition() && expected.blend() >= 0.5 ? 1 : 0;
                    }
                }
            }
        }
        assertTrue(switches > 0, "the samples cross the middle of the zones");
    }

    @Test
    void anotherLayoutClearsTheCache() {
        RingColumnCache cache = new RingColumnCache();
        RingLayout other = new RingLayout(DEFAULTS, 999L);
        for (int x = 3380; x < 3620; x += 11) {
            cache.position(LAYOUT, 1, x, 0);
            assertEquals(other.sampleOverworld(x, 0), cache.position(other, 1, x, 0));
        }
    }
}
