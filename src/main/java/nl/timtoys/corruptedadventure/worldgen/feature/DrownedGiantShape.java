package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

import java.util.List;
import java.util.Map;

/**
 * The drowned giant tree of {@code drowned_forest} ({@link BlightedLandmarks.Kind#DROWNED_GIANT}): a dead
 * {@code withered_moonpetal} giant, three blocks thick and 26–34 high, that leans as it sinks into the dark water, a
 * third of it under the surface. Roots spread over the floor, a broken top, five to eight bare branches with twigs in
 * the upper half; above the water moss on the wood, bracket fungi (brown mushroom blocks) on the trunk, blighted
 * glowcaps on the branches and vines hanging from them. Pure in the site.
 */
final class DrownedGiantShape {
    private static final int REACH = BlightedLandmarks.Kind.DROWNED_GIANT.radius();

    private DrownedGiantShape() {
    }

    static LandmarkPlan build(BlightedLandmarks.Site site) {
        RandomSource random = new XoroshiroRandomSource(site.seed());
        BlockState log = ModBlocks.WITHERED_MOONPETAL_LOG.get().defaultBlockState();
        BlockState wood = ModBlocks.WITHERED_MOONPETAL_WOOD.get().defaultBlockState();
        LandmarkPlan plan = new LandmarkPlan();
        int base = site.ground() - 1;
        int height = 26 + random.nextInt(9);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double lean = 3.0 + random.nextInt(3);
        double lx = Math.cos(angle);
        double lz = Math.sin(angle);
        int surface = BlightedLandmarks.WATER_TOP + 1;

        // The trunk: a disc per level, wider at the foot, leaning more as it rises; the top two levels broken.
        for (int dy = 0; dy <= height; dy++) {
            double f = dy / (double) height;
            double off = lean * Math.pow(f, 1.6);
            double cx = site.x() + 0.5 + lx * off;
            double cz = site.z() + 0.5 + lz * off;
            double radius = dy < 3 ? 3.4 - 0.35 * dy : 2.4 - 1.1 * f;
            int r = Mth.ceil(radius);
            for (int x = Mth.floor(cx) - r; x <= Mth.floor(cx) + r; x++) {
                for (int z = Mth.floor(cz) - r; z <= Mth.floor(cz) + r; z++) {
                    float jag = random.nextFloat();
                    double d = Mth.square(x + 0.5 - cx) + Mth.square(z + 0.5 - cz);
                    boolean broken = dy >= height - 2 && jag < 0.45F;
                    if (d <= radius * radius && !broken) {
                        put(plan, site, new BlockPos(x, base + dy, z), dy < 3 ? wood : log);
                    }
                }
            }
        }

        // Roots over the floor: out from the foot, rising a little, the tips back into the ground.
        int roots = 5 + random.nextInt(3);
        for (int k = 0; k < roots; k++) {
            double a = angle + k * Math.PI * 2.0 / roots + (random.nextDouble() - 0.5) * 0.5;
            int length = 4 + random.nextInt(5);
            for (double s = 1.5; s <= 2.0 + length; s += 0.5) {
                double rise = 2.0 * Math.sin(Math.PI * (s - 1.5) / (length + 0.5)) - (s > length ? 2.0 : 0.0);
                put(plan, site, new BlockPos(Mth.floor(site.x() + 0.5 + Math.cos(a) * s), base + Mth.floor(rise),
                        Mth.floor(site.z() + 0.5 + Math.sin(a) * s)), wood);
            }
        }

        // Bare branches in the upper half, each with a fork of two twigs.
        int branches = 5 + random.nextInt(4);
        for (int k = 0; k < branches; k++) {
            int from = Mth.floor(height * (0.5 + 0.45 * random.nextDouble()));
            double f = from / (double) height;
            double off = lean * Math.pow(f, 1.6);
            double a = angle + k * Math.PI * 2.0 / branches + (random.nextDouble() - 0.5) * 0.8;
            double rise = 0.35 + random.nextDouble() * 0.6;
            int length = 6 + random.nextInt(5);
            double sx = site.x() + 0.5 + lx * off;
            double sz = site.z() + 0.5 + lz * off;
            double[] end = limb(plan, site, wood, sx, base + from + 0.5, sz, a, rise, length);
            for (int side = -1; side <= 1; side += 2) {
                limb(plan, site, wood, end[0], end[1], end[2], a + side * (0.5 + random.nextDouble() * 0.4), rise + 0.3, 2 + random.nextInt(3));
            }
        }

        dress(plan, random, surface, log, wood);
        return plan;
    }

    /** A limb from (x, y, z) along {@code angle}, rising {@code rise} per block; returns its end. */
    private static double[] limb(LandmarkPlan plan, BlightedLandmarks.Site site, BlockState wood, double x, double y, double z, double angle,
                                 double rise, int length) {
        double ex = x;
        double ey = y;
        double ez = z;
        for (double s = 0.0; s <= length; s += 0.34) {
            ex = x + Math.cos(angle) * s;
            ey = y + rise * s;
            ez = z + Math.sin(angle) * s;
            put(plan, site, BlockPos.containing(ex, ey, ez), wood);
        }
        return new double[]{ex, ey, ez};
    }

    /** Wood at {@code pos} unless it lies beyond the landmark's reach (it then stays out). */
    private static void put(LandmarkPlan plan, BlightedLandmarks.Site site, BlockPos pos, BlockState state) {
        if (Math.abs(pos.getX() - site.x()) <= REACH && Math.abs(pos.getZ() - site.z()) <= REACH) {
            plan.wood.putIfAbsent(pos, state);
        }
    }

    /** Above the water: moss, stripped (rotten) patches, bracket fungi, glowcaps and vines. */
    private static void dress(LandmarkPlan plan, RandomSource random, int surface, BlockState log, BlockState wood) {
        BlockState strippedWood = ModBlocks.STRIPPED_WITHERED_MOONPETAL_WOOD.get().defaultBlockState();
        BlockState glowcap = ModBlocks.BLIGHTED_GLOWCAP.get().defaultBlockState();
        for (BlockPos pos : List.copyOf(plan.wood.keySet())) {
            float moss = random.nextFloat();
            float growth = random.nextFloat();
            float vine = random.nextFloat();
            int side = random.nextInt(4);
            int length = 2 + random.nextInt(6);
            if (pos.getY() < surface) {
                continue;
            }
            BlockState state = plan.wood.get(pos);
            if (moss < 0.06F && state.is(log.getBlock())) {
                plan.wood.put(pos, strippedWood);
            }
            BlockPos above = pos.above();
            if (!plan.solid(above)) {
                if (growth < 0.35F) {
                    plan.decor.add(new LandmarkPlan.Decor(above, Blocks.MOSS_CARPET.defaultBlockState(), pos));
                } else if (growth < 0.43F) {
                    plan.decor.add(new LandmarkPlan.Decor(above, glowcap, pos));
                } else if (growth < 0.48F && state.is(wood.getBlock())) {
                    plan.objects.put(above, Blocks.MOSS_BLOCK.defaultBlockState());
                }
            }
            Direction direction = Direction.from2DDataValue(side);
            if (vine < 0.1F && !state.is(log.getBlock()) && !plan.solid(pos.relative(direction))) {
                plan.vine(pos, direction, length);
            }
        }
        // Bracket fungi: a few shelves of two or three brown mushroom blocks on the trunk's sides.
        List<BlockPos> logs = plan.wood.entrySet().stream().filter(entry -> entry.getValue().is(log.getBlock())
                && entry.getKey().getY() >= surface + 2).map(Map.Entry::getKey).toList();
        int shelves = logs.isEmpty() ? 0 : 3 + random.nextInt(3);
        for (int k = 0; k < shelves; k++) {
            BlockPos trunk = logs.get(random.nextInt(logs.size()));
            Direction out = Direction.from2DDataValue(random.nextInt(4));
            BlockPos at = trunk.relative(out);
            if (plan.solid(at)) {
                continue;
            }
            Direction along = out.getClockWise();
            int extra = random.nextInt(2);
            for (int i = -1; i <= extra; i++) {
                BlockPos shelf = at.relative(along, i);
                if (!plan.solid(shelf)) {
                    plan.objects.put(shelf, Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState());
                }
            }
        }
    }
}
