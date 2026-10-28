package nl.timtoys.corruptedadventure.item.lens;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.network.LensScanPayload;
import nl.timtoys.corruptedadventure.network.LensScanPayload.MissingChunk;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingWorld;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5MissingChunks;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5Terrain;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What a sky glass lens shows round a player (§7.2), decided on the server: the glitch blocks within {@link #RADIUS} of
 * the eye, the void holes under the ground and the clean missing chunks within {@link #RADIUS} (horizontally) in ring 5.
 * Reads only chunks that are loaded; never loads one. Glitch blocks come from the blocks (a section's palette first, so
 * a section without one costs a lookup), the holes from the blocks too (a filled hole is gone), the missing chunks from
 * the pure worldgen decision ({@link Ring5MissingChunks}: exact and cheaper than looking at 256 columns).
 */
public final class LensScan {
    public static final int RADIUS = 32;
    /** Floor samples per hole: enough to find its height, and a hole is flat round its mouth. */
    static final int FLOOR_SAMPLES = 4;

    private LensScan() {
    }

    public static LensScanPayload scan(ServerLevel level, Vec3 eye) {
        List<BlockPos> glitches = glitches(level, eye);
        List<BlockPos> holes = List.of();
        List<MissingChunk> chunks = List.of();
        if (level.dimension() == Level.OVERWORLD && RingWorld.isActive(level.getServer())
                && nearRing5(RingLayout.current(), eye.x, eye.z)) {
            Ring5World world = Ring5Terrain.world(level.getChunkSource().randomState());
            if (world != null) {
                holes = holes(new LevelGround(level, world), Mth.floor(eye.x), Mth.floor(eye.z), RADIUS);
                chunks = missingChunks(level, world, Mth.floor(eye.x), Mth.floor(eye.z));
            }
        }
        return new LensScanPayload(glitches, holes, chunks);
    }

    /** Whether ring 5 can lie within the scan's reach of the position (its boundary noise included). */
    static boolean nearRing5(RingLayout layout, double x, double z) {
        return layout.reaches(Ring.RING_5, layout.effectiveDistance(x, z), layout.reachMargin(RADIUS + 16, 1));
    }

    // ---------------------------------------------------------------- glitch blocks

    private static List<BlockPos> glitches(ServerLevel level, Vec3 eye) {
        Block glitch = ModBlocks.GLITCH_BLOCK.get();
        List<BlockPos> found = new ArrayList<>();
        int minSection = Math.max(level.getMinSection(), Mth.floor(eye.y - RADIUS) >> 4);
        int maxSection = Math.min(level.getMaxSection() - 1, Mth.floor(eye.y + RADIUS) >> 4);
        for (int cx = Mth.floor(eye.x - RADIUS) >> 4; cx <= Mth.floor(eye.x + RADIUS) >> 4; cx++) {
            for (int cz = Mth.floor(eye.z - RADIUS) >> 4; cz <= Mth.floor(eye.z + RADIUS) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (int sy = minSection; sy <= maxSection; sy++) {
                    LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
                    if (!section.hasOnlyAir() && section.maybeHas(state -> state.is(glitch))) {
                        collect(section.getStates(), glitch, cx << 4, sy << 4, cz << 4, eye, RADIUS, found);
                    }
                }
            }
        }
        return nearest(found, eye, LensScanPayload.MAX_GLITCHES);
    }

    /**
     * Adds every {@code target} block of one section (origin {@code x0, y0, z0}) within {@code radius} of {@code centre}
     * (block centres) to {@code out}; only the part of the section inside the sphere is read.
     */
    static void collect(PalettedContainer<BlockState> states, Block target, int x0, int y0, int z0, Vec3 centre,
                        int radius, List<BlockPos> out) {
        double r2 = (double) radius * radius;
        for (int y = Math.max(0, Mth.floor(centre.y - radius) - y0); y <= Math.min(15, Mth.floor(centre.y + radius) - y0); y++) {
            double dy = y0 + y + 0.5 - centre.y;
            for (int z = Math.max(0, Mth.floor(centre.z - radius) - z0); z <= Math.min(15, Mth.floor(centre.z + radius) - z0); z++) {
                double dz = z0 + z + 0.5 - centre.z;
                double rest = r2 - dy * dy - dz * dz;
                if (rest < 0.0) {
                    continue;
                }
                double reach = Math.sqrt(rest);
                int from = Math.max(0, Mth.ceil(centre.x - reach - 0.5) - x0);
                int to = Math.min(15, Mth.floor(centre.x + reach - 0.5) - x0);
                for (int x = from; x <= to; x++) {
                    if (states.get(x, y, z).is(target)) {
                        out.add(new BlockPos(x0 + x, y0 + y, z0 + z));
                    }
                }
            }
        }
    }

    /** The {@code max} positions nearest to {@code centre} (block centres), nearest first. */
    static List<BlockPos> nearest(List<BlockPos> found, Vec3 centre, int max) {
        if (found.size() <= max) {
            return found;
        }
        return found.stream().sorted(Comparator.comparingDouble(pos -> pos.distToCenterSqr(centre))).limit(max).toList();
    }

    // ---------------------------------------------------------------- void holes under the ground

    /** The blocks a hole scan reads. */
    interface Ground {
        /** Whether the column is loaded, in ring 5's inner band, open at the world's bottom and has ground over it. */
        boolean openBelow(int x, int z);

        /**
         * The first air block over the solid ground that starts at the world's bottom (the top of a cave floor next to a
         * hole), or {@link Integer#MIN_VALUE} where the column is not loaded or open at the bottom.
         */
        int floorTop(int x, int z);
    }

    /**
     * The void holes under the ground within {@code radius} (horizontally) of (cx, cz): one block per open column, at the
     * floor's height next to its hole (each 4-connected hole takes the lowest of up to {@link #FLOOR_SAMPLES} floor
     * samples round it; a hole without a floor next to it is left out). At most {@link LensScanPayload#MAX_HOLES}, the
     * nearest columns.
     */
    static List<BlockPos> holes(Ground ground, int cx, int cz, int radius) {
        LongSet open = new LongOpenHashSet();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radius * radius && ground.openBelow(cx + dx, cz + dz)) {
                    open.add(ChunkPos.asLong(cx + dx, cz + dz));
                }
            }
        }
        List<BlockPos> found = new ArrayList<>();
        LongSet seen = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        List<Long> group = new ArrayList<>();
        for (long start : open) {
            if (!seen.add(start)) {
                continue;
            }
            group.clear();
            queue.enqueue(start);
            int floor = Integer.MAX_VALUE;
            int samples = 0;
            while (!queue.isEmpty()) {
                long column = queue.dequeueLong();
                group.add(column);
                int x = ChunkPos.getX(column);
                int z = ChunkPos.getZ(column);
                for (int side = 0; side < 4; side++) {
                    int nx = x + (side == 0 ? 1 : side == 1 ? -1 : 0);
                    int nz = z + (side == 2 ? 1 : side == 3 ? -1 : 0);
                    long next = ChunkPos.asLong(nx, nz);
                    if (open.contains(next)) {
                        if (seen.add(next)) {
                            queue.enqueue(next);
                        }
                    } else if (samples < FLOOR_SAMPLES) {
                        int top = ground.floorTop(nx, nz);
                        if (top != Integer.MIN_VALUE) {
                            floor = Math.min(floor, top);
                            samples++;
                        }
                    }
                }
            }
            if (samples > 0) {
                for (long column : group) {
                    found.add(new BlockPos(ChunkPos.getX(column), floor - 1, ChunkPos.getZ(column)));
                }
            }
        }
        if (found.size() <= LensScanPayload.MAX_HOLES) {
            return found;
        }
        return found.stream().sorted(Comparator.comparingLong(pos -> {
            long dx = pos.getX() - cx;
            long dz = pos.getZ() - cz;
            return dx * dx + dz * dz;
        })).limit(LensScanPayload.MAX_HOLES).toList();
    }

    /** {@link Ground} on a server level: loaded chunks only, ring 5's inner band only. */
    private static final class LevelGround implements Ground {
        private static final byte OUT = 0;
        private static final byte IN = 1;
        private static final byte MIXED = 2;
        private static final byte UNKNOWN = -1;

        private final ServerLevel level;
        private final Ring5World world;
        private final int bottom;
        /** Per chunk of this scan: wholly in or out of the inner band, or mixed (then per column). */
        private final Long2ByteOpenHashMap scope = new Long2ByteOpenHashMap();

        LevelGround(ServerLevel level, Ring5World world) {
            this.level = level;
            this.world = world;
            this.bottom = level.getMinBuildHeight();
            scope.defaultReturnValue(UNKNOWN);
        }

        @Override
        public boolean openBelow(int x, int z) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            return chunk != null && air(chunk, x, bottom, z)
                    && chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) > bottom && inScope(x, z);
        }

        /**
         * Ring 5's inner band (not the zone 4→5, not the outer band whose floes all hang over the void). The band line and
         * the ring boundary are smooth at a chunk's size, so a chunk whose corners and middle agree is decided whole.
         */
        private boolean inScope(int x, int z) {
            int x0 = x & ~15;
            int z0 = z & ~15;
            long key = ChunkPos.asLong(x >> 4, z >> 4);
            byte chunk = scope.get(key);
            if (chunk == UNKNOWN) {
                boolean middle = inner(x0 + 8, z0 + 8);
                boolean same = inner(x0, z0) == middle && inner(x0 + 15, z0) == middle && inner(x0, z0 + 15) == middle
                        && inner(x0 + 15, z0 + 15) == middle;
                chunk = same ? (middle ? IN : OUT) : MIXED;
                scope.put(key, chunk);
            }
            return chunk == MIXED ? inner(x, z) : chunk == IN;
        }

        private boolean inner(int x, int z) {
            return world.core(x, z) && world.band().inner(x, z);
        }

        @Override
        public int floorTop(int x, int z) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
            if (chunk == null || air(chunk, x, bottom, z)) {
                return Integer.MIN_VALUE;
            }
            int top = level.getMaxBuildHeight();
            for (int y = bottom + 1; y < top; y++) {
                if (air(chunk, x, y, z)) {
                    return y;
                }
            }
            return Integer.MIN_VALUE;
        }

        private boolean air(LevelChunk chunk, int x, int y, int z) {
            return chunk.getSection(chunk.getSectionIndex(y)).getBlockState(x & 15, y & 15, z & 15).isAir();
        }
    }

    // ---------------------------------------------------------------- missing chunks

    /**
     * The clean missing chunks whose footprint comes within {@link #RADIUS} (horizontally) of (x, z), from the world's
     * bottom to the ground the chunk lost. There is one per 20 × 20 chunks at most, so the cap is a safety net.
     */
    private static List<MissingChunk> missingChunks(ServerLevel level, Ring5World world, int x, int z) {
        List<MissingChunk> found = new ArrayList<>();
        for (int cx = (x - RADIUS) >> 4; cx <= (x + RADIUS) >> 4; cx++) {
            for (int cz = (z - RADIUS) >> 4; cz <= (z + RADIUS) >> 4; cz++) {
                int nearX = Mth.clamp(x, cx << 4, (cx << 4) + 15);
                int nearZ = Mth.clamp(z, cz << 4, (cz << 4) + 15);
                long dx = nearX - x;
                long dz = nearZ - z;
                if (dx * dx + dz * dz <= (long) RADIUS * RADIUS && Ring5MissingChunks.missing(world, cx, cz)) {
                    int top = world.top((cx << 4) + 8, (cz << 4) + 8) + 1;
                    found.add(new MissingChunk(cx, cz, level.getMinBuildHeight(), Math.max(top, level.getMinBuildHeight() + 1)));
                }
            }
        }
        return found.size() <= LensScanPayload.MAX_CHUNKS ? found : found.subList(0, LensScanPayload.MAX_CHUNKS);
    }
}
