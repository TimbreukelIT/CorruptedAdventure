package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import nl.timtoys.corruptedadventure.registry.ModTrunkPlacerTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * {@code corruptedadventure:palm_trunk_placer}: a slender trunk whose first third is straight, after which it leans
 * {@code lean} blocks in one random direction, one sideways step at a time, spread evenly over the rest of the trunk.
 * The single foliage attachment is the block above the top log.
 */
public final class PalmTrunkPlacer extends TrunkPlacer {
    public static final MapCodec<PalmTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(instance -> trunkPlacerParts(instance)
            .and(IntProvider.codec(0, 3).fieldOf("lean").forGetter(placer -> placer.lean))
            .apply(instance, PalmTrunkPlacer::new));

    private final IntProvider lean;

    public PalmTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, IntProvider lean) {
        super(baseHeight, heightRandA, heightRandB);
        this.lean = lean;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return ModTrunkPlacerTypes.PALM_TRUNK_PLACER.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter,
                                                            RandomSource random, int freeTreeHeight, BlockPos pos, TreeConfiguration config) {
        setDirtAt(level, blockSetter, random, pos.below(), config);
        Shape shape = shape(random, freeTreeHeight);
        for (BlockPos log : shape.logs()) {
            placeLog(level, blockSetter, random, pos.offset(log), config);
        }
        return List.of(new FoliagePlacer.FoliageAttachment(pos.offset(shape.top()), 0, false));
    }

    /** The trunk as offsets from the tree origin; pure apart from the random draws. */
    Shape shape(RandomSource random, int height) {
        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int steps = lean.sample(random);
        int straight = Math.ceilDiv(height, 3);
        int leaning = height - straight;
        List<BlockPos> logs = new ArrayList<>(height);
        int shift = 0;
        for (int y = 0; y < height; y++) {
            // Step i (1-based) comes at straight + i · leaning / (steps + 1): evenly spread, never on the straight part.
            while (shift < steps && y >= straight + (shift + 1) * leaning / (steps + 1)) {
                shift++;
            }
            logs.add(BlockPos.ZERO.relative(direction, shift).above(y));
        }
        return new Shape(logs, BlockPos.ZERO.relative(direction, shift).above(height), direction, shift);
    }

    /**
     * @param logs log offsets from the origin, bottom to top
     * @param top  offset of the foliage attachment (above the top log)
     * @param lean number of sideways steps taken
     */
    record Shape(List<BlockPos> logs, BlockPos top, Direction direction, int lean) {
    }
}
