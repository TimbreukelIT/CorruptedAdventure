package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.ZoneFracture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code corruptedadventure:split_rock} (ring 4 decor D, 2026-10-19; since 86c on the fracture zone, V8-A): the landmark
 * of {@code sundered_reach}, a tall rock split in two that marks the way across to ring 5. One stands on every landmark
 * of the zone 4→5 ({@link ZoneFracture#landmarksWithin}: a raised flat floe every ~650 blocks along the zone), in its
 * middle, on its flat top ({@link ZoneFracture.Landmark#topY}). The split runs along the ray, towards ring 5 (as the
 * land bridges it stood on before did), {@link #GAP} · 2 blocks wide at the foot and widening upwards as the two halves
 * lean apart. The halves taper to their tops and differ in height; their faces are ragged. The foot is sized to stay on
 * the floe ({@link #plan}).
 * <p>
 * Order independence (the ground-object model of {@link GroundObjects}, across chunk borders): a rock is a pure function
 * of the seed and its landmark ({@link #plan}; its base is the landmark's top, not a heightmap), and every chunk it
 * reaches writes only its own columns of it, each standing on that column's frozen {@code WORLD_SURFACE_WG}; the block
 * of each position comes from a random of its own. Placed once per chunk without a biome filter; nothing else stands on
 * the floes, and trees never take its stone ({@link TreeOverlap#object}).
 */
public final class SplitRockFeature extends Feature<SplitRockConfiguration> {
    static final int MAX_HEIGHT = 40;
    static final float MAX_LENGTH = 6.0F;
    static final float MAX_LEAN = 4.0F;
    /** The split's half width at the foot (blocks from the split's middle line to a half's inner face). */
    static final double GAP = 1.5;
    /**
     * A half's outer face lies at the foot {@link #FLOE_MARGIN} inside the floe's radius, at least {@link #MIN_HALF_WIDTH}
     * and at most {@link #MAX_HALF_WIDTH} (the halves' thickness of decor D).
     */
    static final double MIN_HALF_WIDTH = 6.0;
    static final double MAX_HALF_WIDTH = 8.0;
    static final int FLOE_MARGIN = 3;
    /** The lowest blocks above the ground are the foot's (moss, rubble). */
    static final int FOOT = 2;
    /** The rock reaches this far into the ground (no gap under a face on a slope). */
    static final int SUNK = 2;
    /**
     * Where a column has no floe under it (its ground lies further below the floe's top than {@link #DROP}), the rock
     * hangs down this far below the top, less towards its ends along the split (a keel, not a flat underside). The foot
     * is sized to the floe ({@link #plan}), so this only guards the ground of a column that is not the floe's.
     */
    static final int HANG = 9;
    static final int DROP = 3;
    /** The faces' roughness (blocks), one value per column and band of {@link #RAG_BAND} blocks. */
    static final double RAG = 0.6;
    static final int RAG_BAND = 3;
    private static final long RAG_SALT = 0x2A6_BA4DL;
    /** How much a half's outer face draws in from the foot to the top (share of its half width). */
    static final double TAPER = 0.3;
    /**
     * No block lies further than this from the rock's anchor along x or z, in any direction of its split (on a diagonal
     * a corner: (half width + lean + rag + length + rag) / √2 ≈ 13.8).
     */
    static final int REACH = 15;
    private static final long SALT = 0x5_9117_70C4L;

    /**
     * A rock: anchor (the landmark's centre), direction along the split (unit, outwards along the ray), the floe's top
     * block under it, outer half width, the halves' heights.
     */
    record Rock(double x, double z, double ux, double uz, int base, double halfWidth, int lowHeight, int highHeight,
                int highSide, double length, double lean) {
        /** The height of the half on {@code side} (−1 or +1 across the split). */
        int height(int side) {
            return side == highSide ? highHeight : lowHeight;
        }
    }

    public SplitRockFeature(Codec<SplitRockConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SplitRockConfiguration> context) {
        WorldGenLevel level = context.level();
        ZoneFracture fracture = ZoneFractureTerrain.fracture(level);
        if (fracture == null) {
            return false;
        }
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        boolean placed = false;
        for (Rock rock : rocksNear(fracture, level.getSeed(), context.config(), minX, minZ)) {
            placed |= placeInChunk(level, context.config(), rock, minX, minZ);
        }
        return placed;
    }

    /** The rocks that reach into the chunk with minimum corner {@code minX, minZ}. */
    static List<Rock> rocksNear(ZoneFracture fracture, long seed, SplitRockConfiguration config, int minX, int minZ) {
        List<Rock> rocks = new ArrayList<>();
        // landmarksWithin pads the box by each floe's radius; a rock reaches the columns within REACH of its anchor's
        // column (what placeInChunk writes), so that decides.
        for (ZoneFracture.Landmark landmark : fracture.landmarksWithin(minX - REACH, minZ - REACH, minX + 15 + REACH, minZ + 15 + REACH)) {
            if (landmark.x() + REACH >= minX && landmark.x() - REACH <= minX + 15
                    && landmark.z() + REACH >= minZ && landmark.z() - REACH <= minZ + 15) {
                rocks.add(plan(seed, config, landmark));
            }
        }
        return rocks;
    }

    /**
     * The rock of a landmark: pure in the seed and the landmark. Its foot, ragged faces included, stays on the floe: the
     * half width {@link #FLOE_MARGIN} inside the radius (clamped to 6–8), the length cut where the foot's corners would
     * reach past the radius.
     */
    static Rock plan(long seed, SplitRockConfiguration config, ZoneFracture.Landmark landmark) {
        double x = landmark.x() + 0.5;
        double z = landmark.z() + 0.5;
        double distance = Math.hypot(x, z);
        double ux = x / distance;
        double uz = z / distance;
        RandomSource random = GroundObjects.random(seed, new BlockPos(landmark.x(), 0, landmark.z()), SALT);
        int high = config.height().sample(random);
        int low = Math.max(8, Math.round(high * config.shorter().sample(random)));
        int highSide = random.nextBoolean() ? 1 : -1;
        double length = config.length().sample(random);
        double lean = config.lean().sample(random);
        int radius = landmark.radius();
        double halfWidth = Mth.clamp(radius - FLOE_MARGIN, MIN_HALF_WIDTH, MAX_HALF_WIDTH);
        // The foot's outline (inside, t = 0) reaches halfWidth + RAG across and length + 0.3 + RAG along.
        double across = halfWidth + RAG;
        double fit = Math.sqrt(Math.max(0.0, (double) radius * radius - across * across)) - 0.3 - RAG;
        return new Rock(x, z, ux, uz, landmark.topY() - 1, halfWidth, low, high, highSide, Math.min(length, fit), lean);
    }

    /** Writes the rock's blocks in the chunk's own columns; whether there were any. */
    private static boolean placeInChunk(WorldGenLevel level, SplitRockConfiguration config, Rock rock, int minX, int minZ) {
        Map<BlockPos, BlockState> stones = new LinkedHashMap<>();
        int x0 = Math.max(minX, Mth.floor(rock.x) - REACH);
        int x1 = Math.min(minX + 15, Mth.floor(rock.x) + REACH);
        int z0 = Math.max(minZ, Mth.floor(rock.z) - REACH);
        int z1 = Math.min(minZ + 15, Mth.floor(rock.z) + REACH);
        long seed = level.getSeed();
        int base = rock.base;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                double dx = x + 0.5 - rock.x;
                double dz = z + 0.5 - rock.z;
                double along = dx * rock.ux + dz * rock.uz;
                double across = -dx * rock.uz + dz * rock.ux;
                int side = across >= 0.0 ? 1 : -1;
                int height = rock.height(side);
                int ground = GroundObjects.surface(level, x, z) - 1;
                if (ground < base - DROP) {
                    ground = base - HANG + (int) Math.round(HANG * 0.6 * Math.min(1.0, Math.abs(along) / rock.length));
                }
                for (int y = ground - SUNK + 1; y <= base + height; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    // Every block its own random: the same block whichever chunk writes it, whatever was written before.
                    RandomSource random = GroundObjects.random(seed, pos, SALT);
                    double face = inside(rock, along, Math.abs(across), height, y - base, rag(seed, x, y, z));
                    if (face < 0.0) {
                        continue;
                    }
                    BlockState state;
                    if (y - ground < FOOT && y > ground) {
                        state = config.foot().getState(random, pos);
                    } else if (face < 1.0) {
                        state = config.crack().getState(random, pos);
                    } else {
                        state = config.body().getState(random, pos);
                    }
                    stones.put(pos, state);
                }
            }
        }
        if (stones.isEmpty()) {
            return false;
        }
        GroundObjects.placeObject(level, stones);
        return true;
    }

    /**
     * Whether a block lies in its half: −1 outside, else how far inside the split's face it lies (blocks; under 1 it is
     * a face of the split). {@code dy} counts from the floe's top ({@link Rock#base}); below it the rock keeps its
     * foot's outline.
     */
    static double inside(Rock rock, double along, double across, int height, int dy, double rag) {
        double t = Mth.clamp(dy / (double) height, 0.0, 1.0);
        double lean = rock.lean * t * t;
        double inner = GAP + lean + 0.5 * rag;
        double outer = rock.halfWidth * (1.0 - TAPER * t) + lean + rag;
        // The halves taper to ragged points: the outline along the split shrinks faster near the top.
        double length = rock.length * (1.0 - 0.6 * t * t) + 0.3 + rag;
        if (dy > height || Math.abs(along) > length || across < inner || across > Math.max(inner + 0.6, outer)) {
            return -1.0;
        }
        return across - inner;
    }

    /**
     * The faces' roughness at a block (−{@link #RAG}..{@link #RAG}): one value per column and band of
     * {@link #RAG_BAND} blocks, so the faces get ledges and bulges rather than single loose blocks and holes.
     */
    static double rag(long seed, int x, int y, int z) {
        RandomSource band = GroundObjects.random(seed, new BlockPos(x, Math.floorDiv(y, RAG_BAND), z), SALT ^ RAG_SALT);
        return RAG * (2.0 * band.nextDouble() - 1.0);
    }
}
