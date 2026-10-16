package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * {@code corruptedadventure:verdant_trunk_placer} (75b, F2 = A): the trunk of the verdant tree in its two forms. A
 * straight trunk; for the emergent tree (14–22 high) with buttress roots of wood at its foot (fins of 2–3 blocks that
 * step down outwards and reach down to the ground) and short branches under its umbrella crown, each carrying a vine
 * (sometimes flowering) or a hanging orchid below it; fruit pods (H3 = A) and orchids on the trunk. The tree fern
 * (4–7) is the bare trunk with its frond crown. The single foliage attachment is the block above the top log.
 * <p>
 * Everything goes through the tree's own block setter, so the tree's leaves count the trunk, roots and branches for
 * their distance; the pods and orchids are set only where they can stand.
 */
public final class VerdantTrunkPlacer extends TrunkPlacer {
    public static final MapCodec<VerdantTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(instance -> trunkPlacerParts(instance)
            .and(instance.group(
                    IntProvider.codec(0, 4).fieldOf("buttresses").forGetter(placer -> placer.buttresses),
                    IntProvider.codec(0, 8).fieldOf("branches").forGetter(placer -> placer.branches),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("pod_chance").forGetter(placer -> placer.podChance),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("orchid_chance").forGetter(placer -> placer.orchidChance)))
            .apply(instance, VerdantTrunkPlacer::new));

    /** A buttress root steps down outwards: its height at distance 1, 2, 3 from the trunk. */
    static final int[] BUTTRESS_HEIGHTS = {3, 2, 1};
    /** How far a root or branch end reaches down to find the ground (a slope). */
    static final int ROOT_REACH_DOWN = 3;
    /** A branch runs out this far from the trunk, one step up. */
    static final int BRANCH_LENGTH = 3;

    private final IntProvider buttresses;
    private final IntProvider branches;
    private final float podChance;
    private final float orchidChance;

    public VerdantTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, IntProvider buttresses, IntProvider branches,
                              float podChance, float orchidChance) {
        super(baseHeight, heightRandA, heightRandB);
        this.buttresses = buttresses;
        this.branches = branches;
        this.podChance = podChance;
        this.orchidChance = orchidChance;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return ModTrunkPlacerTypes.VERDANT_TRUNK_PLACER.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter,
                                                            RandomSource random, int freeTreeHeight, BlockPos pos, TreeConfiguration config) {
        setDirtAt(level, blockSetter, random, pos.below(), config);
        for (int y = 0; y < freeTreeHeight; y++) {
            placeLog(level, blockSetter, random, pos.above(y), config);
        }
        List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        shuffle(sides, random);
        int roots = Math.min(4, buttresses.sample(random));
        for (int i = 0; i < roots; i++) {
            buttress(level, blockSetter, random, pos, sides.get(i), config);
        }
        int arms = Math.min(8, branches.sample(random));
        List<int[]> directions = new ArrayList<>(List.of(new int[]{1, 0}, new int[]{-1, 0}, new int[]{0, 1}, new int[]{0, -1},
                new int[]{1, 1}, new int[]{1, -1}, new int[]{-1, 1}, new int[]{-1, -1}));
        shuffle(directions, random);
        BlockPos top = pos.above(freeTreeHeight - 1);
        for (int i = 0; i < arms; i++) {
            branch(level, blockSetter, random, top.below(1), directions.get(i), config);
        }
        if (podChance > 0.0F || orchidChance > 0.0F) {
            dress(level, blockSetter, random, pos, freeTreeHeight);
        }
        return List.of(new FoliagePlacer.FoliageAttachment(top.above(), 0, false));
    }

    /** One buttress root: wood that steps down outwards from the trunk, each piece reaching down to the ground. */
    private void buttress(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, BlockPos pos,
                          Direction side, TreeConfiguration config) {
        BlockState wood = ModBlocks.VERDANT_WOOD.get().defaultBlockState();
        for (int d = 1; d <= BUTTRESS_HEIGHTS.length; d++) {
            BlockPos base = pos.relative(side, d);
            int height = BUTTRESS_HEIGHTS[d - 1];
            for (int y = height - 1; y >= -ROOT_REACH_DOWN; y--) {
                BlockPos at = base.above(y);
                if (y < 0 && !TreeFeature.validTreePos(level, at)) {
                    break;
                }
                if (TreeFeature.validTreePos(level, at)) {
                    blockSetter.accept(at, wood);
                }
            }
        }
    }

    /** One branch: out and one up from below the top, then a vine or a hanging orchid under its end. */
    private void branch(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, BlockPos from,
                        int[] direction, TreeConfiguration config) {
        BlockPos at = from;
        Direction.Axis axis = direction[0] != 0 ? Direction.Axis.X : Direction.Axis.Z;
        for (int step = 1; step <= BRANCH_LENGTH; step++) {
            at = at.offset(direction[0], step == 2 ? 1 : 0, direction[1]);
            if (!TreeFeature.validTreePos(level, at)) {
                return;
            }
            blockSetter.accept(at, config.trunkProvider.getState(random, at).trySetValue(RotatedPillarBlock.AXIS, axis));
        }
        BlockPos below = at.below();
        if (!TreeFeature.validTreePos(level, below)) {
            return;
        }
        if (random.nextFloat() < 0.6F) {
            boolean flowering = random.nextBoolean();
            int length = 1 + random.nextInt(4);
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
        } else {
            blockSetter.accept(below, ModBlocks.VERDANT_HANGING_ORCHID.get().defaultBlockState());
        }
    }

    /** Fruit pods (facing the trunk) and orchids (facing away) on the trunk's sides above the buttresses. */
    private void dress(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random, BlockPos pos, int height) {
        for (int y = BUTTRESS_HEIGHTS[0] + 1; y < height - 2; y++) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                float roll = random.nextFloat();
                BlockPos at = pos.above(y).relative(side);
                if (!level.isStateAtPosition(at, BlockState::isAir)) {
                    continue;
                }
                BlockState state = null;
                if (roll < podChance) {
                    state = ModBlocks.VERDANT_FRUIT_POD.get().defaultBlockState().setValue(CocoaBlock.FACING, side.getOpposite())
                            .setValue(CocoaBlock.AGE, random.nextInt(3));
                } else if (roll < podChance + orchidChance) {
                    state = ModBlocks.VERDANT_ORCHID.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, side);
                }
                if (state != null && (!(level instanceof LevelReader reader) || state.canSurvive(reader, at))) {
                    blockSetter.accept(at, state);
                }
            }
        }
    }

    /** A Fisher–Yates shuffle with the tree's random (pure for a given random). */
    private static <T> void shuffle(List<T> list, RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T swap = list.get(i);
            list.set(i, list.get(j));
            list.set(j, swap);
        }
    }
}
