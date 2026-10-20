package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import nl.timtoys.corruptedadventure.block.VerdantVineBlock;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTrunkPlacerTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * {@code corruptedadventure:verdant_giant_trunk_placer} (ring 4 decor D, 2026-10-19): the trunk of the giant verdant
 * tree beside the horseshoe gorges of {@code verdant_falls}, a landmark a size up from the emergent tree
 * ({@link VerdantTrunkPlacer}):
 * <ul>
 *   <li>a 2 × 2 trunk with a flared foot (a ring of wood {@link #FLARE} high round it) and large buttress roots of
 *   wood, {@code buttresses} fins of {@code buttress_length} that step down outwards and reach down to the ground;</li>
 *   <li>two tiers of long branches ({@code branches} each, {@code branch_length} out, one up per two steps), each
 *   carrying a small crown of its own ({@link #BRANCH_CROWN} smaller than the top) and a vine or hanging orchid;</li>
 *   <li>eight short arms ({@link #TOP_ARM}) under the wide top crown, so every leaf of the umbrella lies within the
 *   leaves' reach of a log (no leaf decays);</li>
 *   <li>fruit pods and orchids on the trunk above the flare.</li>
 * </ul>
 * Its reach from the origin stays within {@link #MAX_REACH} (branch, crown, buttresses), so the isolated tree writes
 * only into the 3 × 3 chunks round its own, also with the edge updates. Everything goes through the tree's own block
 * setters ({@link TreePlan}): the shape is the tree's random alone, whatever stands round it.
 */
public final class VerdantGiantTrunkPlacer extends TrunkPlacer {
    // Vanilla's three trunk fields (as trunkPlacerParts) and six own ones in one group: Products cannot join 3 and 6.
    public static final MapCodec<VerdantGiantTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 32).fieldOf("base_height").forGetter(placer -> placer.baseHeight),
            Codec.intRange(0, 24).fieldOf("height_rand_a").forGetter(placer -> placer.heightRandA),
            Codec.intRange(0, 24).fieldOf("height_rand_b").forGetter(placer -> placer.heightRandB),
            IntProvider.codec(0, 8).fieldOf("buttresses").forGetter(placer -> placer.buttresses),
            IntProvider.codec(1, 6).fieldOf("buttress_length").forGetter(placer -> placer.buttressLength),
            IntProvider.codec(0, 4).fieldOf("branches").forGetter(placer -> placer.branches),
            IntProvider.codec(2, 7).fieldOf("branch_length").forGetter(placer -> placer.branchLength),
            Codec.floatRange(0.0F, 1.0F).fieldOf("pod_chance").forGetter(placer -> placer.podChance),
            Codec.floatRange(0.0F, 1.0F).fieldOf("orchid_chance").forGetter(placer -> placer.orchidChance)
    ).apply(instance, VerdantGiantTrunkPlacer::new));

    /** The flared foot: a ring of wood round the trunk this many blocks high. */
    static final int FLARE = 3;
    /** A buttress fin's height next to the flare; it falls linearly to 1 at its tip. */
    static final int BUTTRESS_HEIGHT = 6;
    /** How far a root piece reaches down to find the ground. */
    static final int ROOT_REACH_DOWN = 4;
    /** The tiers of branches, as shares of the trunk's height. */
    static final double[] TIERS = {0.55, 0.75};
    /** The branches' crowns are this much smaller (radius) than the top crown. */
    static final int BRANCH_CROWN = 4;
    /** The arms under the top crown reach this far out from the trunk. */
    static final int TOP_ARM = 5;
    /**
     * The furthest block from the origin (the trunk's −x/−z corner) a tree of this placer sets: a diagonal branch of
     * 7 plus its crown (9 − {@link #BRANCH_CROWN}) and the 2 × 2 trunk's other side; checked by the unit test.
     */
    static final int MAX_REACH = 13;

    /** The eight directions (four axes, four diagonals). */
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private final IntProvider buttresses;
    private final IntProvider buttressLength;
    private final IntProvider branches;
    private final IntProvider branchLength;
    private final float podChance;
    private final float orchidChance;

    public VerdantGiantTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, IntProvider buttresses, IntProvider buttressLength,
                                   IntProvider branches, IntProvider branchLength, float podChance, float orchidChance) {
        super(baseHeight, heightRandA, heightRandB);
        this.buttresses = buttresses;
        this.buttressLength = buttressLength;
        this.branches = branches;
        this.branchLength = branchLength;
        this.podChance = podChance;
        this.orchidChance = orchidChance;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return ModTrunkPlacerTypes.VERDANT_GIANT_TRUNK_PLACER.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter,
                                                            RandomSource random, int freeTreeHeight, BlockPos pos, TreeConfiguration config) {
        BlockState wood = ModBlocks.VERDANT_WOOD.get().defaultBlockState();
        // The 2 × 2 trunk, each column reaching down to its own ground (the origin's ground may lie a block higher).
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                BlockPos column = pos.offset(dx, 0, dz);
                int down = groundBelow(level, column);
                setDirtAt(level, blockSetter, random, column.below(down + 1), config);
                for (int y = -down; y < freeTreeHeight; y++) {
                    placeLog(level, blockSetter, random, column.above(y), config);
                }
            }
        }
        // The flare: the ring of eight round the trunk.
        for (int[] ring : new int[][]{{-1, 0}, {-1, 1}, {2, 0}, {2, 1}, {0, -1}, {1, -1}, {0, 2}, {1, 2}}) {
            root(level, blockSetter, pos.offset(ring[0], 0, ring[1]), FLARE, wood);
        }
        List<int[]> sides = shuffled(random);
        int fins = Math.min(DIRECTIONS.length, buttresses.sample(random));
        for (int i = 0; i < fins; i++) {
            buttress(level, blockSetter, random, pos, sides.get(i), wood);
        }
        List<FoliagePlacer.FoliageAttachment> crowns = new ArrayList<>();
        List<int[]> arms = shuffled(random);
        int next = 0;
        for (double tier : TIERS) {
            int count = branches.sample(random);
            int y = Mth.floor(tier * freeTreeHeight);
            for (int i = 0; i < count; i++) {
                BlockPos end = branch(level, blockSetter, random, pos.above(y), arms.get(next++ % arms.size()), branchLength.sample(random), config);
                if (end != null) {
                    crowns.add(new FoliagePlacer.FoliageAttachment(end.above(), -BRANCH_CROWN, false));
                    hang(level, blockSetter, random, end.below());
                }
            }
        }
        BlockPos top = pos.above(freeTreeHeight - 1);
        for (int[] arm : DIRECTIONS) {
            branch(level, blockSetter, random, top.below(1), arm, TOP_ARM, config);
        }
        if (podChance > 0.0F || orchidChance > 0.0F) {
            dress(level, blockSetter, random, pos, freeTreeHeight);
        }
        crowns.add(new FoliagePlacer.FoliageAttachment(top.above(), 0, true));
        return crowns;
    }

    /** How many blocks below {@code column} the ground lies (0: right below), up to {@link #ROOT_REACH_DOWN}. */
    private static int groundBelow(LevelSimulatedReader level, BlockPos column) {
        int down = 0;
        while (down < ROOT_REACH_DOWN && open(level, column.below(down + 1))) {
            down++;
        }
        return down;
    }

    /**
     * One piece of root wood {@code height} high at {@code base}, reaching down to the ground; nothing where the ground
     * lies out of reach or is a fluid (no wood hanging over a drop, damming a brook or standing in a lake).
     */
    private static boolean root(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, BlockPos base, int height,
                                BlockState wood) {
        int down = 0;
        while (down < ROOT_REACH_DOWN && open(level, base.below(down))) {
            down++;
        }
        if (open(level, base.below(down)) || level.isFluidAtPosition(base.below(down), fluid -> !fluid.isEmpty())) {
            return false;
        }
        for (int y = height - 1; y > -down - 1; y--) {
            BlockPos at = base.above(y);
            if (open(level, at)) {
                blockSetter.accept(at, wood);
            }
        }
        return true;
    }

    /** One buttress fin out from the trunk's side in {@code direction}: falling from {@link #BUTTRESS_HEIGHT} to 1. */
    private void buttress(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, BlockPos pos,
                          int[] direction, BlockState wood) {
        int length = buttressLength.sample(random);
        // From the trunk's face on that side: along an axis from one of its two columns, diagonally from its corner.
        int x = direction[0] > 0 ? 1 : 0;
        int z = direction[1] > 0 ? 1 : 0;
        int pick = random.nextInt(2);
        if (direction[0] == 0) {
            x = pick;
        } else if (direction[1] == 0) {
            z = pick;
        }
        // Past the flare along an axis; diagonally from the flare's open corner on.
        int past = direction[0] != 0 && direction[1] != 0 ? 0 : 1;
        for (int d = 1; d <= length; d++) {
            int height = Math.max(1, Math.round(BUTTRESS_HEIGHT * (1.0F - (d - 1) / (float) length)));
            if (!root(level, blockSetter, pos.offset(x + direction[0] * (d + past), 0, z + direction[1] * (d + past)), height, wood)) {
                return;
            }
        }
    }

    /**
     * One branch from the trunk's side at {@code from}'s height: out in {@code direction}, one up per two steps; returns
     * its end (null when it met something it may not replace).
     */
    private BlockPos branch(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, BlockPos from,
                            int[] direction, int length, TreeConfiguration config) {
        Direction.Axis axis = direction[0] != 0 ? Direction.Axis.X : Direction.Axis.Z;
        // Leave the 2 × 2 trunk from the column on that side.
        BlockPos at = from.offset(direction[0] > 0 ? 1 : 0, 0, direction[1] > 0 ? 1 : 0);
        for (int step = 1; step <= length; step++) {
            at = at.offset(direction[0], step % 2 == 0 ? 1 : 0, direction[1]);
            if (!TreeFeature.validTreePos(level, at)) {
                return null;
            }
            blockSetter.accept(at, config.trunkProvider.getState(random, at).trySetValue(RotatedPillarBlock.AXIS, axis));
        }
        return at;
    }

    /** A vine (sometimes flowering) or a hanging orchid under a branch's end, as on the emergent tree. */
    private static void hang(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, BlockPos below) {
        // Always the draws, so the next branch's random does not depend on what stands here.
        float kind = random.nextFloat();
        boolean flowering = random.nextBoolean();
        int length = 2 + random.nextInt(5);
        if (!level.isStateAtPosition(below.below(2), BlockState::isAir)) {
            return;
        }
        if (kind >= 0.65F) {
            if (level.isStateAtPosition(below, BlockState::isAir)) {
                blockSetter.accept(below, ModBlocks.VERDANT_HANGING_ORCHID.get().defaultBlockState());
            }
            return;
        }
        for (int i = 0; i < length; i++) {
            BlockPos vine = below.below(i);
            if (!level.isStateAtPosition(vine, BlockState::isAir)) {
                break;
            }
            boolean tip = i == length - 1 || !level.isStateAtPosition(vine.below(), BlockState::isAir);
            BlockState state = tip
                    ? ModBlocks.VERDANT_VINE.get().defaultBlockState().setValue(VerdantVineBlock.AGE, VerdantVineBlock.MAX_AGE)
                    : ModBlocks.VERDANT_VINE_PLANT.get().defaultBlockState();
            blockSetter.accept(vine, state.setValue(VerdantVineBlock.FLOWERING, flowering));
            if (tip) {
                break;
            }
        }
    }

    /** Fruit pods (facing the trunk) and orchids (facing away) on the trunk's eight side faces above the flare. */
    private void dress(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, BlockPos pos, int height) {
        int[][] faces = {{-1, 0}, {-1, 1}, {2, 0}, {2, 1}, {0, -1}, {1, -1}, {0, 2}, {1, 2}};
        Direction[] away = {Direction.WEST, Direction.WEST, Direction.EAST, Direction.EAST, Direction.NORTH, Direction.NORTH,
                Direction.SOUTH, Direction.SOUTH};
        for (int y = FLARE + 1; y < height - 3; y++) {
            for (int i = 0; i < faces.length; i++) {
                float roll = random.nextFloat();
                int age = random.nextInt(3);
                BlockPos at = pos.offset(faces[i][0], y, faces[i][1]);
                if (!level.isStateAtPosition(at, BlockState::isAir)) {
                    continue;
                }
                BlockState state = null;
                if (roll < podChance) {
                    state = ModBlocks.VERDANT_FRUIT_POD.get().defaultBlockState().setValue(CocoaBlock.FACING, away[i].getOpposite())
                            .setValue(CocoaBlock.AGE, age);
                } else if (roll < podChance + orchidChance) {
                    state = ModBlocks.VERDANT_ORCHID.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, away[i]);
                }
                if (state != null && (!(level instanceof LevelReader reader) || state.canSurvive(reader, at))) {
                    blockSetter.accept(at, state);
                }
            }
        }
    }

    /** A spot a root piece may take: what a tree may replace, but no fluid. */
    private static boolean open(LevelSimulatedReader level, BlockPos pos) {
        return TreeFeature.validTreePos(level, pos) && level.isFluidAtPosition(pos, fluid -> fluid.isEmpty());
    }

    /** The eight directions in the tree's own order (a Fisher–Yates shuffle with its random). */
    private static List<int[]> shuffled(RandomSource random) {
        List<int[]> list = new ArrayList<>(List.of(DIRECTIONS));
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int[] swap = list.get(i);
            list.set(i, list.get(j));
            list.set(j, swap);
        }
        return list;
    }
}
