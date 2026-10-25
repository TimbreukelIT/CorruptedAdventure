package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomSupport;
import nl.timtoys.corruptedadventure.registry.ModBlocks;

/**
 * The salt dome of {@code salt_flats} ({@link Ring2FlatLandmarks.Kind#SALT_DOME}): a hollow dome of salt blocks on the
 * bare crust, 20–30 wide and 8–15 high, slightly oval and lumpy, its shell two blocks thick. Polished salt in patches
 * and crust on the upward faces outside, a few cracks (narrow slits through the shell), one or two arched ways in, salt
 * crystals (light 3) on the inside of the shell and here and there on the floor. Pure in the site: the salt flats are
 * flat, so the dome stands on the site's ground.
 */
final class SaltDomeShape {
    /** The first open block over the bare crust (crust at Y 65). */
    static final int CRUST_TOP = 66;
    static final double MAX_RADIUS = 15.4;
    private static final double SHELL = 2.2;
    private static final float WALL_CRYSTAL = 0.22F;
    private static final float FLOOR_CRYSTAL = 0.05F;

    private SaltDomeShape() {
    }

    /** The dome's form: centre, radii, rotation, lumps, entrances and cracks. */
    record Form(double cx, double cz, double radius, double height, double stretch, double angle, double[] lumps,
                double[] entrances, double[][] cracks) {
        /** The horizontal radius of the outer surface towards {@code theta} (lumpy), at most {@link #MAX_RADIUS}. */
        double radiusAt(double theta) {
            double r = radius * (1.0 + lumps[0] * Math.sin(3.0 * theta + lumps[1]) + lumps[2] * Math.sin(5.0 * theta + lumps[3]));
            return Math.min(r, MAX_RADIUS);
        }

        /** The dome's own polar angle of (dx, dz) from the centre, and its distance in units of the oval. */
        double[] polar(double dx, double dz) {
            double u = dx * Math.cos(angle) + dz * Math.sin(angle);
            double v = (-dx * Math.sin(angle) + dz * Math.cos(angle)) / stretch;
            return new double[] {Math.atan2(v, u), Math.sqrt(u * u + v * v)};
        }

        /** Whether the block (dx, y, dz) (y above the ground) lies inside the outer surface, shrunk by {@code inset}. */
        boolean inside(double dx, int y, double dz, double inset) {
            double[] p = polar(dx, dz);
            double r = radiusAt(p[0]) - inset;
            double h = height - inset;
            if (r <= 0.0 || h <= 0.0) {
                return false;
            }
            double yy = y + 0.5;
            return Mth.square(p[1] / r) + Mth.square(yy / h) <= 1.0;
        }

        boolean shell(double dx, int y, double dz) {
            return y >= 0 && inside(dx, y, dz, 0.0) && !inside(dx, y, dz, SHELL);
        }

        /** Whether the shell is open here: an entrance (3 wide, an arch 4 high) or a crack. */
        boolean open(double dx, int y, double dz) {
            double[] p = polar(dx, dz);
            double r = radiusAt(p[0]);
            for (double entrance : entrances) {
                double lateral = Math.abs(Mth.wrapDegrees(Math.toDegrees(p[0] - entrance))) * Math.PI / 180.0 * r;
                if (lateral < 1.6 && y + 0.5 < 4.0 - 0.6 * lateral * lateral) {
                    return true;
                }
            }
            for (double[] crack : cracks) {
                double lateral = Math.abs(Mth.wrapDegrees(Math.toDegrees(p[0] - crack[0]))) * Math.PI / 180.0 * r;
                if (lateral < 0.5 && y >= crack[1] && y <= crack[2]) {
                    return true;
                }
            }
            return false;
        }
    }

    static Form form(Ring2FlatLandmarks.Site site) {
        RandomSource random = Ring2FlatLandmarks.random(site, 1);
        double radius = 10.0 + random.nextInt(5) + random.nextDouble();
        double height = Math.min(radius + 1.0, 8.0 + random.nextInt(8));
        double stretch = 0.8 + random.nextDouble() * 0.2;
        double angle = random.nextDouble() * Math.PI;
        double[] lumps = {0.04 + random.nextDouble() * 0.05, random.nextDouble() * Math.PI * 2.0,
                0.02 + random.nextDouble() * 0.04, random.nextDouble() * Math.PI * 2.0};
        double first = random.nextDouble() * Math.PI * 2.0;
        double[] entrances = random.nextFloat() < 0.5 ? new double[] {first}
                : new double[] {first, first + Math.PI * (0.6 + random.nextDouble() * 0.8)};
        double[][] cracks = new double[3 + random.nextInt(3)][];
        for (int i = 0; i < cracks.length; i++) {
            int from = 1 + random.nextInt(Math.max(1, (int) height - 3));
            cracks[i] = new double[] {random.nextDouble() * Math.PI * 2.0, from, from + 2 + random.nextInt(4)};
        }
        return new Form(site.x() + 0.5, site.z() + 0.5, radius, height, stretch, angle, lumps, entrances, cracks);
    }

    static LandmarkPlan build(Ring2FlatLandmarks.Site site) {
        Form form = form(site);
        RandomSource random = Ring2FlatLandmarks.random(site, 2);
        LandmarkPlan plan = new LandmarkPlan();
        BlockState salt = ModBlocks.SALT_BLOCK.get().defaultBlockState();
        BlockState polished = ModBlocks.POLISHED_SALT.get().defaultBlockState();
        BlockState crust = ModBlocks.SALT_CRUST.get().defaultBlockState();
        BlockState crystal = ModBlocks.SALT_CRYSTAL.get().defaultBlockState();
        int base = site.ground();
        int reach = site.kind().reach();
        int top = (int) Math.ceil(form.height());
        long patches = random.nextLong();
        for (int dz = -reach; dz <= reach; dz++) {
            for (int dx = -reach; dx <= reach; dx++) {
                double px = site.x() + dx + 0.5 - form.cx();
                double pz = site.z() + dz + 0.5 - form.cz();
                for (int y = 0; y <= top; y++) {
                    if (!form.shell(px, y, pz) || form.open(px, y, pz)) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(site.x() + dx, base + y, site.z() + dz);
                    boolean outer = !form.inside(px, y + 1, pz, 0.0) || !form.inside(px + 1, y, pz, 0.0) || !form.inside(px - 1, y, pz, 0.0)
                            || !form.inside(px, y, pz + 1, 0.0) || !form.inside(px, y, pz - 1, 0.0);
                    BlockState state = salt;
                    if (outer && !form.inside(px, y + 1, pz, 0.0)) {
                        state = crust;
                    } else if (outer && patch(patches, pos)) {
                        state = polished;
                    }
                    plan.objects.put(pos, state);
                    if (y == 0) {
                        plan.feet.add(new LandmarkPlan.Foot(pos.getX(), pos.getZ(), pos.getY(), 1, salt));
                    }
                }
            }
        }
        // Crystals on the inner face of the shell (pointing away from it) and a few on the floor of the cave.
        for (int dz = -reach; dz <= reach; dz++) {
            for (int dx = -reach; dx <= reach; dx++) {
                double px = site.x() + dx + 0.5 - form.cx();
                double pz = site.z() + dz + 0.5 - form.cz();
                for (int y = 0; y <= top; y++) {
                    if (!form.inside(px, y, pz, SHELL)) {
                        continue;
                    }
                    float roll = random.nextFloat();
                    BlockPos pos = new BlockPos(site.x() + dx, base + y, site.z() + dz);
                    Direction face = null;
                    for (Direction direction : Direction.values()) {
                        if (plan.objects.containsKey(pos.relative(direction))) {
                            face = direction;
                            break;
                        }
                    }
                    if (face != null && roll < WALL_CRYSTAL) {
                        plan.decor.add(new LandmarkPlan.Decor(pos, crystal.setValue(AmethystClusterBlock.FACING, face.getOpposite()),
                                pos.relative(face)));
                    } else if (face == null && y == 0 && roll < FLOOR_CRYSTAL) {
                        plan.groundDecor.put(new BlockPos(pos.getX(), 0, pos.getZ()), crystal);
                    }
                }
            }
        }
        return plan;
    }

    /** Polished salt in patches of a few blocks on the outer face. */
    private static boolean patch(long seed, BlockPos pos) {
        long cell = BlockPos.asLong(Math.floorDiv(pos.getX(), 3), Math.floorDiv(pos.getY(), 3), Math.floorDiv(pos.getZ(), 3));
        return (RandomSupport.mixStafford13(seed ^ cell) & 0xFF) < 64;
    }
}
