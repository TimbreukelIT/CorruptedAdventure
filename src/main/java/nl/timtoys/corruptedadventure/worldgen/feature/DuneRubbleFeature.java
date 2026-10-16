package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeaFossilShape;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeaFossils;

import java.util.List;

/**
 * {@code corruptedadventure:dune_rubble} (78, N4 = A): loose bones and boulders half buried in the bone dunes of Nether
 * ring 4, the shape of {@code rubble} ({@link RubbleShape}). The Nether has a ceiling, so the ground is found by scanning
 * down from the origin (the placement puts it just above the dune floor), not from a heightmap. It stands only on the
 * dunes' own blocks (bone sand, bleached netherrack): never on a walkway of fossil stone or on a fossil's bones, and
 * never on a fossil's floor or its ramp (kept free for the fossil temples of phase 3).
 * <p>
 * Reads and writes its own chunk only ({@link RubbleShape} shifts the footprint into it); the fossils are pure terrain.
 */
public final class DuneRubbleFeature extends Feature<RubbleConfiguration> {
    /** How far above and below the origin the ground is looked for. */
    private static final int SCAN_UP = 3;
    private static final int SCAN_DOWN = 8;
    /** Clear of a fossil by this many blocks round the origin (a boulder is at most 3 across, shifted by 1). */
    private static final int FOSSIL_CLEARANCE = 3;

    public DuneRubbleFeature(Codec<RubbleConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RubbleConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (nearFossil(NetherRing4Terrain.fossils(level), origin.getX(), origin.getZ())) {
            return false;
        }
        RandomSource random = context.random();
        RubbleConfiguration config = context.config();
        int size = config.size().sample(random);
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        List<BlockPos> blocks = RubbleShape.blocks(origin, size, random, new RubbleShape.Terrain() {
            @Override
            public int groundTop(int x, int z) {
                for (int y = origin.getY() + SCAN_UP; y >= origin.getY() - SCAN_DOWN; y--) {
                    if (solid(x, y, z) && free(x, y + 1, z)) {
                        return y;
                    }
                }
                return origin.getY() - SCAN_DOWN - 1;
            }

            @Override
            public boolean solid(int x, int y, int z) {
                BlockState state = level.getBlockState(probe.set(x, y, z));
                return state.is(ModBlocks.BONE_SAND.get()) || state.is(ModBlocks.BLEACHED_NETHERRACK.get());
            }

            @Override
            public boolean free(int x, int y, int z) {
                return level.getBlockState(probe.set(x, y, z)).isAir();
            }
        });
        if (blocks.isEmpty()) {
            return false;
        }
        BlockState material = config.stateProvider().getState(random, origin);
        for (BlockPos pos : blocks) {
            level.setBlock(pos, material, Block.UPDATE_CLIENTS);
        }
        return true;
    }

    /** Whether a fossil's bones, floor or ramp lie within {@link #FOSSIL_CLEARANCE} of the column. */
    static boolean nearFossil(SeaFossils fossils, int x, int z) {
        if (fossils == null) {
            return false;
        }
        for (int dx = -FOSSIL_CLEARANCE; dx <= FOSSIL_CLEARANCE; dx++) {
            for (int dz = -FOSSIL_CLEARANCE; dz <= FOSSIL_CLEARANCE; dz++) {
                int cx = x + dx;
                int cz = z + dz;
                // the column's own cell: near a cell edge the neighbouring cell's fossil may be the near one
                SeaFossilShape.Fossil fossil = fossils.fossilAt(cx, cz);
                if (fossil != null && fossil.touches(cx, cz) && (fossil.floorWeight(cx, cz) > 0.0 || fossil.boneMask(cx, cz) != 0L)) {
                    return true;
                }
            }
        }
        return false;
    }
}
