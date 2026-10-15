package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.level.levelgen.DensityFunction;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stone on the steep slopes of {@code verdant_falls} (decision 2026-10-15) on the test seed: the slope output
 * ({@link IslandField#slope}) does not depend on the direction, and the stone the surface rule puts with it
 * ({@code tools/worldgen/ring4_surface.py}: the exact walls, everything from {@link #ROCK} on, and from
 * {@link #PATCHY} on where {@link #PATCHES} is at least 0) lies in patches. Vanilla's {@code minecraft:steep}, which the
 * rule used before, left loose grey columns on the treads.
 */
class VerdantSlopeTest {
    private static final IslandField FIELD = IslandFieldTest.field();
    private static final RingLayout LAYOUT = IslandFieldTest.LAYOUT;
    /** The rule's thresholds (checked against the JSON below). */
    static final double ROCK = 2.0;
    static final double PATCHY = 1.5;
    static final String PATCHES = "ring_4_rock_patches";
    private static final DensityFunction.NoiseHolder PATCH_NOISE = IslandFieldTest.noise(PATCHES, IslandFieldTest.SEED);
    /** Columns around each sampled stretch of the descent. */
    private static final int HALF = 20;

    /**
     * The gradient is the slope of the least-squares plane through the 5 × 5 columns around, the same in every
     * direction; the slope the surface rule reads is the steepest gradient of the column and its eight neighbours.
     */
    @Test
    void theSlopeIsTheSteepestGradientAround() {
        for (int[] c : new int[][]{{5990, 0}, {5997, 3}, {0, 6010}, {-6020, 40}, {4243, 4243}, {4199, -4353}}) {
            double steepest = 0.0;
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    double sx = 0.0;
                    double sz = 0.0;
                    for (int j = -2; j <= 2; j++) {
                        for (int i = -2; i <= 2; i++) {
                            double h = FIELD.column(c[0] + dx + i, c[1] + dz + j).surface;
                            sx += i * h;
                            sz += j * h;
                        }
                    }
                    double gradient = Math.hypot(sx, sz) / 50.0;
                    assertEquals(gradient, FIELD.gradient(c[0] + dx, c[1] + dz), 1e-9);
                    steepest = Math.max(steepest, gradient);
                }
            }
            assertEquals(steepest, FIELD.slope(c[0], c[1]), 1e-9);
            assertEquals(steepest, new IslandOutput(FIELD, IslandOutput.Output.SLOPE)
                    .compute(new DensityFunction.SinglePointContext(c[0], 64, c[1])), 1e-9);
        }
        // A plane rising 1.5 per block towards any direction has a gradient of 1.5: the gradient has no direction.
        for (double angle = 0.0; angle < 2.0 * Math.PI; angle += 0.3) {
            double sx = 0.0;
            double sz = 0.0;
            for (int j = -2; j <= 2; j++) {
                for (int i = -2; i <= 2; i++) {
                    double h = 1.5 * (i * Math.cos(angle) + j * Math.sin(angle));
                    sx += i * h;
                    sz += j * h;
                }
            }
            assertEquals(1.5, Math.hypot(sx, sz) / 50.0, 1e-9);
        }
    }

    /** The surface rule of verdant_falls reads the slope with these thresholds and this noise, and no vanilla steep. */
    @Test
    void theRuleUsesTheseThresholds() {
        JsonObject branch = verdantFallsBranch();
        String text = branch.toString();
        assertTrue(!text.contains("\"minecraft:steep\""), "verdant_falls without vanilla's steep");
        List<Double> slopes = new ArrayList<>();
        collectSlopeThresholds(branch, slopes);
        assertEquals(List.of(ROCK, PATCHY), slopes);
        assertTrue(text.contains("\"noise\":\"corruptedadventure:" + PATCHES + "\",\"min_threshold\":0.0"), "rock patches on " + PATCHES);
    }

    /**
     * The stone on the descent (dry ground, no travertine rim) lies in patches: a column of stone on the treads always
     * has stone beside it. Counted as the rule decides, with vanilla's steep for comparison.
     */
    @Test
    void stoneOnTheDescentLiesInPatches() {
        int columns = 0;
        int stone = 0;
        int isolated = 0;
        int loose = 0;
        int wallsLoose = 0;
        int steepStone = 0;
        int steepLoose = 0;
        for (double angle = 0.1; angle < 2.0 * Math.PI; angle += 2.0 * Math.PI / 7.0) {
            double[] in = VerdantDescent.onRay(LAYOUT, angle, VerdantDescent.zoneStart(LAYOUT) + VerdantDescent.rimAt(LAYOUT));
            double[] out = VerdantDescent.onRay(LAYOUT, angle, VerdantDescent.zoneStart(LAYOUT) + VerdantDescent.width(LAYOUT));
            int x0 = (int) Math.floor(Math.min(in[0], out[0])) - HALF;
            int x1 = (int) Math.ceil(Math.max(in[0], out[0])) + HALF;
            int z0 = (int) Math.floor(Math.min(in[1], out[1])) - HALF;
            int z1 = (int) Math.ceil(Math.max(in[1], out[1])) + HALF;
            int w = x1 - x0 + 1;
            int d = z1 - z0 + 1;
            boolean[] dry = new boolean[w * d];
            boolean[] rock = new boolean[w * d];
            boolean[] walls = new boolean[w * d];
            boolean[] steep = new boolean[w * d];
            int[] top = new int[w * d];
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    top[(x - x0) + (z - z0) * w] = (int) Math.ceil(FIELD.column(x, z).surface);
                }
            }
            for (int z = z0 + 1; z < z1; z++) {
                for (int x = x0 + 1; x < x1; x++) {
                    int i = (x - x0) + (z - z0) * w;
                    IslandField.Column c = FIELD.column(x, z);
                    if (!c.verdant || c.verdantLakeTop > top[i] - 1 || c.rim >= 0.5) {
                        continue;
                    }
                    dry[i] = true;
                    walls[i] = c.cliff >= 0.5;
                    double slope = FIELD.slope(x, z);
                    rock[i] = walls[i] || slope >= ROCK || slope >= PATCHY && PATCH_NOISE.getValue(x, 0.0, z) >= 0.0;
                    // Vanilla's minecraft:steep (without its clamp at the chunk's edge, which only adds gaps).
                    steep[i] = walls[i] || top[i + w] >= top[i - w] + 4 || top[i - 1] >= top[i + 1] + 4;
                }
            }
            for (int z = z0 + 2; z < z1 - 1; z++) {
                for (int x = x0 + 2; x < x1 - 1; x++) {
                    int i = (x - x0) + (z - z0) * w;
                    if (!dry[i]) {
                        continue;
                    }
                    columns++;
                    boolean treads = amongTreads(dry, i, w);
                    if (rock[i]) {
                        stone++;
                        isolated += alone(rock, i, w) ? 1 : 0;
                        loose += alone(rock, i, w) && treads ? 1 : 0;
                        wallsLoose += alone(rock, i, w) && treads && walls[i] ? 1 : 0;
                    }
                    if (steep[i]) {
                        steepStone++;
                        steepLoose += alone(steep, i, w) && treads ? 1 : 0;
                    }
                }
            }
        }
        System.out.printf("descent columns %d: stone %d, without a stone neighbour %d, of which on the treads %d (%d exact"
                + " walls); vanilla steep: stone %d, loose on the treads %d%n", columns, stone, isolated, loose, wallsLoose,
                steepStone, steepLoose);
        assertTrue(columns > 100_000, "columns " + columns);
        // Stone on the steep risers, kinks and walls, not on most of the descent.
        assertTrue(stone > columns / 25 && stone < columns / 6, "stone " + stone + " of " + columns);
        // ≈ 0: the slope's stone always has a neighbour (the steepest gradient around); what is left are single columns
        // of the terrain's exact walls (ring_4/overworld/cliff) and pinches of a patch's outline.
        assertTrue(loose <= stone / 500, "loose " + loose + " of " + stone);
        assertTrue(isolated <= stone / 100, "isolated " + isolated + " of " + stone);
        assertTrue(loose < steepLoose, "loose " + loose + ", vanilla " + steepLoose);
    }

    /** All eight neighbours are dry ground of the descent (no water, no travertine rim). */
    private static boolean amongTreads(boolean[] dry, int i, int w) {
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (!dry[i + dx + dz * w]) {
                    return false;
                }
            }
        }
        return true;
    }

    /** No stone among the eight neighbours. */
    private static boolean alone(boolean[] rock, int i, int w) {
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dx != 0 || dz != 0) && rock[i + dx + dz * w]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static JsonObject verdantFallsBranch() {
        JsonObject[] found = {null};
        walk(WorldgenData.read("noise_settings/overworld.json").getAsJsonObject("surface_rule"), rule -> {
            JsonObject condition = rule.has("if_true") && rule.get("if_true").isJsonObject() ? rule.getAsJsonObject("if_true") : null;
            if (condition != null && condition.get("type").getAsString().equals("minecraft:biome")
                    && condition.getAsJsonArray("biome_is").size() == 1
                    && condition.getAsJsonArray("biome_is").get(0).getAsString().equals("corruptedadventure:verdant_falls")) {
                found[0] = rule.getAsJsonObject("then_run");
            }
        });
        assertTrue(found[0] != null, "verdant_falls branch");
        return found[0];
    }

    /** The {@code min} of every ring4_threshold on {@code ring_4/overworld/slope}, in order. */
    private static void collectSlopeThresholds(JsonObject branch, List<Double> out) {
        walk(branch, rule -> {
            if (rule.has("type") && rule.get("type").getAsString().equals("corruptedadventure:ring4_threshold")
                    && rule.get("density_function").getAsString().equals("corruptedadventure:ring_4/overworld/slope")) {
                out.add(rule.get("min").getAsDouble());
            }
        });
    }

    private static void walk(JsonElement element, java.util.function.Consumer<JsonObject> visit) {
        if (element.isJsonObject()) {
            visit.accept(element.getAsJsonObject());
            element.getAsJsonObject().entrySet().forEach(e -> walk(e.getValue(), visit));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(e -> walk(e, visit));
        }
    }
}
