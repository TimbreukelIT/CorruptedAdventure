package nl.timtoys.corruptedadventure.ring;

import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ring5BandTest {
    private static final long SEED = 20260930L;

    static RingLayout layout(long worldSeed) {
        return new RingLayout(new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
                CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), RingLayout.noiseSeed(worldSeed));
    }

    private static final RingLayout LAYOUT = layout(SEED);
    private static final Ring5Band BAND = Ring5Band.of(LAYOUT);

    /**
     * V1-A: the line moves at most 15 % of the ring from its middle, so inside 35 % of ring 5 always inner and outside
     * 65 % always outer; a further 100 / 1700 · SLOPE in, the value is fully −1 or 1. Ring 4 (zone 4→5) is inner, the
     * ring's edge 9.700 and the zone 5→void outer.
     */
    @Test
    void innerEdgeIsInnerAndOuterEdgeOuter() {
        double full = Ring5Band.SLOPE * Ring5Band.TRANSITION / 2.0 / 1700.0;
        int checkedInner = 0;
        int checkedOuter = 0;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 360.0) {
            for (int distance = 7700; distance <= 10000; distance += 10) {
                double x = distance * Math.cos(angle);
                double z = distance * Math.sin(angle);
                double progress = (LAYOUT.effectiveDistance(x, z) - 8000.0) / 1700.0;
                if (progress < 0.35) {
                    assertTrue(BAND.inner(x, z), "inner at " + x + ", " + z);
                    assertFalse(BAND.outer(x, z));
                    if (progress < 0.35 - full) {
                        assertEquals(-1.0, BAND.value(x, z), "fully inner at " + x + ", " + z);
                    }
                    checkedInner++;
                } else if (progress > 0.65) {
                    assertTrue(BAND.outer(x, z), "outer at " + x + ", " + z);
                    if (progress > 0.65 + full) {
                        assertEquals(1.0, BAND.value(x, z), "fully outer at " + x + ", " + z);
                    }
                    checkedOuter++;
                }
            }
        }
        assertTrue(checkedInner > 10_000 && checkedOuter > 10_000);
    }

    /** About half of the ring's width is inner (sparring round 2), with tongues of each band reaching past the middle. */
    @Test
    void bandsShareTheRingHalfAndHalfWithTongues() {
        int outer = 0;
        int samples = 0;
        boolean outerTongue = false;
        boolean innerTongue = false;
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 720.0) {
            for (double progress = 0.005; progress < 1.0; progress += 0.01) {
                double distance = 8000.0 + 1700.0 * progress;
                double x = distance * Math.cos(angle);
                double z = distance * Math.sin(angle);
                boolean isOuter = BAND.outer(x, z);
                outer += isOuter ? 1 : 0;
                samples++;
                outerTongue |= isOuter && progress < 0.4;
                innerTongue |= !isOuter && progress > 0.6;
            }
        }
        double share = outer / (double) samples;
        assertTrue(share > 0.45 && share < 0.55, "outer share " + share);
        assertTrue(outerTongue, "no outer tongue reaches deep into the inner band");
        assertTrue(innerTongue, "no inner tongue reaches deep into the outer band");
    }

    /**
     * Across the line the value runs from −1 to 1 over about 200 blocks: measured perpendicular to the line (2 / the
     * value's gradient), wherever it is between the two; the noise makes it vary.
     */
    @Test
    void transitionIsAboutTwoHundredBlocks() {
        List<Double> widths = new ArrayList<>();
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += Math.PI / 180.0) {
            for (int distance = 8000; distance <= 9700; distance += 2) {
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
        System.out.println("Ring5BandTest median transition width " + median + " (SLOPE " + Ring5Band.SLOPE + ")");
        assertTrue(median >= 170 && median <= 230, "median transition width " + median);
    }

    @Test
    void sameSeedSameBandOtherSeedOtherBand() {
        Ring5Band again = Ring5Band.of(layout(SEED));
        Ring5Band other = Ring5Band.of(layout(SEED + 1));
        boolean differs = false;
        for (int x = -8850; x <= 8850; x += 97) {
            int z = (int) Math.round(Math.sqrt(Math.max(0, 8850.0 * 8850.0 - (double) x * x)));
            assertEquals(BAND.value(x, z), again.value(x, z));
            differs |= BAND.value(x, z) != other.value(x, z);
        }
        assertTrue(differs);
    }

    /**
     * Kept per layout; a client builds its layout from the synced settings and noise seed and computes the same band
     * (no payload needed).
     */
    @Test
    void eachLayoutKeepsItsOwnBandAndAClientLayoutComputesTheSame() {
        RingLayout client = new RingLayout(LAYOUT.settings(), LAYOUT.noiseSeed());
        Ring5Band clientBand = Ring5Band.of(client);
        assertNotSame(BAND, clientBand);
        for (int i = 0; i < 4; i++) {
            assertSame(BAND, Ring5Band.of(LAYOUT));
            assertSame(clientBand, Ring5Band.of(client));
        }
        for (int x = 8000; x <= 9700; x += 7) {
            for (int z = -900; z <= 900; z += 150) {
                assertEquals(BAND.value(x, z), clientBand.value(x, z));
                assertEquals(BAND.outer(x, z), clientBand.outer(x, z));
            }
        }
    }

    @Test
    void outerIsTheMiddleOfTheTransitionAndInnerItsOpposite() {
        for (int x = 8000; x <= 9700; x += 3) {
            for (int z = -400; z <= 400; z += 50) {
                assertEquals(BAND.value(x, z) >= 0.0, BAND.outer(x, z), x + ", " + z);
                assertEquals(!BAND.outer(x, z), BAND.inner(x, z));
            }
        }
    }
}
