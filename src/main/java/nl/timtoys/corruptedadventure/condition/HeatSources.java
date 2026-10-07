package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import nl.timtoys.corruptedadventure.registry.ModTags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Predicate;

/**
 * Whether a heat source ({@code #corruptedadventure:heat_sources}) is near a player: a box of {@code radius} blocks
 * around the feet and head blocks ({@code (2r+1)² × (2r+2)} blocks, 810 for radius 4), searched nearest first, so it
 * stops at the first hit; the condition tick asks this once per second per exposed player in ring 3. First the
 * palettes of the few chunk sections the box touches are checked, so without a source nearby no block is read.
 * Never loads a chunk: right after a teleport the neighbouring chunks may not be there yet, and loading them here
 * would generate them on the server thread; a block in a missing chunk is no source.
 */
final class HeatSources {
    /** One offset table per radius (1–8 in the config); built on first use (a race only builds a table twice). */
    private static final AtomicReferenceArray<int[]> OFFSETS = new AtomicReferenceArray<>(9);

    private HeatSources() {
    }

    static boolean near(ServerLevel level, BlockPos feet, int radius) {
        // The box is at most 17 blocks wide (radius ≤ 8), so it touches at most 2 × 2 chunks.
        int minChunkX = SectionPos.blockToSectionCoord(feet.getX() - radius);
        int minChunkZ = SectionPos.blockToSectionCoord(feet.getZ() - radius);
        int maxChunkX = SectionPos.blockToSectionCoord(feet.getX() + radius);
        int maxChunkZ = SectionPos.blockToSectionCoord(feet.getZ() + radius);
        LevelChunk[] chunks = new LevelChunk[4];
        boolean candidate = false;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                chunks[(chunkX - minChunkX) * 2 + chunkZ - minChunkZ] = chunk;
                if (!candidate && chunk != null) {
                    candidate = mayContainSource(chunk, feet.getY() - radius, feet.getY() + radius + 1);
                }
            }
        }
        if (!candidate) {
            return false; // the usual case: no source in the palettes of the sections the box touches
        }
        return scan(feet, radius, pos -> {
            LevelChunk chunk = chunks[(SectionPos.blockToSectionCoord(pos.getX()) - minChunkX) * 2
                    + SectionPos.blockToSectionCoord(pos.getZ()) - minChunkZ];
            return chunk != null && isSource(chunk.getBlockState(pos));
        });
    }

    /**
     * Whether a section between {@code minY} and {@code maxY} may hold a source: its palette lists one (a palette can
     * list states that are gone, and a full global palette always says yes; then the block scan decides).
     */
    private static boolean mayContainSource(LevelChunk chunk, int minY, int maxY) {
        int from = SectionPos.blockToSectionCoord(Math.max(minY, chunk.getMinBuildHeight()));
        int to = SectionPos.blockToSectionCoord(Math.min(maxY, chunk.getMaxBuildHeight() - 1));
        for (int sectionY = from; sectionY <= to; sectionY++) {
            LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
            if (!section.hasOnlyAir() && section.maybeHas(HeatSources::isSource)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSource(BlockState state) {
        return state.is(ModTags.Blocks.HEAT_SOURCES);
    }

    /** Nearest first; {@code isSource} gets a reused mutable position (do not keep it). */
    static boolean scan(BlockPos feet, int radius, Predicate<BlockPos> isSource) {
        int[] offsets = offsets(radius);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < offsets.length; i += 3) {
            pos.set(feet.getX() + offsets[i], feet.getY() + offsets[i + 1], feet.getZ() + offsets[i + 2]);
            if (isSource.test(pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The offsets of the box as {@code dx, dy, dz} triples, by rising squared distance to the player's body (the feet
     * block {@code dy = 0} and the head block {@code dy = 1} are both distance 0 vertically); ties keep the scan order.
     */
    static int[] offsets(int radius) {
        if (radius < 1 || radius >= OFFSETS.length()) {
            throw new IllegalArgumentException("Heat source radius out of range: " + radius);
        }
        int[] offsets = OFFSETS.get(radius);
        if (offsets == null) {
            offsets = build(radius);
            OFFSETS.set(radius, offsets);
        }
        return offsets;
    }

    private static int[] build(int radius) {
        List<int[]> offsets = new ArrayList<>();
        for (int dy = -radius; dy <= radius + 1; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    offsets.add(new int[]{dx, dy, dz});
                }
            }
        }
        offsets.sort(Comparator.comparingInt(HeatSources::distanceSquared));
        int[] packed = new int[offsets.size() * 3];
        for (int i = 0; i < offsets.size(); i++) {
            System.arraycopy(offsets.get(i), 0, packed, i * 3, 3);
        }
        return packed;
    }

    private static int distanceSquared(int[] offset) {
        int dy = offset[1] < 0 ? offset[1] : Math.max(0, offset[1] - 1);
        return offset[0] * offset[0] + dy * dy + offset[2] * offset[2];
    }
}
