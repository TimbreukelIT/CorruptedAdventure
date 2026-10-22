package nl.timtoys.corruptedadventure.ring;

import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ring of each column for the offline scans (voidscan {@code --rings}). The boundary noise moves the zone 4→5 by up to
 * 96 blocks, so the zone's inner edge is not a circle: a column just inside the noisy edge is ring 4 (its own sea and
 * rules), not zone. The scan must tell them apart, so it asks this layout.
 * <p>
 * The export runs only when {@code CA_RING_AREAS} (an area file), {@code CA_RING_OUT} (the output) and
 * {@code CA_WORLD_SEED} are set; it is skipped otherwise:
 * <pre>
 * CA_RING_AREAS=tools/test/areas/ring5.txt CA_RING_OUT=build/test/rings-ring5.txt CA_WORLD_SEED=20260930 \
 *   ./gradlew cleanTest test --tests '*RingColumnsExportTest*'
 * </pre>
 */
class RingColumnsExportTest {
    /** The defaults of the run config (common config): the layout of the scanned worlds. */
    private static final RingSettings SETTINGS = new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES);
    /** A column of ring 4 in the noisy margin of the zone 4→5 on the ring 5 test world (seed 20260930, INT1 water). */
    private static final int RING_4_MARGIN_X = 7777;
    private static final int RING_4_MARGIN_Z = -18;

    @Test
    void marginColumnIsRingFourAndNotZone() {
        RingLayout layout = new RingLayout(SETTINGS, RingLayout.noiseSeed(20260930L));
        RingPosition margin = layout.sampleOverworld(RING_4_MARGIN_X, RING_4_MARGIN_Z);
        assertFalse(margin.inTransition(), "the margin column lies outside the zone 4→5");
        assertTrue(margin.ring() == Ring.RING_4, "the margin column is ring 4");
    }

    @Test
    void zoneColumnsAreTransitionOfFourAndFive() {
        RingLayout layout = new RingLayout(SETTINGS, RingLayout.noiseSeed(20260930L));
        boolean found = false;
        for (int x = 7800; x <= 8200 && !found; x++) {
            RingPosition pos = layout.sampleOverworld(x, 0);
            found = pos.inTransition() && pos.inner() == Ring.RING_4 && pos.outer() == Ring.RING_5;
        }
        assertTrue(found, "the zone 4→5 lies on the x axis between 7800 and 8200");
    }

    @Test
    void exportsEveryColumnOfTheAreas() throws Exception {
        String areas = System.getenv("CA_RING_AREAS");
        Assumptions.assumeTrue(areas != null && !areas.isBlank(), "CA_RING_AREAS not set: no export");
        String out = System.getenv("CA_RING_OUT");
        long seed = Long.parseLong(System.getenv("CA_WORLD_SEED"));
        RingLayout layout = new RingLayout(SETTINGS, RingLayout.noiseSeed(seed));
        try (BufferedWriter w = Files.newBufferedWriter(Path.of(out))) {
            for (String line : areaLines(Path.of(areas))) {
                String[] p = line.split("\\s+");
                if (p.length == 6 && !p[5].equals("ow") && !p[5].equals("overworld")) {
                    continue;
                }
                int cx0 = Integer.parseInt(p[1]), cz0 = Integer.parseInt(p[2]);
                int cx1 = Integer.parseInt(p[3]), cz1 = Integer.parseInt(p[4]);
                for (int x = Math.min(cx0, cx1) * 16; x < Math.max(cx0, cx1) * 16 + 16; x++) {
                    for (int z = Math.min(cz0, cz1) * 16; z < Math.max(cz0, cz1) * 16 + 16; z++) {
                        RingPosition pos = layout.sampleOverworld(x, z);
                        w.write(x + " " + z + " " + pos.ring() + " " + pos.inTransition() + "\n");
                    }
                }
            }
        }
    }

    /** The area lines of a file: no comments, no blank lines. */
    private static List<String> areaLines(Path file) throws Exception {
        return Files.readAllLines(file).stream()
                .map(line -> line.split("#", 2)[0].trim())
                .filter(line -> !line.isEmpty())
                .toList();
    }
}
