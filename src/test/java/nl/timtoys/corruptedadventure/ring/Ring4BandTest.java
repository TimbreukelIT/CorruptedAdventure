package nl.timtoys.corruptedadventure.ring;

import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ring4BandTest {
    private static final long SEED = 20260930L;

    private static RingLayout layout(long worldSeed) {
        return new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
                CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), RingLayout.noiseSeed(worldSeed));
    }

    private static final RingLayout LAYOUT = layout(SEED);
    private static final Ring4Band BAND = Ring4Band.of(LAYOUT);

    /** Inside 33 % of ring 4 always mythical, outside 87 % always blighted; ring 3 and the zone 3→4 mythical too. */
    @Test
    void innerEdgeIsMythicalAndOuterEdgeBlighted() {
        int checkedInner = 0;
        int checkedOuter = 0;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 360.0) {
            for (int distance = 5800; distance <= 8300; distance += 10) {
                double x = distance * Math.cos(angle);
                double z = distance * Math.sin(angle);
                OptionalDouble progress = LAYOUT.ringProgress(Ring.RING_4, x, z);
                double effective = LAYOUT.effectiveDistance(x, z);
                if (effective < 6000.0 || progress.isPresent() && progress.getAsDouble() < 0.33) {
                    assertEquals(-1.0, BAND.value(x, z), "mythical at " + x + ", " + z);
                    assertFalse(BAND.blighted(x, z));
                    checkedInner++;
                } else if (effective >= 8000.0 || progress.isPresent() && progress.getAsDouble() > 0.87) {
                    assertEquals(1.0, BAND.value(x, z), "blighted at " + x + ", " + z);
                    assertTrue(BAND.blighted(x, z));
                    checkedOuter++;
                }
            }
        }
        assertTrue(checkedInner > 10_000 && checkedOuter > 10_000);
    }

    /** About 60 % of the ring's width is mythical, but with tongues and islets across the line at 60 %. */
    @Test
    void bandsShareTheRingSixtyFortyWithTonguesAndIslets() {
        int blighted = 0;
        int samples = 0;
        boolean tongue = false;
        boolean islet = false;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 720.0) {
            for (double progress = 0.005; progress < 1.0; progress += 0.01) {
                double distance = 6000.0 + 2000.0 * progress;
                double x = distance * Math.cos(angle);
                double z = distance * Math.sin(angle);
                boolean isBlighted = BAND.blighted(x, z);
                blighted += isBlighted ? 1 : 0;
                samples++;
                tongue |= isBlighted && progress < 0.5;
                islet |= !isBlighted && progress > 0.7;
            }
        }
        double share = blighted / (double) samples;
        assertTrue(share > 0.35 && share < 0.45, "blighted share " + share);
        assertTrue(tongue, "no blighted tongue reaches into the mythical band");
        assertTrue(islet, "no mythical islet in the blighted band");
    }

    /**
     * Across the line the value runs from −1 to 1 over about 64 blocks: measured perpendicular to the line (2 / the
     * value's gradient), wherever it is between the two; the noise makes it vary.
     */
    @Test
    void transitionIsAboutSixtyFourBlocks() {
        List<Double> widths = new ArrayList<>();
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 180.0) {
            for (int distance = 6000; distance <= 8000; distance += 2) {
                double x = distance * Math.cos(angle);
                double z = distance * Math.sin(angle);
                double value = BAND.value(x, z);
                if (value > -0.9 && value < 0.9) {
                    double dx = (BAND.value(x + 0.5, z) - BAND.value(x - 0.5, z));
                    double dz = (BAND.value(x, z + 0.5) - BAND.value(x, z - 0.5));
                    widths.add(2.0 / Math.sqrt(dx * dx + dz * dz));
                }
            }
        }
        Collections.sort(widths);
        double median = widths.get(widths.size() / 2);
        assertTrue(median >= 48 && median <= 80, "median transition width " + median);
    }

    @Test
    void sameSeedSameBandOtherSeedOtherBand() {
        Ring4Band again = Ring4Band.of(layout(SEED));
        Ring4Band other = Ring4Band.of(layout(SEED + 1));
        boolean differs = false;
        for (int x = -8000; x <= 8000; x += 97) {
            int z = (int) Math.round(Math.sqrt(Math.max(0, 7200.0 * 7200.0 - (double) x * x)));
            assertEquals(BAND.value(x, z), again.value(x, z));
            differs |= BAND.value(x, z) != other.value(x, z);
        }
        assertTrue(differs);
    }

    @Test
    void blightedIsTheMiddleOfTheTransition() {
        for (int x = 6000; x <= 8000; x += 3) {
            for (int z = -400; z <= 400; z += 50) {
                assertEquals(BAND.value(x, z) >= 0.0, BAND.blighted(x, z), x + ", " + z);
            }
        }
    }
}
