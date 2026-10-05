package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.SeededHash;

/**
 * {@code corruptedadventure:dew_geode} (change 35): lines the cavities that the density function {@code dew_geodes} dug.
 * Placed once per chunk (origin = the chunk's corner); it handles the geode of the cell whose centre lies in this chunk
 * ({@link DewGeodeShape}, the same cell hash as the density function). Only the lining is feature work: the wall of the
 * cavity gets dew sources, the free sides of those get pre-grown dew crystal buds and the odd patch of glow mold. The
 * cavity itself exists before any feature, so no feature changes terrain except turning it into dew source.
 * <p>
 * Determinism (see {@link GrowthPriority}): decides only on terrain; a block of the wall that is terrain becomes dew
 * source whatever it was (ore too, but not lava or bedrock), buds and mold only go into air or lower growths.
 * Reads and writes stay within {@link DewGeodeShape.Geode#reach()} of a centre inside this chunk, so within the 3 × 3 chunks.
 */
public final class DewGeodeFeature extends Feature<DewGeodeConfiguration> {
    /** The cavity itself must be free at its centre and on the six axes, this fraction of its radius out. */
    private static final double INTERIOR = 0.5;

    public DewGeodeFeature(Codec<DewGeodeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<DewGeodeConfiguration> context) {
        WorldGenLevel level = context.level();
        DewGeodeConfiguration config = context.config();
        ChunkPos chunk = new ChunkPos(context.origin());
        var randomState = level.getLevel().getChunkSource().randomState();
        NormalNoise cells = randomState.getOrCreateNoise(config.cellNoise());
        NormalNoise region = randomState.getOrCreateNoise(config.regionNoise());
        FungalWorld world = FungalWorld.of(level);
        boolean placed = false;
        // Every attempt of the cell whose centre lies in this chunk and whose cavity exists (only the density function's winner has one).
        for (DewGeodeShape.Geode geode : DewGeodeShape.candidatesOfCell(config.shape(), chunk.getMinBlockX(), chunk.getMinBlockZ(),
                (x, z, salt) -> SeededHash.hash(cells, x, z, salt), (x, z) -> region.getValue(x, 0, z))) {
            if (geode.x() >> 4 == chunk.x && geode.z() >> 4 == chunk.z && exists(world, geode, config.shape())) {
                line(world, geode, config, context.random());
                placed = true;
                break;
            }
        }
        return placed;
    }

    /** The cavity is free and enclosed: its centre and axis points inside are not terrain, the 26 wall points are. */
    static boolean exists(FungalWorld world, DewGeodeShape.Geode geode, DewGeodeShape.Settings settings) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        if (FungalTerrainScan.isTerrain(world.get(pos.set(geode.x(), geode.y(), geode.z())))) {
            return false;
        }
        int inner = (int) (geode.radius() * INTERIOR);
        for (Direction direction : Direction.values()) {
            pos.set(geode.x() + direction.getStepX() * inner, geode.y() + direction.getStepY() * inner, geode.z() + direction.getStepZ() * inner);
            if (FungalTerrainScan.isTerrain(world.get(pos))) {
                return false;
            }
        }
        for (int[] point : geode.wallTestPoints(settings.shell(), 1)) {
            if (!FungalTerrainScan.isTerrain(world.get(pos.set(point[0], point[1], point[2])))) {
                return false;
            }
        }
        return true;
    }

    /** Dew sources in the wall ({@code source_chance}), buds and mold on their free sides; blocks visited in a fixed order. */
    static void line(FungalWorld world, DewGeodeShape.Geode geode, DewGeodeConfiguration config, RandomSource random) {
        int reach = geode.reach();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState source = ModBlocks.DEW_SOURCE.get().defaultBlockState();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double distance = geode.distance(geode.x() + dx, geode.y() + dy, geode.z() + dz);
                    if (distance <= geode.radius() || distance > geode.radius() + DewGeodeShape.LINING) {
                        continue;
                    }
                    pos.set(geode.x() + dx, geode.y() + dy, geode.z() + dz);
                    // One draw per lining block, whether or not it qualifies.
                    boolean chosen = random.nextDouble() < config.sourceChance();
                    if (chosen && replaceable(world.get(pos))) {
                        world.set(pos, source);
                        growOn(world, pos.immutable(), geode, config, random);
                    }
                }
            }
        }
    }

    /** Terrain that a dew source may replace: not lava, not bedrock, and not air (so the hole a carver made stays). */
    private static boolean replaceable(BlockState state) {
        return FungalTerrainScan.isTerrain(state) && !FungalTerrainScan.isLava(state) && !state.is(Blocks.BEDROCK);
    }

    private static void growOn(FungalWorld world, BlockPos source, DewGeodeShape.Geode geode, DewGeodeConfiguration config, RandomSource random) {
        for (Direction direction : Direction.values()) {
            BlockPos side = source.relative(direction);
            // Three draws per side, used or not, so the stream does not depend on what stands there.
            double budRoll = random.nextDouble();
            double moldRoll = random.nextDouble();
            double kindRoll = random.nextDouble();
            if (!geode.inCavity(side.getX(), side.getY(), side.getZ())) {
                continue;
            }
            if (budRoll < config.budChance()) {
                GrowthPriority.write(world, side, bud(kindRoll).setValue(AmethystClusterBlock.FACING, direction));
            } else if (moldRoll < config.moldChance()) {
                GrowthPriority.write(world, side, ModBlocks.GLOW_MOLD.get().defaultBlockState()
                        .setValue(MultifaceBlock.getFaceProperty(direction.getOpposite()), true));
            }
        }
    }

    /** Mostly small and medium buds, large ones now and then; the full cluster only grows (random ticks). */
    static BlockState bud(double roll) {
        Block block = roll < 0.55 ? ModBlocks.SMALL_DEW_CRYSTAL_BUD.get() : roll < 0.88 ? ModBlocks.MEDIUM_DEW_CRYSTAL_BUD.get()
                : ModBlocks.LARGE_DEW_CRYSTAL_BUD.get();
        return block.defaultBlockState();
    }
}
