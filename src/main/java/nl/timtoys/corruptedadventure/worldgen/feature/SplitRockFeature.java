package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code corruptedadventure:split_rock} (ring 4 decor D, 2026-10-19): the landmark of {@code sundered_reach}, a tall
 * rock split in two that marks the way across to ring 5. One stands on every land bridge of the zone 4→5
 * ({@link IslandField#landBridgesNear}) at {@code position} of the zone (where the ground has fully broken up); the
 * bridge runs through its split, {@link #GAP} · 2 blocks wide at the foot and widening upwards as the two halves lean
 * apart over the abyss. The halves taper to their tops and differ in height; their faces are ragged.
 * <p>
 * Order independence (the ground-object model of {@link GroundObjects}, across chunk borders): a rock is a pure function
 * of the seed and its bridge ({@link #plan}) and of the frozen {@code WORLD_SURFACE_WG} at its anchor, and every chunk
 * it reaches writes only its own columns of it, each standing on that column's frozen {@code WORLD_SURFACE_WG}; the
 * block of each position comes from a random of its own. Placed once per chunk without a biome filter; nothing else stands on the bridges, and trees never take its stone
 * ({@link TreeOverlap#object}).
 */
public final class SplitRockFeature extends Feature<SplitRockConfiguration> {
    static final int MAX_HEIGHT = 40;
    static final float MAX_LENGTH = 6.0F;
    static final float MAX_LEAN = 4.0F;
    /** The split's half width at the foot (blocks from the bridge's middle line to a half's inner face). */
    static final double GAP = 1.5;
    /**
     * A half's outer face lies at the foot as far out as the bridge is wide, at least {@link #MIN_HALF_WIDTH} (wider than
     * every bridge: the halves reach out over the abyss and hang down there as a keel, {@link #HANG}) and at most
     * {@link #MAX_HALF_WIDTH}.
     */
    static final double MIN_HALF_WIDTH = 6.0;
    static final double MAX_HALF_WIDTH = 8.0;
    /** The lowest blocks above the ground are the foot's (moss, rubble). */
    static final int FOOT = 2;
    /** The rock reaches this far into the ground (no gap under a face on a slope). */
    static final int SUNK = 2;
    /**
     * Where a column has no bridge under it (its ground lies further below the anchor's than {@link #DROP}), the rock
     * hangs down this far below the bridge, less towards its ends along the bridge (a keel, not a flat underside).
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
     * No block lies further than this from the rock's anchor along x or z, in any direction of its bridge (on a
     * diagonal bridge a corner: (half width + lean + rag + length + rag) / √2 ≈ 13.8).
     */
    static final int REACH = 15;
    /** The rock's radius is looked for this far either side of its target distance (the boundary noise is ±96). */
    static final double SEARCH = 160.0;
    private static final long SALT = 0x5_9117_70C4L;

    /** A rock: anchor, direction along the bridge (unit), outer half width, the halves' heights. */
    record Rock(double x, double z, double ux, double uz, double halfWidth, int lowHeight, int highHeight, int highSide,
                double length, double lean) {
        /** The height of the half on {@code side} (−1 or +1 across the bridge). */
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
        IslandField field = Ring4Terrain.field(level);
        if (field == null) {
            return false;
        }
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        boolean placed = false;
        for (Rock rock : rocksNear(field, level.getSeed(), context.config(), minX, minZ)) {
            placed |= placeInChunk(level, context.config(), rock, minX, minZ);
        }
        return placed;
    }

    /** The rocks that reach into the chunk with minimum corner {@code minX, minZ}. */
    static List<Rock> rocksNear(IslandField field, long seed, SplitRockConfiguration config, int minX, int minZ) {
        List<Rock> rocks = new ArrayList<>();
        for (IslandField.LandBridge bridge : field.landBridgesNear(minX + 8, minZ + 8)) {
            Rock rock = plan(field, seed, config, bridge);
            if (rock.x + REACH >= minX && rock.x - REACH <= minX + 15 && rock.z + REACH >= minZ && rock.z - REACH <= minZ + 15) {
                rocks.add(rock);
            }
        }
        return rocks;
    }

    /** The rock of a bridge: pure in the seed, the bridge and the terrain. */
    static Rock plan(IslandField field, long seed, SplitRockConfiguration config, IslandField.LandBridge bridge) {
        double ux = Math.cos(bridge.angle());
        double uz = Math.sin(bridge.angle());
        // The radius whose effective distance (with the boundary noise) is the target share of the zone.
        double target = bridge.zoneStart() + config.position() * (bridge.zoneEnd() - bridge.zoneStart());
        // Bisection: the boundary noise can bend the distance too steeply for a fixed-point step to settle.
        double inner = target - SEARCH;
        double outer = target + SEARCH;
        for (int i = 0; i < 24; i++) {
            double mid = 0.5 * (inner + outer);
            if (field.effectiveDistance(mid * ux, mid * uz) < target) {
                inner = mid;
            } else {
                outer = mid;
            }
        }
        double r = 0.5 * (inner + outer);
        double x = Math.floor(r * ux) + 0.5;
        double z = Math.floor(r * uz) + 0.5;
        RandomSource random = GroundObjects.random(seed, new BlockPos(Mth.floor(x), 0, Mth.floor(z)), SALT);
        int high = config.height().sample(random);
        int low = Math.max(8, Math.round(high * config.shorter().sample(random)));
        int highSide = random.nextBoolean() ? 1 : -1;
        double length = config.length().sample(random);
        double lean = config.lean().sample(random);
        // The bridge's half width is along the zone's middle line; across the bridge here it is that much wider.
        double across = bridge.halfWidth() * r / (0.5 * (bridge.zoneStart() + bridge.zoneEnd()));
        double halfWidth = Mth.clamp(across, MIN_HALF_WIDTH, MAX_HALF_WIDTH);
        return new Rock(x, z, ux, uz, halfWidth, low, high, highSide, length, lean);
    }

    /** Writes the rock's blocks in the chunk's own columns; whether there were any. */
    private static boolean placeInChunk(WorldGenLevel level, SplitRockConfiguration config, Rock rock, int minX, int minZ) {
        Map<BlockPos, BlockState> stones = new LinkedHashMap<>();
        int x0 = Math.max(minX, Mth.floor(rock.x) - REACH);
        int x1 = Math.min(minX + 15, Mth.floor(rock.x) + REACH);
        int z0 = Math.max(minZ, Mth.floor(rock.z) - REACH);
        int z1 = Math.min(minZ + 15, Mth.floor(rock.z) + REACH);
        long seed = level.getSeed();
        // The bridge's top block at the anchor, from the frozen heightmap (the pure field's surface is not the zone's
        // final ground: ring 5's terrain blends in). The anchor always lies within a chunk of every chunk the rock
        // reaches (REACH < 16), where the frozen heightmaps are there to read.
        int base = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, Mth.floor(rock.x), Mth.floor(rock.z)) - 1;
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
     * a face of the split). {@code dy} counts from the ground at the anchor; below it the rock keeps its foot's outline.
     */
    static double inside(Rock rock, double along, double across, int height, int dy, double rag) {
        double t = Mth.clamp(dy / (double) height, 0.0, 1.0);
        double lean = rock.lean * t * t;
        double inner = GAP + lean + 0.5 * rag;
        double outer = rock.halfWidth * (1.0 - TAPER * t) + lean + rag;
        // The halves taper to ragged points: the outline along the bridge shrinks faster near the top.
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
