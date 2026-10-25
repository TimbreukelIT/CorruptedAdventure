package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomSupport;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * The ancient giant palm of {@code oasis_flats} ({@link Ring2FlatLandmarks.Kind#GIANT_PALM}): a palm with a 2 × 2
 * trunk, 20–28 high, leaning a little over its spring pool, with a flared foot and roots, and a big crown of drooping
 * fronds in the style of {@link PalmFoliagePlacer}. The site's anchor is the middle of the spring pool (a
 * {@code surface_pool} placed by {@link Ring2FlatLandmarkFeature} in the anchor's chunk); the trunk stands
 * {@link #TRUNK_DISTANCE} from it, a low rock of sandstone and canyon stone shelters the pool on the far side. The crown's
 * leaves are persistent (they reach farther than vanilla's leaf distance). Pure in the site and the pure terrain.
 */
final class GiantPalmShape {
    /** The highest first open block over the ground of the pool: the low oasis plain. */
    static final int MAX_GROUND = 72;
    static final int TRUNK_DISTANCE = 9;
    /** The pool's footprint (radius + rim) round the anchor, checked dry and level. */
    private static final int POOL_REACH = 6;
    /** The most the pool's ground may rise above its lowest column (one under the pool's {@code max_step} of 3). */
    private static final int POOL_SPREAD = 2;

    private GiantPalmShape() {
    }

    /** Whether the terrain suits a pool at (x, z) and a trunk in one of the eight directions round it. */
    static boolean suits(Ring2FlatLandmarks.Terrain terrain, int x, int z, int ground) {
        return poolFlat(terrain, x, z, ground) && direction(terrain, x, z, ground) >= 0;
    }

    /**
     * Whether the pool's footprint is dry and level enough for the {@code surface_pool}: the ground at the anchor and on
     * rings of 3 and {@link #POOL_REACH} spreads at most {@link #POOL_SPREAD} (the pool digs every column down to the
     * lowest one, at most its {@code max_step}).
     */
    static boolean poolFlat(Ring2FlatLandmarks.Terrain terrain, int x, int z, int ground) {
        int low = ground;
        int high = ground;
        for (int r : new int[] {3, POOL_REACH}) {
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4.0;
                int px = x + (int) Math.round(Math.cos(angle) * r);
                int pz = z + (int) Math.round(Math.sin(angle) * r);
                int g = terrain.surface(px, pz);
                if (terrain.floor(px, pz) != g) {
                    return false;
                }
                low = Math.min(low, g);
                high = Math.max(high, g);
            }
        }
        return high - low <= POOL_SPREAD;
    }

    /**
     * The direction (0–7, from a start drawn from the anchor) of the first trunk spot that stands on dry, level ground,
     * or −1. Pure in the anchor and the terrain, so the shape finds the same one.
     */
    static int direction(Ring2FlatLandmarks.Terrain terrain, int x, int z, int ground) {
        int start = (int) Math.floorMod(RandomSupport.mixStafford13(BlockPos.asLong(x, 0, z)), 8L);
        for (int k = 0; k < 8; k++) {
            int d = (start + k) % 8;
            int tx = x + offset(d, true);
            int tz = z + offset(d, false);
            int g = terrain.surface(tx, tz);
            if (Math.abs(g - ground) <= 3 && terrain.floor(tx, tz) == g
                    && Ring2FlatLandmarks.ringLevel(terrain, tx, tz, new int[] {2, 4}, g, 2, true)) {
                return d;
            }
        }
        return -1;
    }

    /** The trunk's offset from the anchor towards direction {@code d}. */
    static int offset(int d, boolean x) {
        double angle = d * Math.PI / 4.0;
        return (int) Math.round((x ? Math.cos(angle) : Math.sin(angle)) * TRUNK_DISTANCE);
    }

    static LandmarkPlan build(Ring2FlatLandmarks.Site site, Ring2FlatLandmarks.Terrain terrain) {
        LandmarkPlan plan = new LandmarkPlan();
        int d = direction(terrain, site.x(), site.z(), site.ground());
        if (d < 0) {
            return plan;
        }
        RandomSource random = Ring2FlatLandmarks.random(site, 1);
        int reach = site.kind().reach();
        int tx = site.x() + offset(d, true);
        int tz = site.z() + offset(d, false);
        int base = terrain.surface(tx, tz);
        int height = 20 + random.nextInt(9);
        // Leaning over the pool: towards the anchor, up to 2–3 blocks at the top, from a third of the height on.
        double toward = Math.atan2(site.z() - tz, site.x() - tx);
        double lean = 2.0 + random.nextDouble();
        BlockState log = ModBlocks.PALM_LOG.get().defaultBlockState();
        BlockState wood = ModBlocks.PALM_WOOD.get().defaultBlockState();
        BlockState leaves = ModBlocks.PALM_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true)
                .setValue(LeavesBlock.DISTANCE, 1);
        double topX = 0.0;
        double topZ = 0.0;
        for (int y = 0; y < height; y++) {
            double t = y / (double) height;
            double s = t < 0.33 ? 0.0 : Mth.square((t - 0.33) / 0.67) * lean;
            int ox = (int) Math.round(Math.cos(toward) * s);
            int oz = (int) Math.round(Math.sin(toward) * s);
            for (int i = 0; i < 4; i++) {
                put(plan, site, reach, new BlockPos(tx + ox + (i & 1), base + y, tz + oz + (i >> 1)), log);
            }
            topX = ox;
            topZ = oz;
        }
        for (int i = 0; i < 4; i++) {
            plan.feet.add(new LandmarkPlan.Foot(tx + (i & 1), tz + (i >> 1), base, 1, log));
        }
        // The flared foot: wood round the trunk on the first two layers, and roots running out over the ground.
        for (int dz = -1; dz <= 2; dz++) {
            for (int dx = -1; dx <= 2; dx++) {
                boolean ring = dx == -1 || dx == 2 || dz == -1 || dz == 2;
                boolean corner = (dx == -1 || dx == 2) && (dz == -1 || dz == 2);
                if (!ring || corner) {
                    continue;
                }
                float roll = random.nextFloat();
                BlockPos foot = new BlockPos(tx + dx, base, tz + dz);
                if (roll < 0.75F) {
                    put(plan, site, reach, foot, wood);
                    plan.feet.add(new LandmarkPlan.Foot(foot.getX(), foot.getZ(), base, 1, wood));
                    if (roll < 0.3F) {
                        put(plan, site, reach, foot.above(), wood);
                    }
                }
            }
        }
        int roots = 3 + random.nextInt(3);
        for (int i = 0; i < roots; i++) {
            Direction side = Direction.from2DDataValue(random.nextInt(4));
            int along = random.nextInt(2);
            int length = 2 + random.nextInt(2);
            for (int j = 1; j <= length; j++) {
                // Out from the trunk's face (the trunk covers tx..tx+1, tz..tz+1), the last block sunk into the ground.
                int out = side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 + j : -j;
                int rx = tx + (side.getAxis() == Direction.Axis.X ? out : along);
                int rz = tz + (side.getAxis() == Direction.Axis.Z ? out : along);
                int y = terrain.surface(rx, rz) - (j == length ? 1 : 0);
                put(plan, site, reach, new BlockPos(rx, y, rz), wood.setValue(RotatedPillarBlock.AXIS, side.getAxis()));
                plan.feet.add(new LandmarkPlan.Foot(rx, rz, y, 1, wood));
            }
        }
        crown(plan, site, reach, random, leaves, wood, tx + 1.0 + topX, base + height, tz + 1.0 + topZ, (int) topX, (int) topZ, tx, tz);
        rock(plan, site, reach, random, terrain, d);
        // Ferns and grass round the foot.
        for (int i = 0; i < 14; i++) {
            int gx = tx + random.nextInt(9) - 4;
            int gz = tz + random.nextInt(9) - 4;
            float roll = random.nextFloat();
            plan.groundDecor.put(new BlockPos(gx, 0, gz), roll < 0.4F ? Blocks.FERN.defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState());
        }
        return plan;
    }

    /**
     * The crown on top of the trunk (top centre (cx, top, cz) between the four top logs): a knob of leaves over the
     * trunk and 12 fronds of 6–8 leaves that bend down towards their tips, the tips hanging.
     */
    private static void crown(LandmarkPlan plan, Ring2FlatLandmarks.Site site, int reach, RandomSource random, BlockState leaves,
                              BlockState wood, double cx, int top, double cz, int ox, int oz, int tx, int tz) {
        for (int i = 0; i < 4; i++) {
            put(plan, site, reach, new BlockPos(tx + ox + (i & 1), top, tz + oz + (i >> 1)), wood);
        }
        for (int dz = -1; dz <= 2; dz++) {
            for (int dx = -1; dx <= 2; dx++) {
                boolean corner = (dx == -1 || dx == 2) && (dz == -1 || dz == 2);
                if (!corner) {
                    put(plan, site, reach, new BlockPos(tx + ox + dx, top + 1, tz + oz + dz), leaves);
                }
                if (dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1) {
                    put(plan, site, reach, new BlockPos(tx + ox + dx, top + 2, tz + oz + dz), leaves);
                }
            }
        }
        double start = random.nextDouble() * Math.PI * 2.0;
        for (int f = 0; f < 12; f++) {
            double angle = start + f * Math.PI / 6.0 + (random.nextDouble() - 0.5) * 0.25;
            int length = 6 + random.nextInt(3);
            double rise = random.nextDouble() < 0.5 ? 1.0 : 0.0;
            BlockPos last = null;
            for (double s = 1.5; s <= length + 1.5; s += 0.5) {
                double u = (s - 1.5) / length;
                int y = top + (int) Math.floor(rise + 0.6 - 3.2 * u * u);
                BlockPos pos = new BlockPos(Mth.floor(cx + Math.cos(angle) * s), y, Mth.floor(cz + Math.sin(angle) * s));
                if (pos.equals(last)) {
                    continue;
                }
                // Keep the frond connected: a step down of more than one gets a leaf in between.
                if (last != null && last.getY() - pos.getY() >= 1 && (last.getX() != pos.getX() || last.getZ() != pos.getZ())) {
                    put(plan, site, reach, new BlockPos(pos.getX(), last.getY(), pos.getZ()), leaves);
                }
                put(plan, site, reach, pos, leaves);
                last = pos;
            }
            if (last != null) {
                put(plan, site, reach, last.below(), leaves);
                if (random.nextBoolean()) {
                    put(plan, site, reach, last.below(2), leaves);
                }
            }
        }
    }

    /** A low rock of 2–4 lumps on the far side of the pool (away from the trunk), its foot in the ground. */
    private static void rock(LandmarkPlan plan, Ring2FlatLandmarks.Site site, int reach, RandomSource random,
                             Ring2FlatLandmarks.Terrain terrain, int d) {
        double away = d * Math.PI / 4.0 + Math.PI + (random.nextDouble() - 0.5) * 0.6;
        double rx = site.x() + 0.5 + Math.cos(away) * 8.0;
        double rz = site.z() + 0.5 + Math.sin(away) * 8.0;
        BlockState[] stones = {Blocks.SANDSTONE.defaultBlockState(), Blocks.SMOOTH_SANDSTONE.defaultBlockState(),
                ModBlocks.CREAM_CANYON_STONE.get().defaultBlockState(), ModBlocks.ORANGE_CANYON_STONE.get().defaultBlockState()};
        int lumps = 2 + random.nextInt(3);
        for (int i = 0; i < lumps; i++) {
            double lx = rx + (random.nextDouble() - 0.5) * 3.0;
            double lz = rz + (random.nextDouble() - 0.5) * 3.0;
            double sx = 1.8 + random.nextDouble() * 1.2;
            double sz = 1.8 + random.nextDouble() * 1.2;
            double sy = 2.0 + random.nextDouble() * 2.0;
            BlockState stone = stones[random.nextInt(stones.length)];
            int g = terrain.surface(Mth.floor(lx), Mth.floor(lz)) - 1;
            for (int dz = -3; dz <= 3; dz++) {
                for (int dx = -3; dx <= 3; dx++) {
                    int x = Mth.floor(lx) + dx;
                    int z = Mth.floor(lz) + dz;
                    double ex = (x + 0.5 - lx) / sx;
                    double ez = (z + 0.5 - lz) / sz;
                    double flat = ex * ex + ez * ez;
                    if (flat > 1.0) {
                        continue;
                    }
                    int h = (int) Math.round(sy * Math.sqrt(1.0 - flat));
                    for (int y = 0; y <= h; y++) {
                        put(plan, site, reach, new BlockPos(x, g + y, z), stone);
                    }
                    plan.feet.add(new LandmarkPlan.Foot(x, z, g, 1, stone));
                }
            }
        }
    }

    private static void put(LandmarkPlan plan, Ring2FlatLandmarks.Site site, int reach, BlockPos pos, BlockState state) {
        if (Math.abs(pos.getX() - site.x()) <= reach && Math.abs(pos.getZ() - site.z()) <= reach) {
            plan.objects.putIfAbsent(pos, state);
        }
    }
}
