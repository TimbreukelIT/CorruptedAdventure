package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Placement modifier {@code corruptedadventure:jittered_grid}, in place of {@code count} + {@code in_square}: one
 * candidate per cell of a grid fixed in world coordinates, kept with {@code chance} and jittered inside the cell minus
 * {@code margin} on every side.
 * <p>
 * Cells divide the chunk, so they never straddle a chunk border: two candidates of different cells are always at
 * least 2 · {@code margin} + 1 apart along one axis, also across chunks, without reading anything of the neighbours.
 * That spaces out trees (no "knots") and keeps a feature of reach ≤ {@code margin} inside its own chunk. Draws come
 * from the placement random (seeded per chunk and feature), so the result does not depend on generation order.
 */
public final class JitteredGridPlacement extends PlacementModifier {
    public static final MapCodec<JitteredGridPlacement> CODEC = RecordCodecBuilder.<JitteredGridPlacement>mapCodec(instance -> instance.group(
            Codec.intRange(1, 16).fieldOf("cell_size").forGetter(placement -> placement.cellSize),
            Codec.intRange(0, 7).optionalFieldOf("margin", 0).forGetter(placement -> placement.margin),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance", 1.0F).forGetter(placement -> placement.chance)
    ).apply(instance, JitteredGridPlacement::new)).validate(JitteredGridPlacement::validate);

    private final int cellSize;
    private final int margin;
    private final float chance;

    public JitteredGridPlacement(int cellSize, int margin, float chance) {
        this.cellSize = cellSize;
        this.margin = margin;
        this.chance = chance;
    }

    public static JitteredGridPlacement of(int cellSize, int margin, float chance) {
        return new JitteredGridPlacement(cellSize, margin, chance);
    }

    private static DataResult<JitteredGridPlacement> validate(JitteredGridPlacement placement) {
        if (16 % placement.cellSize != 0) {
            return DataResult.error(() -> "jittered_grid: cell_size must divide 16, got " + placement.cellSize);
        }
        if (2 * placement.margin >= placement.cellSize) {
            return DataResult.error(() -> "jittered_grid: margin " + placement.margin + " leaves no room in a cell of " + placement.cellSize);
        }
        return DataResult.success(placement);
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        return candidates(pos.getX() & ~15, pos.getY(), pos.getZ() & ~15, random).stream();
    }

    /** The kept candidates of the chunk with minimum corner {@code minX, minZ}, row by row. */
    List<BlockPos> candidates(int minX, int y, int minZ, RandomSource random) {
        int cells = 16 / cellSize;
        int span = cellSize - 2 * margin;
        List<BlockPos> positions = new ArrayList<>(cells * cells);
        for (int cz = 0; cz < cells; cz++) {
            for (int cx = 0; cx < cells; cx++) {
                // Always draw all three, so a cell's position does not depend on whether earlier cells were kept.
                boolean kept = random.nextFloat() < chance;
                int x = minX + cx * cellSize + margin + random.nextInt(span);
                int z = minZ + cz * cellSize + margin + random.nextInt(span);
                if (kept) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        return positions;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.JITTERED_GRID.get();
    }
}
