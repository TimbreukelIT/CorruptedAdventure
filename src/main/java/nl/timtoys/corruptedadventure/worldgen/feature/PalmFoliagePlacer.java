package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import nl.timtoys.corruptedadventure.registry.ModFoliagePlacerTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code corruptedadventure:palm_foliage_placer}: a palm crown on the attachment (the block above the top log):
 * <ul>
 *     <li>one leaf on the attachment itself;</li>
 *     <li>four straight fronds at that level, {@code radius} blocks long (sampled per frond), each ending in a leaf
 *     hanging one block below its last one;</li>
 *     <li>{@code diagonal_fronds} diagonal fronds one level lower (beside the top log): a staircase of face-connected
 *     leaves two diagonal steps out, also ending in a hanging leaf.</li>
 * </ul>
 * Every leaf reaches the trunk through leaves in at most 5 face steps, inside vanilla's decay distance of 6. A frond
 * stops at the first leaf that cannot be placed, so no leaf is ever cut off from the trunk; only a log (another palm's
 * trunk) is skipped, as the leaves beyond it hang on that log. That keeps neighbouring palms independent of which grew
 * first: a log placed later replaces the leaf anyway, so either way the frond runs on past the trunk.
 */
public final class PalmFoliagePlacer extends FoliagePlacer {
    public static final MapCodec<PalmFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(instance -> foliagePlacerParts(instance)
            .and(IntProvider.codec(0, 4).fieldOf("diagonal_fronds").forGetter(placer -> placer.diagonalFronds))
            .apply(instance, PalmFoliagePlacer::new));

    private static final int[][] STRAIGHT = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] DIAGONAL = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private final IntProvider diagonalFronds;

    /** @param radius length of each straight frond, without its hanging leaf */
    public PalmFoliagePlacer(IntProvider radius, IntProvider offset, IntProvider diagonalFronds) {
        super(radius, offset);
        this.diagonalFronds = diagonalFronds;
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return ModFoliagePlacerTypes.PALM_FOLIAGE_PLACER.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter blockSetter, RandomSource random, TreeConfiguration config,
                                 int maxFreeTreeHeight, FoliageAttachment attachment, int foliageHeight, int foliageRadius, int offset) {
        BlockPos crown = attachment.pos().above(offset);
        for (List<BlockPos> frond : fronds(random)) {
            for (BlockPos leaf : frond) {
                BlockPos pos = crown.offset(leaf);
                if (!tryPlaceLeaf(level, blockSetter, random, config, pos) && !level.isStateAtPosition(pos, state -> state.is(BlockTags.LOGS))) {
                    break;
                }
            }
        }
    }

    /**
     * The crown as offsets from the attachment, one list per frond in placement order: every leaf touches an earlier
     * leaf of its frond, the crown leaf or the top log. The first list is the crown leaf itself.
     */
    List<List<BlockPos>> fronds(RandomSource random) {
        List<List<BlockPos>> fronds = new ArrayList<>();
        fronds.add(List.of(BlockPos.ZERO));
        for (int[] d : STRAIGHT) {
            int length = radius.sample(random);
            List<BlockPos> frond = new ArrayList<>();
            for (int i = 1; i <= length; i++) {
                frond.add(new BlockPos(d[0] * i, 0, d[1] * i));
            }
            frond.add(new BlockPos(d[0] * length, -1, d[1] * length));
            fronds.add(frond);
        }
        List<int[]> diagonals = new ArrayList<>(List.of(DIAGONAL));
        int count = Math.min(diagonalFronds.sample(random), diagonals.size());
        for (int n = 0; n < count; n++) {
            int[] d = diagonals.remove(random.nextInt(diagonals.size()));
            boolean xFirst = random.nextBoolean();
            List<BlockPos> frond = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                // A staircase, first along one axis and then the other, so each leaf has a face neighbour before it.
                frond.add(xFirst ? new BlockPos(d[0] * i, -1, d[1] * (i - 1)) : new BlockPos(d[0] * (i - 1), -1, d[1] * i));
                frond.add(new BlockPos(d[0] * i, -1, d[1] * i));
            }
            frond.add(new BlockPos(d[0] * 2, -2, d[1] * 2));
            fronds.add(frond);
        }
        return fronds;
    }

    @Override
    public int foliageHeight(RandomSource random, int height, TreeConfiguration config) {
        return 0;
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int localX, int localY, int localZ, int range, boolean large) {
        return false;
    }
}
