package nl.timtoys.corruptedadventure.ring;

import nl.timtoys.corruptedadventure.config.RingSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RingColumnCacheTest {
    private static final RingSettings DEFAULTS = new RingSettings(List.of(1500, 3500, 5500, 7500, 9500), 10000, 96, 128);
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
