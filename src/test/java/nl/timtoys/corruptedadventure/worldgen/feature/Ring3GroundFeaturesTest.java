package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** G2 ring 3: the subzones of {@link Ring3GroundFeatures} and the surface script read one noise with one threshold. */
class Ring3GroundFeaturesTest {
    private static final long SEED = 20260930L;

    /** About 20 % of the land in each patch kind, 40 % "other" (decision 2 = A), over many independent patches. */
    @Test
    void eachSubzoneCoversAboutAFifth() {
        NormalNoise noise = NormalNoise.create(new XoroshiroRandomSource(SEED).forkPositional()
                .fromHashOf(ResourceLocation.parse("corruptedadventure:ring_3_patches")), WorldgenData.noiseParameters("ring_3_patches"));
        int a = 0;
        int b = 0;
        int samples = 0;
        for (int i = 0; i < 200; i++) {
            for (int j = 0; j < 200; j++) {
                double value = noise.getValue(i * 397.0, 0.0, j * 397.0);
                a += value < -Ring3GroundFeatures.PATCH ? 1 : 0;
                b += value > Ring3GroundFeatures.PATCH ? 1 : 0;
                samples++;
            }
        }
        double shareA = a / (double) samples;
        double shareB = b / (double) samples;
        assertTrue(shareA > 0.15 && shareA < 0.25, "A patches " + shareA);
        assertTrue(shareB > 0.15 && shareB < 0.25, "B patches " + shareB);
    }

    /** The surface rules switch the subzones at ±PATCH (and draw the sinter's orange rim just inside +PATCH). */
    @Test
    void surfaceUsesTheFeaturesThreshold() throws IOException {
        String json = Files.readString(WorldgenData.root().resolve("noise_settings/overworld.json"));
        Matcher matcher = Pattern.compile("\"noise\": \"corruptedadventure:ring_3_patches\",\\s*\"min_threshold\": ([-0-9.eE+]+),\\s*"
                + "\"max_threshold\": ([-0-9.eE+]+)").matcher(json);
        Set<Double> thresholds = new TreeSet<>();
        while (matcher.find()) {
            for (int group = 1; group <= 2; group++) {
                double value = Double.parseDouble(matcher.group(group));
                if (Math.abs(value) < 1.0E300) {
                    thresholds.add(value);
                }
            }
        }
        double patch = Ring3GroundFeatures.PATCH;
        assertEquals(Set.of(-patch, patch, patch + 0.03), thresholds);
    }
}
