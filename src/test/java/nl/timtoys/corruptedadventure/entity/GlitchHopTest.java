package nl.timtoys.corruptedadventure.entity;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where a glitch arrow's hop may land in the ring layout: never in the void, in ring 5 never outward. */
class GlitchHopTest {
    private final RingLayout layout = new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), RingLayout.noiseSeed(20260930L));

    private boolean allows(ResourceKey<Level> dimension, int fromX, int fromZ, int x, int z) {
        return GlitchHop.RingRule.of(layout, dimension, fromX, fromZ).allows(x, z);
    }

    @Test
    void outsideRingFiveEveryDirectionIsAllowed() {
        assertEquals(Ring.RING_3, layout.sampleOverworld(5000.5, 0.5).ring());
        assertTrue(allows(Level.OVERWORLD, 5000, 0, 5005, 0));
        assertTrue(allows(Level.OVERWORLD, 5000, 0, 4995, 0));
    }

    @Test
    void inRingFiveTheHopNeverGoesTowardTheVoid() {
        int checked = 0;
        for (int x = 8200; x < 9400; x += 7) {
            if (layout.sampleOverworld(x + 0.5, 0.5).ring() != Ring.RING_5
                    || layout.sampleOverworld(x + 5.5, 0.5).ring() != Ring.RING_5) {
                continue;
            }
            boolean outward = layout.effectiveDistance(x + 5.5, 0.5) > layout.effectiveDistance(x + 0.5, 0.5);
            assertEquals(!outward, allows(Level.OVERWORLD, x, 0, x + 5, 0), "x " + x);
            assertEquals(outward, allows(Level.OVERWORLD, x + 5, 0, x, 0), "x " + x);
            checked++;
        }
        assertTrue(checked > 100, "enough of ring 5 checked: " + checked);
    }

    @Test
    void neverIntoTheVoid() {
        assertEquals(Ring.VOID, layout.sampleOverworld(10100.5, 0.5).ring());
        assertFalse(allows(Level.OVERWORLD, 10104, 0, 10100, 0), "not even inward within the void");
    }

    @Test
    void theNetherCountsInOverworldBlocks() {
        // Nether 2200 is overworld 8800 (ring 5); Nether 2525 is 10100 (the void).
        assertEquals(Ring.RING_5, layout.sample(Level.NETHER, 2200.5, 0.5).ring());
        assertFalse(allows(Level.NETHER, 2500, 0, 2525, 0));
        boolean outward = layout.effectiveDistance(2205.5 * 4, 2.0) > layout.effectiveDistance(2200.5 * 4, 2.0);
        assertEquals(!outward, allows(Level.NETHER, 2200, 0, 2205, 0));
    }
}
