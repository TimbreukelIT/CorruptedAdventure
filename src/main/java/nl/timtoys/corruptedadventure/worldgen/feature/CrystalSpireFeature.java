package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.material.Fluids;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code corruptedadventure:crystal_spire} (ring 4 decor rollout group A, 2026-10-19): the eye-catcher of
 * {@code crystal_lagoon}, a group of hexagonal crystal spires rising 8–20 blocks out of the clear water, a tall one and
 * a few smaller ones leaning out around it; aquamarine with tidestone at the foot and in streaks, light in the shell
 * (sea lanterns) and crystals on the faces.
 * <p>
 * One group at most per landmark cell ({@link Landmarks}); its place and shape come from the seed and the pure ring 4
 * terrain alone (the floor from {@link IslandField}, never a block). Every chunk writes only its own part, at its own
 * fixed point of the feature order, so the result is the same in either order of generation; the group stands
 * {@link #BIOME_MARGIN} inside the lagoon, out of reach of any tree (the lagoon has none of its own), so nothing else
 * writes into it from a neighbouring chunk.
 */
public final class CrystalSpireFeature extends Feature<CrystalSpireConfiguration> {
    static final int MAX_HEIGHT = 24;
    static final float MAX_RADIUS = 4.0F;
    /**
     * How far a group reaches from its anchor: a companion stands up to 7.6 out and leans up to 0.32 per block over at
     * most ~40 blocks of height, its narrow top ~21 out.
     */
    static final int REACH = 24;
    /** Lagoon all round the group, this far beyond its reach. */
    static final int BIOME_MARGIN = 12;
    /** The deepest floor a group stands on (blocks under the sea level) and the shallowest (two blocks of water). */
    static final int MAX_DEPTH = 22;
    static final int MIN_DEPTH = 2;
    private static final long SALT = 0xC4_A57A_15L;
    private static final int FLAGS = Block.UPDATE_CLIENTS;
    private static final Direction[] FACES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP};

    public CrystalSpireFeature(Codec<CrystalSpireConfiguration> codec) {
        super(codec);
    }

    /** One spire: foot centre, the lean per block up, its height range, half-width at the foot and the hexagon's turn. */
    record Spire(double x, double z, double leanX, double leanZ, int bottom, int tip, double radius, double angle) {
        /** Whether the block (bx, y, bz) lies in the spire. */
        boolean contains(int bx, int y, int bz) {
            if (y < bottom || y > tip) {
                return false;
            }
            double t = (y - bottom) / (double) (tip - bottom);
            double r = t < 0.55 ? radius * (1.0 - 0.2 * t) : radius * 0.89 * (1.0 - t) / 0.45;
            if (r < 0.35) {
                return y == tip && Math.abs(bx + 0.5 - centreX(y)) < 0.75 && Math.abs(bz + 0.5 - centreZ(y)) < 0.75;
            }
            double dx = bx + 0.5 - centreX(y);
            double dz = bz + 0.5 - centreZ(y);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double u = Math.abs(dx * cos + dz * sin);
            double v = Math.abs(-dx * sin + dz * cos);
            return Math.max(u, 0.5 * u + 0.866 * v) <= r;
        }

        double centreX(int y) {
            return x + leanX * (y - bottom);
        }

        double centreZ(int y) {
            return z + leanZ * (y - bottom);
        }

        /** The height share of y, 0 at the foot, 1 at the tip. */
        double share(int y) {
            return (y - bottom) / (double) (tip - bottom);
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<CrystalSpireConfiguration> context) {
        WorldGenLevel level = context.level();
        IslandField field = Ring4Terrain.field(level);
        if (field == null) {
            return false;
        }
        CrystalSpireConfiguration config = context.config();
        ChunkPos chunk = new ChunkPos(context.origin());
        int sea = level.getSeaLevel();
        boolean placed = false;
        for (Landmarks.Candidate group : Landmarks.reaching(Landmarks.world(level), level.getSeed(), SALT, config.grid(), chunk, REACH,
                candidate -> accepts(level, field, config, sea, candidate))) {
            placed |= write(level, config, chunk, sea, spires(field, config, sea, group));
        }
        return placed;
    }

    /** Whether a group may stand at the candidate: deep enough water in the lagoon, nothing of the islands in the way. */
    private static boolean accepts(WorldGenLevel level, IslandField field, CrystalSpireConfiguration config, int sea, Landmarks.Candidate at) {
        double floor = field.column(at.x(), at.z()).surface();
        if (floor > sea - 1 - MIN_DEPTH || floor < sea - 1 - MAX_DEPTH) {
            return false;
        }
        int minX = at.x() - REACH;
        int minZ = at.z() - REACH;
        int maxX = at.x() + REACH;
        int maxZ = at.z() + REACH;
        int top = sea + MAX_HEIGHT + 8;
        return field.islandsWithin(minX, minZ, maxX, maxZ).stream().noneMatch(island -> island.bottom() < top)
                && field.liftsWithin(minX, minZ, maxX, maxZ).isEmpty() && field.waterfallsWithin(minX, minZ, maxX, maxZ).isEmpty()
                && field.worldTrees(minX - 64, minZ - 64, maxX + 64, maxZ + 64).isEmpty()
                && Landmarks.biomeAround(level, at.x(), at.z(), REACH + BIOME_MARGIN, config.biome());
    }

    /** The spires of a group, from its random and the pure floor under each foot. */
    static List<Spire> spires(IslandField field, CrystalSpireConfiguration config, int sea, Landmarks.Candidate group) {
        RandomSource random = group.random();
        List<Spire> spires = new ArrayList<>();
        int height = config.height().sample(random);
        double radius = config.radius().sample(random);
        double angle = random.nextDouble() * Math.PI;
        double lean = random.nextDouble() * 0.12;
        double heading = random.nextDouble() * Math.PI * 2.0;
        spires.add(spire(field, sea, group.x() + 0.5, group.z() + 0.5, Math.cos(heading) * lean, Math.sin(heading) * lean,
                sea + height, radius, angle));
        int companions = config.companions().sample(random);
        double start = random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < companions; i++) {
            double around = start + i * Math.PI * 2.0 / companions + (random.nextDouble() - 0.5) * 0.8;
            double distance = radius + 1.0 + random.nextDouble() * 3.0;
            double x = group.x() + 0.5 + Math.cos(around) * distance;
            double z = group.z() + 0.5 + Math.sin(around) * distance;
            double outward = 0.12 + random.nextDouble() * 0.2;
            int tip = sea + Math.max(3, (int) Math.round(height * (0.3 + random.nextDouble() * 0.4)));
            spires.add(spire(field, sea, x, z, Math.cos(around) * outward, Math.sin(around) * outward, tip,
                    radius * (0.45 + random.nextDouble() * 0.3), random.nextDouble() * Math.PI));
        }
        return spires;
    }

    /** A spire whose foot stands two blocks deep in the pure floor under it. */
    private static Spire spire(IslandField field, int sea, double x, double z, double leanX, double leanZ, int tip, double radius,
                               double angle) {
        int bottom = Math.min(sea - 2, Mth.floor(field.column(Mth.floor(x), Mth.floor(z)).surface()) - 2);
        return new Spire(x, z, leanX, leanZ, bottom, tip, radius, angle);
    }

    /** Writes the part of the group in {@code chunk}: the spires, then the crystals on their open faces. */
    private static boolean write(WorldGenLevel level, CrystalSpireConfiguration config, ChunkPos chunk, int sea, List<Spire> spires) {
        int bottom = spires.stream().mapToInt(Spire::bottom).min().orElseThrow();
        int tip = spires.stream().mapToInt(Spire::tip).max().orElseThrow();
        long seed = level.getSeed();
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        List<BlockPos> shell = new ArrayList<>();
        // Only the columns the group can reach (its leaned spires' box), not the whole chunk.
        int minX = chunk.getMinBlockX();
        int maxX = chunk.getMaxBlockX();
        int minZ = chunk.getMinBlockZ();
        int maxZ = chunk.getMaxBlockZ();
        int boxMinX = Integer.MAX_VALUE;
        int boxMaxX = Integer.MIN_VALUE;
        int boxMinZ = Integer.MAX_VALUE;
        int boxMaxZ = Integer.MIN_VALUE;
        for (Spire spire : spires) {
            for (int y : new int[]{spire.bottom(), spire.tip()}) {
                boxMinX = Math.min(boxMinX, Mth.floor(spire.centreX(y) - spire.radius()) - 1);
                boxMaxX = Math.max(boxMaxX, Mth.floor(spire.centreX(y) + spire.radius()) + 1);
                boxMinZ = Math.min(boxMinZ, Mth.floor(spire.centreZ(y) - spire.radius()) - 1);
                boxMaxZ = Math.max(boxMaxZ, Mth.floor(spire.centreZ(y) + spire.radius()) + 1);
            }
        }
        minX = Math.max(minX, boxMinX);
        maxX = Math.min(maxX, boxMaxX);
        minZ = Math.max(minZ, boxMinZ);
        maxZ = Math.min(maxZ, boxMaxZ);
        for (int y = bottom; y <= tip; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    Spire spire = containing(spires, x, y, z);
                    if (spire == null || level.getBlockState(pos.set(x, y, z)).is(ModBlocks.UPDRAFT.get())) {
                        continue;
                    }
                    boolean outer = false;
                    for (Direction face : Direction.values()) {
                        outer |= containing(spires, x + face.getStepX(), y + face.getStepY(), z + face.getStepZ()) == null;
                    }
                    RandomSource random = random(seed, x, y, z, 0);
                    level.setBlock(pos, block(config, spire, sea, x, y, z, outer, random), FLAGS);
                    placed = true;
                    if (outer && y >= spire.bottom() + 2) {
                        shell.add(pos.immutable());
                    }
                }
            }
        }
        for (BlockPos at : shell) {
            for (Direction face : FACES) {
                BlockPos out = at.relative(face);
                if (!Landmarks.inChunk(out, chunk) || containing(spires, out.getX(), out.getY(), out.getZ()) != null) {
                    continue;
                }
                RandomSource random = random(seed, out.getX(), out.getY(), out.getZ(), face.ordinal() + 1);
                if (random.nextFloat() >= config.budChance()) {
                    continue;
                }
                BlockState bud = config.buds().getState(random, out);
                BlockState existing = level.getBlockState(out);
                boolean water = existing.getFluidState().isSourceOfType(Fluids.WATER) && existing.is(Blocks.WATER);
                if (!existing.isAir() && !water) {
                    continue;
                }
                if (bud.hasProperty(BlockStateProperties.FACING)) {
                    bud = bud.setValue(BlockStateProperties.FACING, face);
                }
                if (bud.hasProperty(BlockStateProperties.WATERLOGGED)) {
                    bud = bud.setValue(BlockStateProperties.WATERLOGGED, water);
                }
                if (bud.canSurvive(level, out)) {
                    level.setBlock(out, bud, FLAGS);
                }
            }
        }
        return placed;
    }

    /** The block at (x, y, z) of {@code spire}: the base low down and in streaks, the shell with its lights, the core. */
    private static BlockState block(CrystalSpireConfiguration config, Spire spire, int sea, int x, int y, int z, boolean outer,
                                    RandomSource random) {
        BlockPos pos = new BlockPos(x, y, z);
        float roll = random.nextFloat();
        if (y <= sea + 1) {
            return config.base().getState(random, pos);
        }
        if (!outer) {
            return config.core();
        }
        // Tidestone streaks thin out up the lower half; above it only crystal and light.
        double streaks = Math.max(0.0, 0.55 * (1.0 - spire.share(y) / 0.5));
        if (roll < streaks) {
            return config.base().getState(random, pos);
        }
        if (random.nextFloat() < config.glowChance()) {
            return config.glow();
        }
        return config.shell().getState(random, pos);
    }

    private static Spire containing(List<Spire> spires, int x, int y, int z) {
        for (Spire spire : spires) {
            if (spire.contains(x, y, z)) {
                return spire;
            }
        }
        return null;
    }

    /** A block's own random (position, seed, purpose): the same whichever chunk asks first. */
    private static RandomSource random(long seed, int x, int y, int z, int purpose) {
        return new XoroshiroRandomSource(RandomSupport.mixStafford13(seed ^ SALT ^ purpose),
                RandomSupport.mixStafford13(BlockPos.asLong(x, y, z)));
    }
}
