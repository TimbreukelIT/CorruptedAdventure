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
import java.util.function.IntBinaryOperator;

/**
 * The stone circle of {@code peat_moors} ({@link BlightedLandmarks.Kind#MENHIR_CIRCLE}): seven to nine menhirs (4–7
 * high, most two wide, some 2 × 2) on a ring of radius 6.5–8.5, most of them leaning one block out or aside from half their height, one
 * now and then fallen flat or gone. Tidestone and cobblestone, patched with mossy cobblestone and mossy tidestone, moss on the
 * tops, vines down the sides. In the middle a small toxic pool in a ring of mud with dead bushes, round it a ring of
 * blighted glowcaps. Pure in the site and the pure ground heights.
 */
final class MenhirCircleShape {
    private MenhirCircleShape() {
    }

    static LandmarkPlan build(BlightedLandmarks.Site site, IntBinaryOperator ground) {
        RandomSource random = new XoroshiroRandomSource(site.seed());
        LandmarkPlan plan = new LandmarkPlan();
        // No stone or andesite: vanilla ore blobs of a neighbouring chunk replace those (the base stone), so a menhir
        // would hold ore or not by the order of generation (det check decor B). Tidestone's own ores stay below Y 48.
        List<BlockState> stones = List.of(ModBlocks.TIDESTONE.get().defaultBlockState(), ModBlocks.TIDESTONE.get().defaultBlockState(),
                Blocks.COBBLESTONE.defaultBlockState());
        // Mossy tidestone beside mossy cobblestone (2026-10-20); in no ore-host tag (not ring_4_stones), so no ore blob takes it.
        Mossy mossy = new Mossy(Blocks.MOSSY_COBBLESTONE.defaultBlockState(), ModBlocks.MOSSY_TIDESTONE.get().defaultBlockState());
        int count = 7 + random.nextInt(3);
        double radius = 6.5 + random.nextDouble() * 2.0;
        double start = random.nextDouble() * Math.PI * 2.0;
        int fallen = random.nextFloat() < 0.6F ? random.nextInt(count) : -1;
        int gone = random.nextFloat() < 0.4F ? random.nextInt(count) : -1;
        for (int k = 0; k < count; k++) {
            double angle = start + k * Math.PI * 2.0 / count + (random.nextDouble() - 0.5) * 0.25;
            int height = 4 + random.nextInt(4);
            float size = random.nextFloat();
            boolean wide = size < 0.6F;
            boolean thick = size < 0.15F;
            int leanAt = random.nextFloat() < 0.75F ? 1 + random.nextInt(Math.max(1, height - 2)) : height;
            boolean outward = random.nextFloat() < 0.6F;
            BlockState main = stones.get(random.nextInt(stones.size()));
            long blockSeed = random.nextLong();
            if (k == gone) {
                continue;
            }
            int x = Mth.floor(site.x() + 0.5 + Math.cos(angle) * radius);
            int z = Mth.floor(site.z() + 0.5 + Math.sin(angle) * radius);
            Direction out = Direction.getNearest(Math.cos(angle), 0.0, Math.sin(angle));
            Direction along = out.getClockWise();
            Direction lean = outward ? out : (random.nextBoolean() ? along : along.getOpposite());
            RandomSource blocks = new XoroshiroRandomSource(blockSeed);
            if (k == fallen) {
                fallenStone(plan, site, ground, x, z, out, along, height, wide, main, mossy, blocks);
            } else {
                standingStone(plan, site, ground, x, z, out, along, lean, leanAt, height, wide, thick, main, mossy, blocks);
            }
        }
        vines(plan, random);
        centre(plan, site, random, radius);
        return plan;
    }

    private static void standingStone(LandmarkPlan plan, BlightedLandmarks.Site site, IntBinaryOperator ground, int x, int z, Direction out, Direction along,
                                      Direction lean, int leanAt, int height, boolean wide, boolean thick, BlockState main,
                                      Mossy mossy, RandomSource random) {
        int base = ground.applyAsInt(x, z);
        for (int w = 0; w < (wide ? 2 : 1) * (thick ? 2 : 1); w++) {
            int cx = x + along.getStepX() * (w % 2) + out.getStepX() * (w / 2);
            int cz = z + along.getStepZ() * (w % 2) + out.getStepZ() * (w / 2);
            plan.feet.add(new LandmarkPlan.Foot(cx, cz, base, 2, main));
            BlockPos top = null;
            for (int dy = 0; dy < height; dy++) {
                boolean shifted = dy >= leanAt;
                BlockPos pos = new BlockPos(cx + (shifted ? lean.getStepX() : 0), base + dy, cz + (shifted ? lean.getStepZ() : 0));
                put(plan, site, pos, mossy.patch(random.nextFloat(), 0.4F, main));
                top = pos;
            }
            // Under the shifted part the stone also fills the block it leans over (no thin overhang at the joint).
            if (leanAt < height) {
                BlockPos joint = new BlockPos(cx + lean.getStepX(), base + leanAt - 1, cz + lean.getStepZ());
                if (!plan.objects.containsKey(joint)) {
                    put(plan, site, joint, main);
                }
            }
            dressTop(plan, top, random);
        }
    }

    private static void fallenStone(LandmarkPlan plan, BlightedLandmarks.Site site, IntBinaryOperator ground, int x, int z, Direction out, Direction along,
                                    int length, boolean wide, BlockState main, Mossy mossy, RandomSource random) {
        for (int i = 0; i < length; i++) {
            for (int w = 0; w < (wide ? 2 : 1); w++) {
                int cx = x + out.getStepX() * i + along.getStepX() * w;
                int cz = z + out.getStepZ() * i + along.getStepZ() * w;
                int base = ground.applyAsInt(cx, cz);
                BlockPos pos = new BlockPos(cx, base, cz);
                BlockState stone = mossy.patch(random.nextFloat(), 0.5F, main);
                if (put(plan, site, pos, stone)) {
                    plan.feet.add(new LandmarkPlan.Foot(cx, cz, base, 1, main));
                    dressTop(plan, pos, random);
                }
            }
        }
    }

    /**
     * The two mossy stones of the patches. One roll decides both whether a block is mossy and which moss it gets, so the
     * menhirs draw exactly as many numbers as before the mossy tidestone.
     */
    private record Mossy(BlockState cobblestone, BlockState tidestone) {
        /** {@code main} unless {@code roll < share}: then mossy cobblestone in the lower half of the share, else mossy tidestone. */
        BlockState patch(float roll, float share, BlockState main) {
            return roll >= share ? main : roll < share * 0.5F ? cobblestone : tidestone;
        }
    }

    /** Stone at {@code pos} unless it lies beyond the landmark's reach (a fallen stone's tip stays out). */
    private static boolean put(LandmarkPlan plan, BlightedLandmarks.Site site, BlockPos pos, BlockState state) {
        int reach = BlightedLandmarks.Kind.MENHIR_CIRCLE.radius();
        if (Math.abs(pos.getX() - site.x()) > reach || Math.abs(pos.getZ() - site.z()) > reach) {
            return false;
        }
        plan.objects.put(pos, state);
        return true;
    }

    /** Moss on a stone's top: a moss block cap or a carpet. */
    private static void dressTop(LandmarkPlan plan, BlockPos top, RandomSource random) {
        float roll = random.nextFloat();
        if (roll < 0.35F) {
            plan.objects.put(top, Blocks.MOSS_BLOCK.defaultBlockState());
        } else if (roll < 0.6F) {
            plan.decor.add(new LandmarkPlan.Decor(top.above(), Blocks.MOSS_CARPET.defaultBlockState(), top));
        }
    }

    /** Short vines down a few of the stones' faces. */
    private static void vines(LandmarkPlan plan, RandomSource random) {
        for (BlockPos pos : List.copyOf(plan.objects.keySet())) {
            float roll = random.nextFloat();
            Direction side = Direction.from2DDataValue(random.nextInt(4));
            int length = 1 + random.nextInt(3);
            if (roll < 0.07F && !plan.solid(pos.relative(side)) && !plan.solid(pos.relative(side).below())) {
                plan.vine(pos, side, length);
            }
        }
    }

    /**
     * The middle: a toxic pool of three to five columns (one deep; {@link SludgePuddle} checks it is closed), mud round
     * it, dead bushes on the mud, and a ring of blighted glowcaps between the pool and the stones.
     */
    private static void centre(LandmarkPlan plan, BlightedLandmarks.Site site, RandomSource random, double radius) {
        int cx = site.x();
        int cz = site.z();
        plan.pool.add(new BlockPos(cx, 0, cz));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (random.nextFloat() < 0.55F) {
                plan.pool.add(new BlockPos(cx + direction.getStepX(), 0, cz + direction.getStepZ()));
            }
        }
        BlockState mud = Blocks.MUD.defaultBlockState();
        BlockState coarse = Blocks.COARSE_DIRT.defaultBlockState();
        double ring = radius - 2.5;
        for (int dz = -Mth.ceil(radius); dz <= Mth.ceil(radius); dz++) {
            for (int dx = -Mth.ceil(radius); dx <= Mth.ceil(radius); dx++) {
                float soil = random.nextFloat();
                float plant = random.nextFloat();
                double d = Math.sqrt(dx * dx + dz * dz);
                BlockPos column = new BlockPos(cx + dx, 0, cz + dz);
                if (plan.pool.contains(column)) {
                    continue;
                }
                if (d <= 2.3 || d <= 3.2 && soil < 0.5F) {
                    plan.ground.put(column, soil < 0.8F ? mud : coarse);
                    if (plant < 0.3F) {
                        plan.groundDecor.put(column, Blocks.DEAD_BUSH.defaultBlockState());
                    }
                } else if (Math.abs(d - ring) < 0.55 && plant < 0.6F) {
                    plan.groundDecor.put(column, ModBlocks.BLIGHTED_GLOWCAP.get().defaultBlockState());
                }
            }
        }
    }
}
