package nl.timtoys.corruptedadventure.command.studio;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.HashMap;
import java.util.Map;

/**
 * The blocks (and block entities) of a box, to put back later: before a placement (so {@code clear} restores the
 * terrain of an in-situ placement) and before a cut-away. Changes are written without neighbour updates and without
 * drops, so nothing falls, flows or spills while the studio works.
 */
final class BlockSnapshot {
    /** Block updates for the studio's own changes: clients only, no neighbour shape updates. */
    static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private final BoundingBox box;
    private final BlockState[] states;
    private final Map<BlockPos, CompoundTag> blockEntities;

    private BlockSnapshot(BoundingBox box, BlockState[] states, Map<BlockPos, CompoundTag> blockEntities) {
        this.box = box;
        this.states = states;
        this.blockEntities = blockEntities;
    }

    static long volume(BoundingBox box) {
        return (long) box.getXSpan() * box.getYSpan() * box.getZSpan();
    }

    static BlockSnapshot take(ServerLevel level, BoundingBox box) {
        BlockState[] states = new BlockState[Math.toIntExact(volume(box))];
        Map<BlockPos, CompoundTag> blockEntities = new HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int i = 0;
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    states[i++] = state;
                    if (state.hasBlockEntity()) {
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity != null) {
                            blockEntities.put(pos.immutable(), blockEntity.saveWithFullMetadata(level.registryAccess()));
                        }
                    }
                }
            }
        }
        return new BlockSnapshot(box, states, blockEntities);
    }

    BoundingBox box() {
        return box;
    }

    /** The saved state at a position inside the box. */
    BlockState get(int x, int y, int z) {
        return states[((y - box.minY()) * box.getZSpan() + (z - box.minZ())) * box.getXSpan() + (x - box.minX())];
    }

    /** Puts every saved block back where the world differs, then the saved block entity data. */
    void restore(ServerLevel level) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int i = 0;
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    pos.set(x, y, z);
                    BlockState saved = states[i++];
                    if (level.getBlockState(pos) != saved || level.getBlockEntity(pos) != null) {
                        set(level, pos, saved);
                    }
                }
            }
        }
        blockEntities.forEach((at, tag) -> {
            BlockEntity blockEntity = level.getBlockEntity(at);
            if (blockEntity != null) {
                blockEntity.loadWithComponents(tag, level.registryAccess());
                blockEntity.setChanged();
                BlockState state = level.getBlockState(at);
                level.sendBlockUpdated(at, state, state, Block.UPDATE_CLIENTS);
            }
        });
    }

    /** Sets a block quietly; a container is emptied first, so replacing it drops nothing. */
    static void set(ServerLevel level, BlockPos pos, BlockState state) {
        Clearable.tryClear(level.getBlockEntity(pos));
        level.setBlock(pos, state, QUIET);
    }
}
