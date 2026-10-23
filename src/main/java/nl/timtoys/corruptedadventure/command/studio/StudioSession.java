package nl.timtoys.corruptedadventure.command.studio;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What the studio has placed (one thing at a time, server-wide, in memory only): per level the blocks of the box before
 * the placement and the entities the placement added, the structure's own box and floors, and a running cut-away.
 * A new placement clears the previous one first; a server stop forgets it.
 */
final class StudioSession {
    /** The studio's invisible floor (barrier); things stand on the layer above it. */
    static final int STUDIO_FLOOR_Y = 63;
    /** What a studio placement (or a clear without a session) empties at least: around x = z = 0. */
    static final BoundingBox STUDIO_AREA = new BoundingBox(-128, 0, -128, 127, 319, 127);
    /** Largest box the studio snapshots (blocks), against running out of memory with a huge structure. */
    static final long MAX_VOLUME = 8_000_000L;

    @Nullable
    private static StudioSession current;

    /** One level the thing was placed in: the blocks before, and the entities the placement added. */
    record Stage(ResourceKey<Level> dimension, BlockSnapshot before, List<UUID> entities) {
    }

    /** A placement prepared for one level: its box is known before anything is placed (for the snapshot). */
    interface Prepared {
        BoundingBox box();

        void place() throws CommandSyntaxException;
    }

    @FunctionalInterface
    interface Placer {
        Prepared prepare(ServerLevel level) throws CommandSyntaxException;
    }

    final String what;
    final boolean inStudio;
    final List<Stage> stages;
    /** The blocks the placement changed into something other than air (the structure itself). */
    final BoundingBox tight;
    final List<Integer> floors;
    /** The structure's columns (lowest and highest structure block), what the camera frames. */
    final List<StudioCamera.Column> columns;
    @Nullable
    private BlockSnapshot cut;

    private StudioSession(String what, boolean inStudio, List<Stage> stages, BoundingBox tight, List<Integer> floors,
                          List<StudioCamera.Column> columns) {
        this.what = what;
        this.inStudio = inStudio;
        this.stages = stages;
        this.tight = tight;
        this.floors = floors;
        this.columns = columns;
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(ServerStoppedEvent.class, event -> current = null);
    }

    @Nullable
    static StudioSession current() {
        return current;
    }

    static StudioSession require() throws CommandSyntaxException {
        if (current == null) {
            throw fail("Nothing placed in the studio (/ca studio template|structure first)");
        }
        return current;
    }

    static CommandSyntaxException fail(String message) {
        return new SimpleCommandExceptionType(Component.literal(message)).create();
    }

    /** Clears the previous placement, then places into every level (the first one is measured). */
    static StudioSession place(MinecraftServer server, String what, boolean inStudio, List<ServerLevel> levels, Placer placer)
            throws CommandSyntaxException {
        clear(server);
        List<Stage> stages = new ArrayList<>();
        try {
            for (ServerLevel level : levels) {
                Prepared prepared = placer.prepare(level);
                BoundingBox box = clip(prepared.box(), level);
                if (BlockSnapshot.volume(box) > MAX_VOLUME) {
                    throw fail("Box " + describe(box) + " is too big for the studio (max " + MAX_VOLUME + " blocks)");
                }
                if (inStudio) {
                    // Start from an empty studio: what a forgotten session (server restart) left behind goes too.
                    wipeStudio(level, new BoundingBox(
                            Math.min(box.minX(), STUDIO_AREA.minX()), Math.min(box.minY(), STUDIO_AREA.minY()),
                            Math.min(box.minZ(), STUDIO_AREA.minZ()), Math.max(box.maxX(), STUDIO_AREA.maxX()),
                            Math.max(box.maxY(), STUDIO_AREA.maxY()), Math.max(box.maxZ(), STUDIO_AREA.maxZ())));
                }
                BlockSnapshot before = BlockSnapshot.take(level, box);
                Set<UUID> present = entityIds(level, box);
                prepared.place();
                List<UUID> added = new ArrayList<>();
                for (Entity entity : entities(level, box)) {
                    if (!present.contains(entity.getUUID())) {
                        added.add(entity.getUUID());
                        // Placed mobs stand still, so every photo of a rerun shows the same scene.
                        if (entity instanceof Mob mob) {
                            mob.setNoAi(true);
                        }
                    }
                }
                stages.add(new Stage(level.dimension(), before, added));
            }
        } finally {
            // Whatever got placed can be cleared, also after a failure halfway.
            if (!stages.isEmpty()) {
                current = new StudioSession(what, inStudio, stages, stages.getFirst().before().box(), List.of(), List.of());
            }
        }
        Stage first = stages.getFirst();
        StudioSession session = measure(server.getLevel(first.dimension()), what, inStudio, stages);
        current = session;
        return session;
    }

    /**
     * Empties a studio level within the area: every block back to the generator's state (air, the barrier floor) and
     * every entity but players gone. Only sections with blocks are read, so the empty void costs almost nothing.
     */
    static int wipeStudio(ServerLevel level, BoundingBox area) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState floor = Blocks.BARRIER.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int changed = 0;
        for (int cx = SectionPos.blockToSectionCoord(area.minX()); cx <= SectionPos.blockToSectionCoord(area.maxX()); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(area.minZ()); cz <= SectionPos.blockToSectionCoord(area.maxZ()); cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    if (sections[i].hasOnlyAir()) {
                        continue;
                    }
                    int baseY = SectionPos.sectionToBlockCoord(level.getSectionYFromSectionIndex(i));
                    for (int y = Math.max(baseY, area.minY()); y <= Math.min(baseY + 15, area.maxY()); y++) {
                        BlockState target = y == STUDIO_FLOOR_Y ? floor : air;
                        for (int z = Math.max(cz << 4, area.minZ()); z <= Math.min((cz << 4) + 15, area.maxZ()); z++) {
                            for (int x = Math.max(cx << 4, area.minX()); x <= Math.min((cx << 4) + 15, area.maxX()); x++) {
                                if (sections[i].getBlockState(x & 15, y & 15, z & 15) != target) {
                                    BlockSnapshot.set(level, pos.set(x, y, z), target);
                                    changed++;
                                }
                            }
                        }
                    }
                }
            }
        }
        entities(level, area).forEach(Entity::discard);
        return changed;
    }

    /** Puts the cut-away back, removes the placed entities and restores every level's blocks. */
    static boolean clear(MinecraftServer server) {
        StudioSession session = current;
        if (session == null) {
            return false;
        }
        session.restoreCut(server);
        for (Stage stage : session.stages) {
            ServerLevel level = server.getLevel(stage.dimension());
            if (level == null) {
                continue;
            }
            for (UUID id : stage.entities()) {
                Entity entity = level.getEntity(id);
                if (entity != null) {
                    entity.discard();
                }
            }
            stage.before().restore(level);
        }
        current = null;
        return true;
    }

    /** Removes every block above {@code keepY} in the box of the first level (after putting back an earlier cut). */
    int cut(MinecraftServer server, int keepY) throws CommandSyntaxException {
        restoreCut(server);
        ServerLevel level = primary(server);
        BoundingBox box = stages.getFirst().before().box();
        if (keepY >= box.maxY()) {
            return 0;
        }
        BoundingBox above = new BoundingBox(box.minX(), Math.max(box.minY(), keepY + 1), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
        cut = BlockSnapshot.take(level, above);
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int removed = 0;
        // Top down: nothing is left hanging on a block that is about to go.
        for (int y = above.maxY(); y >= above.minY(); y--) {
            for (int z = above.minZ(); z <= above.maxZ(); z++) {
                for (int x = above.minX(); x <= above.maxX(); x++) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        BlockSnapshot.set(level, pos, air);
                        removed++;
                    }
                }
            }
        }
        return removed;
    }

    /** Puts a running cut-away back; false when there was none. */
    boolean restoreCut(MinecraftServer server) {
        BlockSnapshot running = cut;
        if (running == null) {
            return false;
        }
        cut = null;
        ServerLevel level = server.getLevel(stages.getFirst().dimension());
        if (level != null) {
            running.restore(level);
        }
        return true;
    }

    ServerLevel primary(MinecraftServer server) throws CommandSyntaxException {
        ServerLevel level = server.getLevel(stages.getFirst().dimension());
        if (level == null) {
            throw fail("The studio's level " + stages.getFirst().dimension().location() + " is gone");
        }
        return level;
    }

    /** The structure's blocks in the first level: what changed into something other than air. */
    private static StudioSession measure(ServerLevel level, String what, boolean inStudio, List<Stage> stages)
            throws CommandSyntaxException {
        BlockSnapshot before = stages.getFirst().before();
        BoundingBox box = before.box();
        BitSet solid = new BitSet();
        int[] lowest = new int[box.getXSpan() * box.getZSpan()];
        int[] highest = new int[lowest.length];
        Arrays.fill(lowest, Integer.MAX_VALUE);
        Arrays.fill(highest, Integer.MIN_VALUE);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (state.isAir() || state == before.get(x, y, z)) {
                        continue;
                    }
                    int column = (z - box.minZ()) * box.getXSpan() + (x - box.minX());
                    lowest[column] = Math.min(lowest[column], y);
                    highest[column] = Math.max(highest[column], y);
                    if (state.blocksMotion()) {
                        solid.set(((y - box.minY()) * box.getZSpan() + (z - box.minZ())) * box.getXSpan() + (x - box.minX()));
                    }
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                }
            }
        }
        if (minX == Integer.MAX_VALUE) {
            throw fail("The placement changed no blocks in " + describe(box));
        }
        List<StudioCamera.Column> columns = new ArrayList<>();
        for (int i = 0; i < lowest.length; i++) {
            if (lowest[i] != Integer.MAX_VALUE) {
                columns.add(new StudioCamera.Column(box.minX() + i % box.getXSpan(), box.minZ() + i / box.getXSpan(), lowest[i], highest[i]));
            }
        }
        // Per layer of the structure itself (a jigsaw start's box can reach far below or above it): the columns that go
        // on above the layer, and those of them that are solid at it.
        int[] solidAbove = new int[maxY - minY + 1];
        int[] spanning = new int[solidAbove.length];
        int area = box.getXSpan() * box.getZSpan();
        for (int column = 0; column < area; column++) {
            for (int y = lowest[column]; y < highest[column] && lowest[column] != Integer.MAX_VALUE; y++) {
                spanning[y - minY]++;
                if (solid.get((y - box.minY()) * area + column)) {
                    solidAbove[y - minY]++;
                }
            }
        }
        List<Integer> floors = StudioFloors.detect(minY, solidAbove, spanning, columns.size());
        return new StudioSession(what, inStudio, stages, new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ), floors,
                List.copyOf(columns));
    }

    private static BoundingBox clip(BoundingBox box, ServerLevel level) {
        return new BoundingBox(box.minX(), Math.max(box.minY(), level.getMinBuildHeight()), box.minZ(),
                box.maxX(), Math.min(box.maxY(), level.getMaxBuildHeight() - 1), box.maxZ());
    }

    private static List<Entity> entities(ServerLevel level, BoundingBox box) {
        return level.getEntities((Entity) null, AABB.of(box).inflate(1.0), entity -> !(entity instanceof Player));
    }

    private static Set<UUID> entityIds(ServerLevel level, BoundingBox box) {
        Set<UUID> ids = new HashSet<>();
        entities(level, box).forEach(entity -> ids.add(entity.getUUID()));
        return ids;
    }

    static String describe(BoundingBox box) {
        return box.minX() + " " + box.minY() + " " + box.minZ() + " .. " + box.maxX() + " " + box.maxY() + " " + box.maxZ()
                + " (" + box.getXSpan() + "x" + box.getYSpan() + "x" + box.getZSpan() + ")";
    }
}
