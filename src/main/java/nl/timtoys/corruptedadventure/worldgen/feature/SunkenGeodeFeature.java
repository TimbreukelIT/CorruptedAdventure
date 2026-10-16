package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.material.Fluids;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code corruptedadventure:sunken_geode} (package 77, Q14 = A): an aquamarine geode half open in the floor under deep
 * water. A sphere around the floor block at the origin (the first water block above the floor): a hollow, then the
 * crystal layer (with budding blocks), a middle and an outer layer; only the part at or below each column's floor
 * ({@code OCEAN_FLOOR_WG} − 1) is set, so the upper half is the open water and the bowl lies open to it. The hollow
 * fills with water and the buds on its walls face into it, waterlogged.
 * <p>
 * Never water against air: the geode is only placed when it lies in the origin's chunk, no block it would set is air
 * (a cave under the floor) and water stands over every column of the hollow. Reads the frozen heightmap and blocks of
 * its own chunk; writes only there.
 */
public final class SunkenGeodeFeature extends Feature<SunkenGeodeConfiguration> {
    public static final int MAX_HOLLOW = 3;
    /** The layers around the hollow: crystal, middle, outer. */
    static final int LAYERS = 3;

    public SunkenGeodeFeature(Codec<SunkenGeodeConfiguration> codec) {
        super(codec);
    }

    /** What a block of the geode is. */
    enum Part {
        HOLLOW, CRYSTAL, MIDDLE, OUTER
    }

    /** The part at distance² {@code d2} from the centre for a hollow of {@code hollow}, or null outside. */
    static Part part(int d2, int hollow) {
        double d = Math.sqrt(d2);
        if (d < hollow + 0.5) {
            return Part.HOLLOW;
        }
        if (d < hollow + 1.5) {
            return Part.CRYSTAL;
        }
        if (d < hollow + 2.5) {
            return Part.MIDDLE;
        }
        return d < hollow + LAYERS + 0.5 ? Part.OUTER : null;
    }

    @Override
    public boolean place(FeaturePlaceContext<SunkenGeodeConfiguration> context) {
        WorldGenLevel level = context.level();
        SunkenGeodeConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos centre = context.origin().below();
        int hollow = config.hollow().sample(random);
        int reach = hollow + LAYERS;
        int chunkX = centre.getX() >> 4;
        int chunkZ = centre.getZ() >> 4;
        Map<BlockPos, Part> parts = new LinkedHashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                if (x >> 4 != chunkX || z >> 4 != chunkZ) {
                    return false;
                }
                int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                // Every column of the footprint under water: the open hollow then only ever meets water.
                if (!level.getFluidState(pos.set(x, floor + 1, z)).is(Fluids.WATER)) {
                    return false;
                }
                for (int dy = -reach; dy <= reach; dy++) {
                    int y = centre.getY() + dy;
                    Part part = part(dx * dx + dy * dy + dz * dz, hollow);
                    if (part == null || y > floor) {
                        continue;
                    }
                    if (level.getBlockState(pos.set(x, y, z)).isAir()) {
                        return false;
                    }
                    parts.put(new BlockPos(x, y, z), part);
                }
            }
        }
        BlockState water = Blocks.WATER.defaultBlockState();
        for (Map.Entry<BlockPos, Part> entry : parts.entrySet()) {
            BlockState state = switch (entry.getValue()) {
                case HOLLOW -> water;
                case CRYSTAL -> random.nextFloat() < config.buddingChance() ? config.budding() : config.crystal();
                case MIDDLE -> config.middle();
                case OUTER -> config.outer();
            };
            level.setBlock(entry.getKey(), state, Block.UPDATE_CLIENTS);
        }
        for (Map.Entry<BlockPos, Part> entry : parts.entrySet()) {
            if (entry.getValue() != Part.HOLLOW) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                if (parts.get(entry.getKey().relative(direction)) == Part.CRYSTAL && random.nextFloat() < config.budChance()) {
                    BlockState bud = config.buds().getState(random, entry.getKey());
                    level.setBlock(entry.getKey(), bud.setValue(AmethystClusterBlock.FACING, direction.getOpposite())
                            .setValue(BlockStateProperties.WATERLOGGED, true), Block.UPDATE_CLIENTS);
                    break;
                }
            }
        }
        return true;
    }
}
