package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import nl.timtoys.corruptedadventure.registry.ModFoliagePlacerTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code corruptedadventure:verdant_foliage_placer} (75b, F2 = A): the crowns of the verdant tree. The {@code umbrella}
 * of the emergent tree is a wide, flat disc (radius 4–6) with a smaller dome on top and a rim that hangs down in
 * places; the {@code fronds} of the tree fern are eight leaf arms around a tuft, their tips bending down. The shapes
 * are pure functions of the radius and the random ({@link #shape}); the tree feature computes the leaves' distance.
 */
public final class VerdantFoliagePlacer extends FoliagePlacer {
    public static final MapCodec<VerdantFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(instance -> foliagePlacerParts(instance)
            .and(Shape.CODEC.fieldOf("shape").forGetter(placer -> placer.shape))
            .apply(instance, VerdantFoliagePlacer::new));

    /** The chance that a rim block of the umbrella hangs one lower. */
    static final float DROOP = 0.4F;

    private final Shape shape;

    public VerdantFoliagePlacer(IntProvider radius, IntProvider offset, Shape shape) {
        super(radius, offset);
        this.shape = shape;
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return ModFoliagePlacerTypes.VERDANT_FOLIAGE_PLACER.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter blockSetter, RandomSource random, TreeConfiguration config,
                                 int maxFreeTreeHeight, FoliageAttachment attachment, int foliageHeight, int foliageRadius, int offset) {
        BlockPos centre = attachment.pos().above(offset);
        for (BlockPos leaf : shape(shape, foliageRadius + attachment.radiusOffset(), random)) {
            tryPlaceLeaf(level, blockSetter, random, config, centre.offset(leaf));
        }
    }

    /** The leaves of a crown of {@code radius} as offsets from the attachment. */
    static List<BlockPos> shape(Shape shape, int radius, RandomSource random) {
        List<BlockPos> leaves = new ArrayList<>();
        if (shape == Shape.UMBRELLA) {
            double rim = radius + 0.5;
            double dome = Math.max(1, radius - 2) + 0.5;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    // Always the draw, so a block's droop never depends on the ones before it.
                    boolean droop = random.nextFloat() < DROOP;
                    if (d <= rim) {
                        leaves.add(new BlockPos(dx, -1, dz));
                        if (d > rim - 1.0 && droop) {
                            leaves.add(new BlockPos(dx, -2, dz));
                        }
                    }
                    if (d <= dome) {
                        leaves.add(new BlockPos(dx, 0, dz));
                    }
                }
            }
            return leaves;
        }
        // Fronds: a tuft on top and around the stem, eight arms; the diagonal ones one shorter; each tip one lower.
        leaves.add(BlockPos.ZERO);
        leaves.add(BlockPos.ZERO.above());
        int[][] arms = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] arm : arms) {
            int length = arm[0] != 0 && arm[1] != 0 ? Math.max(1, radius - 1) : radius;
            for (int step = 1; step <= length; step++) {
                leaves.add(new BlockPos(arm[0] * step, 0, arm[1] * step));
                if (arm[0] != 0 && arm[1] != 0) {
                    // A diagonal arm steps sideways too, so its leaves touch face to face (the leaves' distance).
                    leaves.add(new BlockPos(arm[0] * step, 0, arm[1] * (step - 1)));
                }
            }
            // The tip bends down under the arm's last leaf (connected, so it counts its distance through the arm).
            leaves.add(new BlockPos(arm[0] * length, -1, arm[1] * length));
        }
        return leaves;
    }

    @Override
    public int foliageHeight(RandomSource random, int height, TreeConfiguration config) {
        return shape == Shape.UMBRELLA ? 3 : 2;
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int localX, int localY, int localZ, int range, boolean large) {
        return false;
    }

    /** The two crowns. */
    public enum Shape implements StringRepresentable {
        UMBRELLA,
        FRONDS;

        public static final Codec<Shape> CODEC = StringRepresentable.fromEnum(Shape::values);

        private final String serializedName = name().toLowerCase(Locale.ROOT);

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
