package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import javax.annotation.Nullable;
import java.util.function.IntBinaryOperator;

/**
 * The sinter terraces of {@code sulfur_fields} ({@link VolcanicLandmarks.Kind#SINTER_TERRACES}): a Pamukkale-like
 * flight of 4–6 terraces down a slope, 20–40 wide and fanning out downhill, each a level pool of sulfur mud (one colour
 * per terrace, boiling mud at the spring on top) behind a scalloped rim of calcite, the rims' faces banded with yellow and
 * orange terracotta and calcite down to the next terrace. The terraces are built out over the slope (filled down to the
 * ground) and cut a little into it (cleared up to three blocks over a pool). Pure in the site and the pure ground heights;
 * null where the slope is too flat, too steep or uneven across.
 */
final class SinterTerraceShape {
    /** How high a terrace may stand over the ground below it, and the ground over a terrace's level. */
    private static final int MAX_FILL = 6;
    private static final int MAX_CUT = 3;

    private SinterTerraceShape() {
    }

    @Nullable
    static VolcanicPlan build(VolcanicLandmarks.Site site, IntBinaryOperator ground) {
        double cx = site.x() + 0.5;
        double cz = site.z() + 0.5;
        int bx = site.x();
        int bz = site.z();
        double gx = ground.applyAsInt(bx + 6, bz) - ground.applyAsInt(bx - 6, bz);
        double gz = ground.applyAsInt(bx, bz + 6) - ground.applyAsInt(bx, bz - 6);
        double drop = Math.sqrt(gx * gx + gz * gz);
        // A slope of 1 in 6 to 1 in 1.5 over the 12 blocks round the anchor.
        if (drop < 2.0 || drop > 8.0) {
            return null;
        }
        double ux = -gx / drop;
        double uz = -gz / drop;
        RandomSource random = new XoroshiroRandomSource(site.seed());
        int count = 4 + random.nextInt(3);
        double halfWidth = (20 + random.nextInt(21)) / 2.0;
        double[] edges = new double[count + 1];
        double depth = 0.0;
        for (int k = 0; k < count; k++) {
            depth += 3.0 + random.nextInt(3);
            edges[k + 1] = depth;
        }
        // The flight runs from s = -depth / 2 (top) to depth / 2 (bottom) along the way down.
        for (int k = 0; k <= count; k++) {
            edges[k] -= depth / 2.0;
        }
        double[] lobe = new double[count];
        double[] phase = new double[count];
        double[] scallop = new double[count];
        BlockState[] mud = new BlockState[count];
        BlockState[] muds = {ModBlocks.YELLOW_SULFUR_MUD.get().defaultBlockState(), ModBlocks.ORANGE_SULFUR_MUD.get().defaultBlockState(),
                ModBlocks.GREEN_SULFUR_MUD.get().defaultBlockState()};
        int colour = random.nextInt(muds.length);
        for (int k = 0; k < count; k++) {
            lobe[k] = 4.0 + random.nextDouble() * 3.0;
            phase[k] = random.nextDouble() * Math.PI;
            scallop[k] = 0.8 + random.nextDouble() * 1.2;
            if (random.nextFloat() < 0.6F) {
                colour = (colour + 1) % muds.length;
            }
            mud[k] = muds[colour];
        }
        // Each terrace's level: the ground along its top edge, at least one and at most three below the one above.
        int[] levels = new int[count];
        for (int k = 0; k < count; k++) {
            int sum = 0;
            int samples = 0;
            for (double l = -halfWidth * 0.6; l <= halfWidth * 0.6; l += halfWidth * 0.3) {
                sum += ground.applyAsInt(Mth.floor(cx + ux * (edges[k] + 1.0) - uz * l), Mth.floor(cz + uz * (edges[k] + 1.0) + ux * l));
                samples++;
            }
            int level = Math.round(sum / (float) samples);
            levels[k] = k == 0 ? level : Mth.clamp(level, levels[k - 1] - 3, levels[k - 1] - 1);
        }

        VolcanicPlan plan = new VolcanicPlan(site.x(), site.z(), site.kind().reach());
        BlockState calcite = Blocks.CALCITE.defaultBlockState();
        BlockState yellow = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
        BlockState orange = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState boiling = ModBlocks.BOILING_MUD.get().defaultBlockState();
        int reach = site.kind().reach();
        for (int dz = -reach; dz <= reach; dz++) {
            for (int dx = -reach; dx <= reach; dx++) {
                int x = bx + dx;
                int z = bz + dz;
                double px = x + 0.5 - cx;
                double pz = z + 0.5 - cz;
                double s = px * ux + pz * uz;
                double l = -px * uz + pz * ux;
                // The outline: a fan, narrower at the spring, rounded at the corners.
                double f = (s - edges[0]) / depth;
                double width = halfWidth * (0.6 + 0.4 * Mth.clamp(f, 0.0, 1.0));
                if (f < -0.02 || Mth.square(l / width) + Mth.square(s / (depth / 2.0 + 1.5)) > 1.25
                        || Math.abs(l) > width) {
                    continue;
                }
                // Which terrace: its scalloped downhill rim bounds it.
                int k = 0;
                while (k < count - 1 && s >= rim(edges, lobe, phase, scallop, k, l)) {
                    k++;
                }
                double rimAt = rim(edges, lobe, phase, scallop, k, l);
                if (s > rimAt + 0.5 && k == count - 1) {
                    continue;
                }
                boolean rimBlock = s >= rimAt - 1.3 || Math.abs(l) >= width - 1.3;
                int level = levels[k];
                int g = ground.applyAsInt(x, z);
                if (level - g > MAX_FILL || g - level > MAX_CUT + 1) {
                    return null;
                }
                // The body down to the ground: calcite banded with the terracottas.
                for (int y = Math.min(g, level) - 1; y < level; y++) {
                    plan.put(new BlockPos(x, y, z), band(y, calcite, yellow, orange));
                }
                plan.foot(x, z, Math.min(g, level) - 1, 1, calcite);
                if (rimBlock) {
                    plan.put(new BlockPos(x, level, z), calcite);
                    clear(plan, air, x, z, level + 1, g);
                } else {
                    boolean spring = k == 0 && Math.abs(l) < 1.5 && s < edges[0] + 2.5;
                    plan.set(new BlockPos(x, level - 1, z), spring ? boiling : mud[k]);
                    clear(plan, air, x, z, level, g);
                }
            }
        }
        return plan;
    }

    /** Where terrace {@code k}'s rim lies across the flight: its downhill edge with a scallop of lobes. */
    private static double rim(double[] edges, double[] lobe, double[] phase, double[] scallop, int k, double l) {
        return edges[k + 1] - scallop[k] * Math.abs(Math.sin(Math.PI * l / lobe[k] + phase[k]));
    }

    /** Calcite with thin bands of yellow and orange terracotta, by height. */
    private static BlockState band(int y, BlockState calcite, BlockState yellow, BlockState orange) {
        return switch (Math.floorMod(y, 5)) {
            case 1 -> yellow;
            case 3 -> orange;
            default -> calcite;
        };
    }

    /** Air from {@code from} up to the ground's top and a block over it (at most {@link #MAX_CUT} + 1). */
    private static void clear(VolcanicPlan plan, BlockState air, int x, int z, int from, int ground) {
        for (int y = from; y <= Math.min(ground, from + MAX_CUT); y++) {
            plan.put(new BlockPos(x, y, z), air);
        }
    }
}
