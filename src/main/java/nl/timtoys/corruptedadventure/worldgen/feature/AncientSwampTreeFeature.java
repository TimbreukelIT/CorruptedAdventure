package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * {@code corruptedadventure:ancient_swamp_tree} (ring 4 decor rollout group A, 2026-10-19): the eye-catcher of
 * {@code glowroot_swamp}, an ancient mangrove with a 3 × 3 trunk 26–34 high on a skirt of prop roots, a few great limbs
 * carrying a wide, flat crown, and long strands of hanging moss with glowing roots under it.
 * <p>
 * One tree at most per landmark cell ({@link Landmarks}), written whole from the chunk that holds its anchor, as a ring 4
 * tree: it grows in a {@link TreePlan} against the terrain without the other trees, from its own random, and merges with
 * them by their priority ({@link TreeOverlap#wins}). Only an anchor {@link #EDGE} inside its chunk is taken, so the tree
 * fits in the chunks a feature may write ({@link #REACH}).
 */
public final class AncientSwampTreeFeature extends Feature<AncientSwampTreeConfiguration> {
    static final int MAX_HEIGHT = 40;
    /** The anchor keeps this far from its chunk's edges. */
    static final int EDGE = 5;
    /** The tree's reach from the trunk's centre (16 + {@link #EDGE} − 1: inside the eight chunks around). */
    static final int REACH = 20;
    /** The deepest water the trunk stands in, the highest bank. */
    static final int MAX_DEPTH = 3;
    static final int MAX_BANK = 6;
    private static final long SALT = 0xA9C1_E47L;

    public AncientSwampTreeFeature(Codec<AncientSwampTreeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<AncientSwampTreeConfiguration> context) {
        WorldGenLevel level = context.level();
        IslandField field = Ring4Terrain.field(level);
        if (field == null || !(level instanceof WorldGenRegion)) {
            return false;
        }
        AncientSwampTreeConfiguration config = context.config();
        ChunkPos chunk = new ChunkPos(context.origin());
        int sea = level.getSeaLevel();
        Landmarks.Candidate at = Landmarks.anchoredIn(Landmarks.world(level), level.getSeed(), SALT, config.grid(), chunk,
                candidate -> accepts(level, field, config, sea, candidate));
        if (at == null) {
            return false;
        }
        // The ground from the terrain alone, not OCEAN_FLOOR_WG: under a high island that is the island's top.
        TreeOverlap.Terrain terrain = IsolatedTreeFeature.terrain(level);
        int ground = RootArch.groundTop(level, terrain, at.x(), at.z(), sea + MAX_BANK + 2, sea - MAX_DEPTH - 4);
        if (ground == Integer.MIN_VALUE) {
            return false;
        }
        BlockPos base = new BlockPos(at.x(), ground, at.z());
        TreePlan plan = new TreePlan(level, terrain);
        new Grower(plan, config, at.random(), base, chunk).grow();
        if (plan.isEmpty()) {
            return false;
        }
        plan.settleLeafDistances();
        plan.commit();
        return true;
    }

    /** Pure: the swamp's shallow water or a low bank, no island low overhead, no lift, fall or world tree near. */
    private static boolean accepts(WorldGenLevel level, IslandField field, AncientSwampTreeConfiguration config, int sea,
                                   Landmarks.Candidate at) {
        // Inside its chunk by EDGE, so the whole tree fits in the chunks its own may write.
        if ((at.x() & 15) < EDGE || (at.x() & 15) > 15 - EDGE || (at.z() & 15) < EDGE || (at.z() & 15) > 15 - EDGE) {
            return false;
        }
        double ground = field.column(at.x(), at.z()).surface();
        if (ground < sea - 1 - MAX_DEPTH || ground > sea + MAX_BANK) {
            return false;
        }
        int minX = at.x() - REACH - 16;
        int minZ = at.z() - REACH - 16;
        int maxX = at.x() + REACH + 16;
        int maxZ = at.z() + REACH + 16;
        int top = sea + MAX_HEIGHT + 12;
        return field.islandsWithin(minX, minZ, maxX, maxZ).stream().noneMatch(island -> island.bottom() < top)
                && field.liftsWithin(minX, minZ, maxX, maxZ).isEmpty() && field.waterfallsWithin(minX, minZ, maxX, maxZ).isEmpty()
                && field.worldTrees(minX - 64, minZ - 64, maxX + 64, maxZ + 64).isEmpty()
                && Landmarks.biomeAround(level, at.x(), at.z(), 12, config.biome());
    }

    /** Grows the tree into the plan, all from one random in a fixed order. */
    private static final class Grower {
        private final TreePlan plan;
        private final AncientSwampTreeConfiguration config;
        private final RandomSource random;
        private final BlockPos base;
        private final ChunkPos chunk;
        private final BiConsumer<BlockPos, BlockState> trunk;
        private final BiConsumer<BlockPos, BlockState> roots;
        private final BiConsumer<BlockPos, BlockState> decor;
        private final FoliagePlacer.FoliageSetter foliage;
        private final Set<BlockPos> leaves = new LinkedHashSet<>();
        private final Set<BlockPos> limbs = new LinkedHashSet<>();
        private final List<BlockPos> rootBlocks = new ArrayList<>();

        Grower(TreePlan plan, AncientSwampTreeConfiguration config, RandomSource random, BlockPos base, ChunkPos chunk) {
            this.plan = plan;
            this.config = config;
            this.random = random;
            this.base = base;
            this.chunk = chunk;
            this.trunk = plan.trunkSetter();
            this.roots = plan.rootSetter();
            this.decor = plan.decorationSetter();
            this.foliage = plan.foliageSetter();
        }

        void grow() {
            int height = config.height().sample(random);
            int top = base.getY() + height;
            // The trunk: 3 × 3, the corners give way to a cross two thirds up, one log at the very top.
            for (int y = base.getY(); y < top; y++) {
                double share = (y - base.getY()) / (double) height;
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        boolean corner = dx != 0 && dz != 0;
                        boolean edge = dx != 0 || dz != 0;
                        if (corner && share > 0.62 || edge && share > 0.9) {
                            continue;
                        }
                        log(base.offset(dx, y - base.getY(), dz), Direction.Axis.Y);
                    }
                }
            }
            propRoots();
            int branches = config.branches().sample(random);
            double start = random.nextDouble() * Math.PI * 2.0;
            for (int i = 0; i < branches; i++) {
                double heading = start + i * Math.PI * 2.0 / branches + (random.nextDouble() - 0.5) * 0.7;
                int from = base.getY() + (int) Math.round(height * (0.58 + random.nextDouble() * 0.3));
                limb(heading, from, 5 + random.nextInt(4), 2 + random.nextInt(4));
            }
            // The crown's top over the trunk.
            blob(base.atY(top), 4.5 + random.nextDouble(), 2.5);
            hangUnderCrown();
            decorateWood();
        }

        /** 8–12 prop roots from the trunk's foot, out and down into the water and the mud. */
        private void propRoots() {
            int count = 8 + random.nextInt(5);
            double start = random.nextDouble() * Math.PI * 2.0;
            for (int i = 0; i < count; i++) {
                double heading = start + i * Math.PI * 2.0 / count + (random.nextDouble() - 0.5) * 0.5;
                double reach = 4.0 + random.nextDouble() * 4.0;
                double rise = 2.0 + random.nextDouble() * 4.0;
                double cos = Math.cos(heading);
                double sin = Math.sin(heading);
                int steps = (int) Math.ceil((reach + rise) * 3.0);
                for (int s = 0; s <= steps; s++) {
                    double t = s / (double) steps;
                    double out = 1.5 + reach * t;
                    double y = base.getY() + rise * Math.cos(t * Math.PI / 2.0) - 1.5 * t * t;
                    BlockPos pos = BlockPos.containing(base.getX() + 0.5 + cos * out, y, base.getZ() + 0.5 + sin * out);
                    if (!root(pos)) {
                        break;
                    }
                }
            }
        }

        /** One root block: in open space a root, in mud a muddy root (and the root ends), else the root ends. */
        private boolean root(BlockPos pos) {
            if (!reachable(pos)) {
                return false;
            }
            BlockState here = plan.getBlockState(pos);
            if (here.is(Blocks.MUD)) {
                set(roots, pos, config.muddyRoots());
                return false;
            }
            if (here.is(config.log().getBlock()) || here.is(config.roots().getBlock()) || here.is(config.muddyRoots().getBlock())) {
                return true;
            }
            if (!TreeFeature.validTreePos(plan, pos) && !here.getFluidState().isSource()) {
                return false;
            }
            set(roots, pos, waterlogged(config.roots(), pos));
            rootBlocks.add(pos);
            return true;
        }

        /** A limb out from the trunk, rising, with a leafy blob at its end. */
        private void limb(double heading, int fromY, int length, int rise) {
            double cos = Math.cos(heading);
            double sin = Math.sin(heading);
            Direction.Axis axis = Math.abs(cos) > Math.abs(sin) ? Direction.Axis.X : Direction.Axis.Z;
            BlockPos end = base.atY(fromY);
            int steps = length * 2;
            for (int s = 1; s <= steps; s++) {
                double t = s / (double) steps;
                double out = 1.0 + length * t;
                BlockPos pos = BlockPos.containing(base.getX() + 0.5 + cos * out, fromY + rise * Math.sqrt(t), base.getZ() + 0.5 + sin * out);
                if (!reachable(pos) || !TreeFeature.validTreePos(plan, pos) && !plan.getBlockState(pos).is(config.log().getBlock())) {
                    break;
                }
                log(pos, t < 0.85 ? axis : Direction.Axis.Y);
                limbs.add(pos);
                end = pos;
            }
            blob(end.above(), 3.5 + random.nextDouble() * 1.5, 1.8 + random.nextDouble() * 0.7);
        }

        /** A flat ball of leaves round a log at its middle (every leaf within six steps of a log: none decays). */
        private void blob(BlockPos centre, double radius, double half) {
            log(centre, Direction.Axis.Y);
            int r = Mth.ceil(radius);
            int h = Mth.ceil(half);
            for (int dy = -h; dy <= h; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dx = -r; dx <= r; dx++) {
                        double d = (dx * dx + dz * dz) / (radius * radius) + (dy * dy) / (half * half);
                        float ragged = random.nextFloat();
                        if (d > 1.0 - 0.25 * ragged || Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > 6) {
                            continue;
                        }
                        BlockPos pos = centre.offset(dx, dy, dz);
                        if (reachable(pos) && TreeFeature.validTreePos(plan, pos) && !leaves.contains(pos)) {
                            foliage.set(pos, waterlogged(config.leaves(), pos));
                            leaves.add(pos);
                        }
                    }
                }
            }
        }

        /** Long strands of moss under the crown, single glowing roots, a propagule now and then. */
        private void hangUnderCrown() {
            for (BlockPos leaf : leaves) {
                float roll = random.nextFloat();
                int length = 3 + random.nextInt(9);
                BlockPos below = leaf.below();
                if (leaves.contains(below) || !plan.isStateAtPosition(below, BlockState::isAir)) {
                    continue;
                }
                // Leaves give moss no hold: a strand or glowing root hangs from an aerial root under the leaf.
                if (roll < 0.53F && reachable(below)) {
                    set(roots, below, config.roots());
                    if (roll < 0.45F) {
                        strand(below.below(), length);
                    } else {
                        hangOne(below.below(), config.tip());
                    }
                } else if (roll < 0.57F) {
                    hangOne(below, config.propagule().trySetValue(MangrovePropaguleBlock.AGE, 4));
                }
            }
        }

        /** Moss and glowcaps on top of the roots and limbs, a glowing root or a short strand under the limbs. */
        private void decorateWood() {
            for (BlockPos pos : rootBlocks) {
                float roll = random.nextFloat();
                BlockState state = config.rootDecor().getState(random, pos.above());
                if (roll < 0.35F && plan.isStateAtPosition(pos.above(), BlockState::isAir) && reachable(pos.above())) {
                    set(decor, pos.above(), state);
                }
            }
            for (BlockPos pos : limbs) {
                float roll = random.nextFloat();
                int length = 1 + random.nextInt(4);
                BlockPos below = pos.below();
                if (!plan.isStateAtPosition(below, BlockState::isAir)) {
                    continue;
                }
                if (roll < 0.25F) {
                    hangOne(below, config.tip());
                } else if (roll < 0.5F) {
                    strand(below, length);
                }
            }
        }

        private void strand(BlockPos from, int length) {
            for (int i = 0; i < length; i++) {
                BlockPos pos = from.below(i);
                if (!reachable(pos) || !plan.isStateAtPosition(pos, BlockState::isAir)) {
                    return;
                }
                set(decor, pos, config.strand());
            }
        }

        private void hangOne(BlockPos pos, BlockState state) {
            if (reachable(pos) && plan.isStateAtPosition(pos, BlockState::isAir)) {
                set(decor, pos, state);
            }
        }

        private void log(BlockPos pos, Direction.Axis axis) {
            if (reachable(pos)) {
                set(trunk, pos, config.log().trySetValue(RotatedPillarBlock.AXIS, axis));
            }
        }

        private void set(BiConsumer<BlockPos, BlockState> setter, BlockPos pos, BlockState state) {
            setter.accept(pos.immutable(), state);
        }

        private BlockState waterlogged(BlockState state, BlockPos pos) {
            return state.hasProperty(BlockStateProperties.WATERLOGGED)
                    ? state.setValue(BlockStateProperties.WATERLOGGED, plan.getFluidState(pos).isSource()) : state;
        }

        /** Inside the chunks the anchor's chunk may write (itself and the eight around it). */
        private boolean reachable(BlockPos pos) {
            return Math.abs((pos.getX() >> 4) - chunk.x) <= 1 && Math.abs((pos.getZ() >> 4) - chunk.z) <= 1
                    && pos.getY() > plan.getMinBuildHeight() && pos.getY() < plan.getMaxBuildHeight();
        }
    }
}
