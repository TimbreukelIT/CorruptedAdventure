package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;

/**
 * The basalt organ of {@code basalt_flows} ({@link VolcanicLandmarks.Kind#BASALT_ORGAN}): 10–30 basalt columns packed on
 * a jittered hexagonal grid, each a small polygon of one to five blocks, 8–20 high, tallest in the middle (a dome) or
 * towards one side (a ramp), some broken short; basalt or polished basalt with heads of smooth basalt, a few fallen
 * pieces on the ground round it. Pure in the site and the pure ground heights; null where the ground is not level.
 */
final class BasaltOrganShape {
    /** One column: its middle, radius, height above the site's ground and whether it is polished. */
    private record Column(double x, double z, double radius, int height, boolean polished, boolean doubleHead) {
    }

    private BasaltOrganShape() {
    }

    @Nullable
    static VolcanicPlan build(VolcanicLandmarks.Site site, IntBinaryOperator ground) {
        int g = site.ground();
        double cx = site.x() + 0.5;
        double cz = site.z() + 0.5;
        if (!VolcanicLandmarks.level(ground, cx, cz, 8, 4, g)) {
            return null;
        }
        RandomSource random = new XoroshiroRandomSource(site.seed());
        int count = 10 + random.nextInt(21);
        double spacing = 2.0 + random.nextDouble() * 0.4;
        int tallest = 14 + random.nextInt(7);
        boolean ramp = random.nextFloat() < 0.4F;
        double rampAngle = random.nextDouble() * Math.PI * 2.0;

        // The hexagonal grid round the middle; the count nearest (a little shuffled) are the columns.
        record Spot(double x, double z, double order) {
        }
        List<Spot> spots = new ArrayList<>();
        for (int r = -5; r <= 5; r++) {
            for (int q = -5; q <= 5; q++) {
                double x = cx + spacing * (q + r / 2.0) + (random.nextDouble() - 0.5) * 0.6;
                double z = cz + spacing * r * Math.sqrt(3.0) / 2.0 + (random.nextDouble() - 0.5) * 0.6;
                double d = Math.sqrt(Mth.square(x - cx) + Mth.square(z - cz));
                spots.add(new Spot(x, z, d + random.nextDouble() * spacing * 0.8));
            }
        }
        spots.sort(Comparator.comparingDouble(Spot::order));
        List<Spot> chosen = spots.subList(0, count);
        double extent = 0.0;
        double low = Double.MAX_VALUE;
        double high = -Double.MAX_VALUE;
        for (Spot spot : chosen) {
            extent = Math.max(extent, Math.sqrt(Mth.square(spot.x() - cx) + Mth.square(spot.z() - cz)));
            double along = (spot.x() - cx) * Math.cos(rampAngle) + (spot.z() - cz) * Math.sin(rampAngle);
            low = Math.min(low, along);
            high = Math.max(high, along);
        }
        List<Column> columns = new ArrayList<>();
        for (Spot spot : chosen) {
            double f;
            if (ramp) {
                double along = (spot.x() - cx) * Math.cos(rampAngle) + (spot.z() - cz) * Math.sin(rampAngle);
                f = high > low ? (along - low) / (high - low) : 1.0;
            } else {
                double d = Math.sqrt(Mth.square(spot.x() - cx) + Mth.square(spot.z() - cz));
                f = 1.0 - Math.pow(extent > 0.0 ? d / extent : 0.0, 1.2);
            }
            int height = 8 + (int) Math.round((tallest - 8) * f) + random.nextInt(3) - 1;
            if (random.nextFloat() < 0.15F) {
                height = Math.max(3, (int) (height * (0.4 + random.nextDouble() * 0.3)));
            }
            columns.add(new Column(spot.x(), spot.z(), 0.85 + random.nextDouble() * 0.45, height, random.nextFloat() < 0.35F,
                    random.nextFloat() < 0.3F));
        }

        // Per block the tallest column that covers it.
        Map<BlockPos, Column> blocks = new LinkedHashMap<>();
        for (Column column : columns) {
            int r = Mth.ceil(column.radius());
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    int x = Mth.floor(column.x()) + dx;
                    int z = Mth.floor(column.z()) + dz;
                    if (Mth.square(x + 0.5 - column.x()) + Mth.square(z + 0.5 - column.z()) > Mth.square(column.radius())) {
                        continue;
                    }
                    blocks.merge(new BlockPos(x, 0, z), column, (a, b) -> b.height() > a.height() ? b : a);
                }
            }
        }
        VolcanicPlan plan = new VolcanicPlan(site.x(), site.z(), site.kind().reach());
        BlockState basalt = Blocks.BASALT.defaultBlockState();
        BlockState polished = Blocks.POLISHED_BASALT.defaultBlockState();
        BlockState head = Blocks.SMOOTH_BASALT.defaultBlockState();
        blocks.forEach((column, owner) -> {
            int x = column.getX();
            int z = column.getZ();
            int top = g + owner.height();
            int head0 = top - (owner.doubleHead() ? 1 : 0);
            BlockState body = owner.polished() ? polished : basalt;
            for (int y = ground.applyAsInt(x, z) - 1; y <= top; y++) {
                plan.put(new BlockPos(x, y, z), y >= head0 ? head : body);
            }
            plan.foot(x, z, ground.applyAsInt(x, z) - 1, 1, basalt);
        });

        // Fallen pieces: short lengths of column lying on the ground just outside the organ.
        int pieces = 2 + random.nextInt(3);
        for (int k = 0; k < pieces; k++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            double d = extent + 2.0 + random.nextDouble() * 2.0;
            int len = 2 + random.nextInt(3);
            double turn = a + Math.PI / 2.0 + (random.nextDouble() - 0.5);
            Direction.Axis axis = Math.abs(Math.cos(turn)) >= Math.abs(Math.sin(turn)) ? Direction.Axis.X : Direction.Axis.Z;
            BlockState piece = (random.nextBoolean() ? polished : basalt).setValue(RotatedPillarBlock.AXIS, axis);
            for (int s = 0; s < len; s++) {
                int x = Mth.floor(cx + Math.cos(a) * d + Math.cos(turn) * s);
                int z = Mth.floor(cz + Math.sin(a) * d + Math.sin(turn) * s);
                plan.groundDecor(x, z, piece);
            }
        }
        return plan;
    }
}
